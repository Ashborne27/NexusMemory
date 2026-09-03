package com.ashborne.nexusmemory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MemoryAdapter(private val onDeleteClick: (MemoryEntity) -> Unit) :
    ListAdapter<MemoryEntity, MemoryAdapter.MemoryViewHolder>(MemoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = getItem(position)
        holder.bind(memory, onDeleteClick)
    }

    class MemoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val contentView: TextView = itemView.findViewById(R.id.tvItemContent)
        private val deleteButton: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(memory: MemoryEntity, onDeleteClick: (MemoryEntity) -> Unit) {
            titleView.text = memory.title
            contentView.text = memory.content
            deleteButton.setOnClickListener { onDeleteClick(memory) }
        }
    }

    class MemoryDiffCallback : DiffUtil.ItemCallback<MemoryEntity>() {
        override fun areItemsTheSame(oldItem: MemoryEntity, newItem: MemoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MemoryEntity, newItem: MemoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
