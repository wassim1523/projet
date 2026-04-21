package com.example.myapplication.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.databinding.FragmentNotesBinding
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File

class NotesFragment : Fragment(R.layout.fragment_notes), RecognitionListener {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(requireActivity().application)
    }

    private lateinit var noteAdapter: NoteAdapter

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false

    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var isSpeechToTextRunning = false
    private var isVoskReady = false

    companion object {
        private const val AUDIO_PERMISSION_CODE = 1001
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotesBinding.bind(view)

        setupRecycler()
        observeNotes()
        setupClicks()
        initVosk()
    }

    private fun setupRecycler() {
        noteAdapter = NoteAdapter { note ->
            deleteNoteWithAudioIfNeeded(note)
        }

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }
    }

    private fun observeNotes() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.submitList(notes)
        }
    }

    private fun setupClicks() {
        binding.btnAddTextNote.setOnClickListener {
            val title = binding.etNoteTitle.text.toString().trim()
            val content = binding.etNoteContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(requireContext(), "Enter title and note", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addTextNote(title, content)
            binding.etNoteTitle.text?.clear()
            binding.etNoteContent.text?.clear()
        }

        binding.btnStartRecording.setOnClickListener {
            if (hasAudioPermission()) {
                startRecording()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    AUDIO_PERMISSION_CODE
                )
            }
        }

        binding.btnStopRecording.setOnClickListener {
            stopRecordingAndSave()
        }

        binding.btnStartSpeechToText.setOnClickListener {
            if (hasAudioPermission()) {
                startSpeechToText()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    AUDIO_PERMISSION_CODE
                )
            }
        }

        binding.btnStopSpeechToText.setOnClickListener {
            stopSpeechToText()
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initVosk() {
        binding.tvRecordingStatus.text = "Preparing speech recognition..."

        StorageService.unpack(
            requireContext(),
            "vosk-model-small-en-us-0.15",
            "model",
            { model ->
                voskModel = model
                isVoskReady = true
                binding.tvRecordingStatus.text = "Speech recognition ready"
            },
            { exception ->
                isVoskReady = false
                binding.tvRecordingStatus.text = "Speech recognition unavailable"
                Toast.makeText(
                    requireContext(),
                    "Vosk init failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun startRecording() {
        if (isRecording) return

        if (isSpeechToTextRunning) {
            Toast.makeText(
                requireContext(),
                "Stop speech-to-text first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val title = binding.etNoteTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Enter title first", Toast.LENGTH_SHORT).show()
            return
        }

        val audioDir = File(requireContext().filesDir, "audio_notes")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val fileName = "audio_${System.currentTimeMillis()}.m4a"
        val audioFile = File(audioDir, fileName)
        audioFilePath = audioFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            MediaRecorder()
        }

        try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            binding.tvRecordingStatus.text = "Recording..."
            Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            binding.tvRecordingStatus.text = "Not recording"
            Toast.makeText(requireContext(), "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecordingAndSave() {
        if (!isRecording) return

        val title = binding.etNoteTitle.text.toString().trim()
        val path = audioFilePath

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }

        mediaRecorder = null
        isRecording = false
        binding.tvRecordingStatus.text = "Not recording"

        if (!path.isNullOrEmpty() && title.isNotEmpty()) {
            viewModel.addVoiceNote(title, path)
            binding.etNoteTitle.text?.clear()
            binding.etNoteContent.text?.clear()
            Toast.makeText(requireContext(), "Voice note saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Voice note not saved", Toast.LENGTH_SHORT).show()
        }

        audioFilePath = null
    }

    private fun startSpeechToText() {
        if (isSpeechToTextRunning) return

        if (isRecording) {
            Toast.makeText(
                requireContext(),
                "Stop recording first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isVoskReady || voskModel == null) {
            Toast.makeText(
                requireContext(),
                "Speech recognition model not ready yet",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val recognizer = Recognizer(voskModel, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)

            isSpeechToTextRunning = true
            binding.tvRecordingStatus.text = "Listening..."
            Toast.makeText(requireContext(), "Speech-to-text started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            speechService?.stop()
            speechService = null
            isSpeechToTextRunning = false
            binding.tvRecordingStatus.text = "Speech-to-text failed"
            Toast.makeText(
                requireContext(),
                "Failed to start speech-to-text",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopSpeechToText() {
        if (!isSpeechToTextRunning) return

        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isSpeechToTextRunning = false
        binding.tvRecordingStatus.text = "Speech-to-text stopped"
        Toast.makeText(requireContext(), "Speech-to-text stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onPartialResult(hypothesis: String?) {
        if (_binding == null || hypothesis.isNullOrEmpty()) return

        try {
            val partial = JSONObject(hypothesis).optString("partial", "")
            if (partial.isNotEmpty()) {
                binding.etNoteContent.setText(partial)
                binding.etNoteContent.setSelection(binding.etNoteContent.text?.length ?: 0)
            }
        } catch (_: Exception) {
        }
    }

    override fun onResult(hypothesis: String?) {
        if (_binding == null || hypothesis.isNullOrEmpty()) return

        try {
            val text = JSONObject(hypothesis).optString("text", "")
            if (text.isNotEmpty()) {
                val current = binding.etNoteContent.text.toString().trim()
                val finalText = if (current.isEmpty()) text else "$current $text"
                binding.etNoteContent.setText(finalText)
                binding.etNoteContent.setSelection(binding.etNoteContent.text?.length ?: 0)
            }
        } catch (_: Exception) {
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (_binding == null || hypothesis.isNullOrEmpty()) {
            stopSpeechToText()
            return
        }

        try {
            val text = JSONObject(hypothesis).optString("text", "")
            if (text.isNotEmpty()) {
                val current = binding.etNoteContent.text.toString().trim()
                val finalText = if (current.isEmpty()) text else "$current $text"
                binding.etNoteContent.setText(finalText)
                binding.etNoteContent.setSelection(binding.etNoteContent.text?.length ?: 0)
            }
        } catch (_: Exception) {
        }

        stopSpeechToText()
    }

    override fun onError(e: Exception?) {
        if (_binding == null) return
        stopSpeechToText()
        Toast.makeText(
            requireContext(),
            e?.message ?: "Speech recognition error",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onTimeout() {
        if (_binding == null) return
        stopSpeechToText()
    }

    private fun deleteNoteWithAudioIfNeeded(note: NoteEntity) {
        note.audioPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        viewModel.deleteNote(note)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null

        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (_: Exception) {
        }
        speechService = null

        try {
            voskModel?.close()
        } catch (_: Exception) {
        }
        voskModel = null

        _binding = null
    }
}