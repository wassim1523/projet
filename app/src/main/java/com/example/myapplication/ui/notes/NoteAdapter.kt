package com.example.myapplication.ui.notes

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.databinding.ItemNoteBinding
import java.io.File

class NoteAdapter(
    private val onDeleteClick: (NoteEntity) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val items = mutableListOf<NoteEntity>()
    private var mediaPlayer: MediaPlayer? = null

    fun submitList(list: List<NoteEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvTitle.text = note.title

            val isVoiceNote = !note.audioPath.isNullOrEmpty()

            if (isVoiceNote) {
                binding.tvContent.text = "Voice note"
                binding.tvType.text = "Type: Audio"
                binding.layoutAudioActions.visibility = android.view.View.VISIBLE
            } else {
                binding.tvContent.text = note.content
                binding.tvType.text = "Type: Text"
                binding.layoutAudioActions.visibility = android.view.View.GONE
            }

            binding.btnDeleteNote.setOnClickListener {
                stopAudio()
                onDeleteClick(note)
            }

            binding.btnPlayAudio.setOnClickListener {
                note.audioPath?.let { path ->
                    playAudio(path)
                }
            }

            binding.btnStopAudio.setOnClickListener {
                stopAudio()
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private fun playAudio(path: String) {
        stopAudio()

        val file = File(path)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener {
                stopAudio()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}