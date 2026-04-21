package com.example.myapplication.domain.model

data class Course(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val gradeOn20: Double,
    val coefficient: Double
)