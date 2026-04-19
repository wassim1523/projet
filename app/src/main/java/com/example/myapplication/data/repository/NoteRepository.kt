package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.data.local.notes.NoteDao
import com.example.myapplication.data.local.notes.NoteEntity

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: LiveData<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insert(note: NoteEntity) {
        noteDao.insert(note)
    }

    suspend fun update(note: NoteEntity) {
        noteDao.update(note)
    }

    suspend fun delete(note: NoteEntity) {
        noteDao.delete(note)
    }
}
