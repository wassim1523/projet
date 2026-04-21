package com.example.myapplication.ui.gpa

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentGpaBinding
import java.util.Locale

class GpaFragment : Fragment(R.layout.fragment_gpa) {

    private var _binding: FragmentGpaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GpaViewModel by viewModels()
    private lateinit var courseAdapter: CourseAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGpaBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupClicks()
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseAdapter { course ->
            viewModel.deleteCourse(course)
        }

        binding.rvCourses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = courseAdapter
        }
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseAdapter.submitList(courses)
            binding.tvEmpty.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.averageOn20.observe(viewLifecycleOwner) { avg20 ->
            binding.tvAverage20.text = String.format(
                Locale.US,
                "Moyenne /20 : %.2f",
                avg20
            )
        }

        viewModel.gpaOn4.observe(viewLifecycleOwner) { gpa4 ->
            binding.tvGpa4.text = String.format(
                Locale.US,
                "GPA /4 : %.2f",
                gpa4
            )
        }
    }

    private fun setupClicks() {
        binding.btnAddCourse.setOnClickListener {
            val name = binding.etCourseName.text.toString().trim()
            val gradeText = binding.etGrade20.text.toString().trim()
            val coeffText = binding.etCoefficient.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter course name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (gradeText.isEmpty() || coeffText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter grade and coefficient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val grade = gradeText.toDoubleOrNull()
            val coefficient = coeffText.toDoubleOrNull()

            if (grade == null || coefficient == null) {
                Toast.makeText(requireContext(), "Invalid numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (grade < 0.0 || grade > 20.0) {
                Toast.makeText(requireContext(), "Grade must be between 0 and 20", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (coefficient <= 0.0) {
                Toast.makeText(requireContext(), "Coefficient must be > 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addCourse(name, grade, coefficient)

            binding.etCourseName.text?.clear()
            binding.etGrade20.text?.clear()
            binding.etCoefficient.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}