package com.example.myapplication.repository

import com.example.myapplication.data.local.pdf.PageDao
import com.example.myapplication.data.local.pdf.PageEntity

class PageNoteRepository(
    private val pageDao: PageDao
) {

    suspend fun saveNote(pdfUri: String, pageIndex: Int, noteText: String) {
        pageDao.insert(
            PageEntity(
                pdfUri = pdfUri,
                pageIndex = pageIndex,
                noteText = noteText
            )
        )
    }

    suspend fun getNote(pdfUri: String, pageIndex: Int): PageEntity? {
        return pageDao.getPageNote(pdfUri, pageIndex)
    }
}