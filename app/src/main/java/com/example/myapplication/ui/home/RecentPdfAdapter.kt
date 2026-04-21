package com.example.myapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.local.recentpdf.RecentPdfEntity
import com.example.myapplication.databinding.ItemRecentPdfBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentPdfAdapter(
    private val onClick: (RecentPdfEntity) -> Unit,
    private val onDelete: (RecentPdfEntity) -> Unit
) : RecyclerView.Adapter<RecentPdfAdapter.RecentPdfViewHolder>() {

    private val items = mutableListOf<RecentPdfEntity>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun submitList(list: List<RecentPdfEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentPdfViewHolder {
        val binding = ItemRecentPdfBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentPdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentPdfViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RecentPdfViewHolder(
        private val binding: ItemRecentPdfBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentPdfEntity) {
            binding.tvPdfName.text = item.name
            binding.tvPdfDate.text = dateFormat.format(Date(item.lastOpened))

            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.ivDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }
}