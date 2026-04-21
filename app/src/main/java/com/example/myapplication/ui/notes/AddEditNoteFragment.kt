package com.example.myapplication.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.databinding.FragmentAddEditNoteBinding
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class AddEditNoteFragment : Fragment(), RecognitionListener {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteViewModel: NotesViewModel
    private lateinit var speechViewModel: SpeechViewModel

    private var noteId: Int = -1

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var isListening = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            } else {
                Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = NotesViewModelFactory(requireActivity().application)
        noteViewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]
        speechViewModel = ViewModelProvider(this)[SpeechViewModel::class.java]

        noteId = arguments?.getInt("noteId", -1) ?: -1

        if (noteId != -1) {
            noteViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
                val note = notes.find { it.id == noteId }
                if (note != null) {
                    binding.etTitle.setText(note.title)
                    binding.etContent.setText(note.content)
                }
            }
        }

        binding.tvSpeechStatus.text = ""
        initVoskModel()

        binding.btnSave.setOnClickListener {
            saveNote()
        }

        binding.btnRecordSpeech.setOnClickListener {
            if (!isListening) {
                checkPermissionAndStart()
            } else {
                stopListening()
            }
        }

        speechViewModel.speechText.observe(viewLifecycleOwner) { text ->
            if (text.isNotBlank()) {
                val current = binding.etContent.text.toString()
                val newText = if (current.isBlank()) text else "$current $text"
                binding.etContent.setText(newText)
                binding.etContent.setSelection(binding.etContent.text?.length ?: 0)
                binding.tvSpeechStatus.text = "Speech added"
            }
        }

        speechViewModel.isListening.observe(viewLifecycleOwner) { listening ->
            isListening = listening == true
            binding.btnRecordSpeech.text = if (isListening) "Stop Speech" else "Start Speech"
            if (!isListening && binding.tvSpeechStatus.text == "Listening...") {
                binding.tvSpeechStatus.text = ""
            }
        }

        speechViewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.tvSpeechStatus.text = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initVoskModel() {
        binding.tvSpeechStatus.text = "Loading speech model..."
        binding.btnRecordSpeech.isEnabled = false

        StorageService.unpack(
            requireContext(),
            "vosk-model-small-en-us-0.15",
            "model",
            { unpackedModel ->
                model = unpackedModel
                binding.tvSpeechStatus.text = "Speech ready"
                binding.btnRecordSpeech.isEnabled = true
            },
            { exception ->
                model = null
                binding.tvSpeechStatus.text = "Model load failed: ${exception.message}"
                binding.btnRecordSpeech.isEnabled = false
                speechViewModel.setError(exception.message ?: "Model load failed")
            }
        )
    }

    private fun checkPermissionAndStart() {
        val permissionState = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        )

        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val currentModel = model
        if (currentModel == null) {
            binding.tvSpeechStatus.text = "Model not ready yet"
            Toast.makeText(requireContext(), "Model not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            recognizer?.close()
            recognizer = Recognizer(currentModel, 16000.0f)

            speechService?.stop()
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)

            speechViewModel.setListening(true)
            binding.tvSpeechStatus.text = "Listening..."
        } catch (e: Exception) {
            speechViewModel.setListening(false)
            speechViewModel.setError(e.message ?: "Speech start failed")
        }
    }

    private fun stopListening() {
        try {
            speechService?.stop()
        } catch (_: Exception) {
        }
        speechService = null

        try {
            recognizer?.close()
        } catch (_: Exception) {
        }
        recognizer = null

        speechViewModel.setListening(false)
        binding.tvSpeechStatus.text = "Stopped"
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (noteId == -1) {
            noteViewModel.insertNote(title, content)
            Toast.makeText(requireContext(), "Note added", Toast.LENGTH_SHORT).show()
        } else {
            noteViewModel.updateNote(noteId, title, content)
            Toast.makeText(requireContext(), "Note updated", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    override fun onPartialResult(hypothesis: String?) {
        val text = extractText(hypothesis)
        if (text.isNotBlank()) {
            binding.tvSpeechStatus.text = text
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractText(hypothesis)
        if (text.isNotBlank()) {
            speechViewModel.setSpeechText(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = extractText(hypothesis)
        if (text.isNotBlank()) {
            speechViewModel.setSpeechText(text)
        }
        stopListening()
    }

    override fun onError(exception: Exception?) {
        speechViewModel.setListening(false)
        speechViewModel.setError(exception?.message ?: "Speech error")
    }

    override fun onTimeout() {
        stopListening()
    }

    private fun extractText(json: String?): String {
        if (json.isNullOrBlank()) return ""

        return try {
            val obj = JSONObject(json)
            when {
                obj.has("text") -> obj.getString("text")
                obj.has("partial") -> obj.getString("partial")
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    override fun onDestroyView() {
        try {
            speechService?.stop()
        } catch (_: Exception) {
        }
        speechService = null

        try {
            recognizer?.close()
        } catch (_: Exception) {
        }
        recognizer = null

        try {
            model?.close()
        } catch (_: Exception) {
        }
        model = null

        _binding = null
        super.onDestroyView()
    }
}