package com.example.myapplication.data.local.pdf

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PageDao {

    @Insert
    suspend fun insert(page: PageEntity)

    @Update
    suspend fun update(page: PageEntity)

    @Query("SELECT * FROM page_notes WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPageNote(pdfUri: String, pageIndex: Int): PageEntity?
}