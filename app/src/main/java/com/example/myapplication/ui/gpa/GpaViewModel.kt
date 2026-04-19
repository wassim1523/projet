package com.example.myapplication.ui.gpa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.domain.Course

class GpaViewModel : ViewModel() {

    private val _courses = MutableLiveData<MutableList<Course>>(mutableListOf())
    val courses: LiveData<MutableList<Course>> = _courses

    private val _gpa = MutableLiveData(0.0)
    val gpa: LiveData<Double> = _gpa

    fun addCourse(name: String, credit: Int, grade: Double) {
        val list = _courses.value ?: mutableListOf()
        list.add(Course(name, credit, grade))
        _courses.value = list
        calculate()
    }

    fun deleteCourse(position: Int) {
        val list = _courses.value ?: mutableListOf()
        if (position in list.indices) {
            list.removeAt(position)
            _courses.value = list
            calculate()
        }
    }

    private fun calculate() {
        val list = _courses.value ?: mutableListOf()
        var totalCredits = 0
        var totalPoints = 0.0

        for (course in list) {
            totalCredits += course.credit
            totalPoints += course.credit * course.grade
        }

        _gpa.value = if (totalCredits == 0) 0.0 else totalPoints / totalCredits
    }
}