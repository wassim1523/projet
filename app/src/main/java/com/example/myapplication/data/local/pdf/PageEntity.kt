package com.example.myapplication.data.local.pdf

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "page_notes")
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pdfUri: String,
    val pageIndex: Int,
    val noteText: String
)