package com.example.myapplication.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.data.repository.NotesRepository
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotesRepository
    val notes: LiveData<List<NoteEntity>>
    val allNotes: LiveData<List<NoteEntity>> get() = notes

    init {
        val dao = AppDatabase.getInstance(application).noteDao()
        repository = NotesRepository(dao)
        notes = repository.getAllNotes()
    }

    fun addTextNote(title: String, content: String) {
        insertNote(title, content)
    }

    fun insertNote(title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title,
                    content = content,
                    audioPath = null
                )
            )
        }
    }

    fun addVoiceNote(title: String, audioPath: String) {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title,
                    content = "",
                    audioPath = audioPath
                )
            )
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun updateNote(id: Int, title: String, content: String) {
        viewModelScope.launch {
            repository.updateNote(
                NoteEntity(
                    id = id,
                    title = title,
                    content = content,
                    audioPath = null
                )
            )
        }
    }
}