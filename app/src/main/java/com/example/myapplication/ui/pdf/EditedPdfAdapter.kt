package com.example.myapplication.ui.pdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.EditedPdfItemBinding
import com.example.myapplication.domain.model.EditedPdfItem

class EditedPdfAdapter(
    private val onClick: (EditedPdfItem) -> Unit
) : RecyclerView.Adapter<EditedPdfAdapter.EditedPdfViewHolder>() {

    private val items = mutableListOf<EditedPdfItem>()

    fun submitList(newItems: List<EditedPdfItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class EditedPdfViewHolder(
        private val binding: EditedPdfItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EditedPdfItem) {
            binding.tvPdfTitle.text = item.displayName
            binding.tvPdfSubtitle.text = item.originalPdfPath

            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.btnOpenPdf.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditedPdfViewHolder {
        val binding = EditedPdfItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EditedPdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EditedPdfViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}