package com.example.myapplication.ui.gpa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemCourseBinding
import com.example.myapplication.domain.Course

class CourseAdapter(
    private var list: MutableList<Course>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<CourseAdapter.Holder>() {

    inner class Holder(val binding: ItemCourseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val c = list[position]

        holder.binding.tvCourseName.text = c.name
        holder.binding.tvCourseCredit.text = "${c.credit} credits"
        holder.binding.tvCourseGrade.text = String.format("%.2f / 4", c.grade)

        holder.binding.btnDeleteCourse.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = list.size

    fun update(newList: MutableList<Course>) {
        list = newList
        notifyDataSetChanged()
    }
}