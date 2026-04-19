package com.example.myapplication.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.databinding.ItemNoteBinding

class NotesAdapter(
    private val onEditClick: (NoteEntity) -> Unit,
    private val onDeleteClick: (NoteEntity) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes: List<NoteEntity> = emptyList()

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvTitle.text = note.title
            binding.tvContent.text = note.content

            binding.btnEdit.setOnClickListener {
                onEditClick(note)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    fun submitList(newNotes: List<NoteEntity>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}