package com.example.myapplication.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.databinding.FragmentNotesBinding
import com.example.myapplication.data.repository.NoteRepository

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NoteViewModel
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = AppDatabase.getDatabase(requireContext()).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        notesAdapter = NotesAdapter(
            onEditClick = { note ->
                findNavController().navigate(
                    R.id.action_notesFragment_to_addEditNoteFragment,
                    bundleOf("noteId" to note.id)
                )
            },
            onDeleteClick = { note ->
                viewModel.deleteNote(note)
            }
        )

        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotes.adapter = notesAdapter

        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            notesAdapter.submitList(notes)
        }

        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(R.id.action_notesFragment_to_addEditNoteFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}