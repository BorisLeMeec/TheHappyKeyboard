package com.example.thehappykeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TagsAdapter(private val tagsList: List<String>) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tagTextView: TextView = itemView.findViewById(R.id.tagTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val currentTag = tagsList[position]
        holder.tagTextView.text = currentTag
    }

    override fun getItemCount(): Int {
        return tagsList.size
    }
}