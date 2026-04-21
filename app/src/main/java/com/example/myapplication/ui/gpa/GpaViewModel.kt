package com.example.myapplication.ui.gpa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.domain.model.Course

class GpaViewModel : ViewModel() {

    private val _courses = MutableLiveData<List<Course>>(emptyList())
    val courses: LiveData<List<Course>> = _courses

    private val _averageOn20 = MutableLiveData(0.0)
    val averageOn20: LiveData<Double> = _averageOn20

    private val _gpaOn4 = MutableLiveData(0.0)
    val gpaOn4: LiveData<Double> = _gpaOn4

    fun addCourse(name: String, gradeOn20: Double, coefficient: Double) {
        val current = _courses.value.orEmpty().toMutableList()
        current.add(
            Course(
                name = name,
                gradeOn20 = gradeOn20,
                coefficient = coefficient
            )
        )
        _courses.value = current
        recalculate()
    }

    fun deleteCourse(course: Course) {
        val current = _courses.value.orEmpty().toMutableList()
        current.removeAll { it.id == course.id }
        _courses.value = current
        recalculate()
    }

    private fun recalculate() {
        val list = _courses.value.orEmpty()

        if (list.isEmpty()) {
            _averageOn20.value = 0.0
            _gpaOn4.value = 0.0
            return
        }

        val totalCoeff = list.sumOf { it.coefficient }

        if (totalCoeff <= 0.0) {
            _averageOn20.value = 0.0
            _gpaOn4.value = 0.0
            return
        }

        val weightedSum = list.sumOf { it.gradeOn20 * it.coefficient }
        val avg20 = weightedSum / totalCoeff
        val gpa4 = (avg20 / 20.0) * 4.0

        _averageOn20.value = avg20
        _gpaOn4.value = gpa4
    }
}