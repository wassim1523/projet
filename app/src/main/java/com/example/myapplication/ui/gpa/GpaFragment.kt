package com.example.myapplication.ui.gpa

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentGpaBinding

class GpaFragment : Fragment() {

    private var _binding: FragmentGpaBinding? = null
    private val binding get() = _binding!!

    private data class CourseItem(
        val name: String,
        val coeff: Double,
        val grade: Double
    )

    private val courses = mutableListOf<CourseItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGpaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddCourse.setOnClickListener {
            addCourse()
        }

        binding.btnCalculateGpa.setOnClickListener {
            calculateAverage()
        }

        binding.btnClearCourses.setOnClickListener {
            courses.clear()
            binding.coursesContainer.removeAllViews()
            binding.tvGpaResult.text = "Average: 0.00 / 20"
        }
    }

    private fun addCourse() {
        val name = binding.etCourseName.text.toString().trim().ifBlank { "Course ${courses.size + 1}" }
        val coeffText = binding.etCredits.text.toString().trim()
        val gradeText = binding.etGradePoint.text.toString().trim()

        if (coeffText.isEmpty() || gradeText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter coefficient and grade", Toast.LENGTH_SHORT).show()
            return
        }

        val coeff = coeffText.toDoubleOrNull()
        val grade = gradeText.toDoubleOrNull()

        if (coeff == null || grade == null) {
            Toast.makeText(requireContext(), "Invalid numbers", Toast.LENGTH_SHORT).show()
            return
        }

        if (coeff <= 0) {
            Toast.makeText(requireContext(), "Coefficient must be > 0", Toast.LENGTH_SHORT).show()
            return
        }

        if (grade < 0 || grade > 20) {
            Toast.makeText(requireContext(), "Grade must be between 0 and 20", Toast.LENGTH_SHORT).show()
            return
        }

        val item = CourseItem(name, coeff, grade)
        courses.add(item)
        addCourseRow(item, courses.lastIndex)

        binding.etCourseName.text?.clear()
        binding.etCredits.text?.clear()
        binding.etGradePoint.text?.clear()
        calculateAverage()
    }

    private fun addCourseRow(item: CourseItem, index: Int) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#EEE7F3"))
            setPadding(24, 20, 24, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.topMargin = 12
        row.layoutParams = rowParams

        val infoText = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "${index + 1}. ${item.name}  |  Coeff: ${item.coeff}  |  Grade: ${item.grade}/20"
            setTextColor(Color.parseColor("#2B2233"))
            textSize = 15f
        }

        val deleteButton = TextView(requireContext()).apply {
            text = "Delete"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.RED)
            setPadding(20, 12, 20, 12)
            setOnClickListener {
                val position = binding.coursesContainer.indexOfChild(row)
                if (position in courses.indices) {
                    courses.removeAt(position)
                    binding.coursesContainer.removeView(row)
                    refreshCourseRows()
                    calculateAverage()
                }
            }
        }

        row.addView(infoText)
        row.addView(deleteButton)
        binding.coursesContainer.addView(row)
    }

    private fun refreshCourseRows() {
        binding.coursesContainer.removeAllViews()
        courses.forEachIndexed { i, item ->
            addCourseRow(item, i)
        }
    }

    private fun calculateAverage() {
        if (courses.isEmpty()) {
            binding.tvGpaResult.text = "Average: 0.00 / 20"
            return
        }

        var weightedSum = 0.0
        var totalCoeff = 0.0

        courses.forEach {
            weightedSum += it.coeff * it.grade
            totalCoeff += it.coeff
        }

        val average = if (totalCoeff == 0.0) 0.0 else weightedSum / totalCoeff
        binding.tvGpaResult.text = "Average: %.2f / 20".format(average)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}