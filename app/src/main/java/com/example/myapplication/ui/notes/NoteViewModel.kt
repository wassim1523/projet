package com.example.myapplication.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.data.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    val allNotes: LiveData<List<NoteEntity>> = repository.allNotes

    fun insertNote(title: String, content: String) {
        viewModelScope.launch {
            repository.insert(
                NoteEntity(
                    title = title,
                    content = content
                )
            )
        }
    }

    fun updateNote(id: Int, title: String, content: String) {
        viewModelScope.launch {
            repository.update(
                NoteEntity(
                    id = id,
                    title = title,
                    content = content
                )
            )
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
}