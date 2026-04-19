package com.example.myapplication.data.local.recentpdf

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_pdfs")
data class RecentPdfEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val lastOpened: Long
)