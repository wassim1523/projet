package com.example.myapplication.domain.model

data class EditedPdfItem(
    val originalPdfPath: String,
    val displayName: String,
    val savedProjectPath: String,
    val lastEdited: Long
)