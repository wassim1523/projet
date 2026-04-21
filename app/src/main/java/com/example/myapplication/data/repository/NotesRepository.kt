package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.data.local.notes.NoteDao
import com.example.myapplication.data.local.notes.NoteEntity

class NotesRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): LiveData<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insertNote(note: NoteEntity) = noteDao.insertNote(note)

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)
}