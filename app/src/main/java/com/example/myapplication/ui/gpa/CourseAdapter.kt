package com.example.myapplication.ui.gpa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemCourseBinding
import com.example.myapplication.domain.model.Course
import java.util.Locale

class CourseAdapter(
    private val onDeleteClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private val items = mutableListOf<Course>()

    fun submitList(newList: List<Course>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class CourseViewHolder(private val binding: ItemCourseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCourseName.text = course.name
            binding.tvCourseGrade.text = String.format(
                Locale.US,
                "Note: %.2f / 20",
                course.gradeOn20
            )
            binding.tvCourseCoeff.text = String.format(
                Locale.US,
                "Coef: %.2f",
                course.coefficient
            )

            binding.btnDeleteCourse.setOnClickListener {
                onDeleteClick(course)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}