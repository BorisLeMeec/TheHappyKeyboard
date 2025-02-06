package com.example.thehappykeyboard

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import pl.droidsonroids.gif.GifImageView
import java.io.File

class MediaAdapter(
    private var mediaList: List<MediaItem>,
    private val mainActivity: MainActivity?,
    private val listener: OnMediaClickListener?) :
    RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    interface OnMediaClickListener {
        fun onMediaClick(mediaFile: File)
    }

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mediaImageView: ImageView = itemView.findViewById(R.id.mediaImageView)
        val gifImageView: GifImageView = itemView.findViewById(R.id.gifImageView)
        val videoImageView: ImageView = itemView.findViewById(R.id.videoImageView)
        val playIcon: ImageView = itemView.findViewById(R.id.playIcon)
        val deleteMediaButton: ImageView? = itemView.findViewById(R.id.deleteMediaButton)

        init {
            itemView.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.media_item, parent, false)
        return MediaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val currentItem = mediaList[position]
        val file = currentItem.file
        val type = currentItem.type

        holder.mediaImageView.visibility = View.GONE
        holder.gifImageView.visibility = View.GONE
        holder.videoImageView.visibility = View.GONE
        holder.playIcon.visibility = View.GONE

        when (type) {
            "gif" -> {
                holder.gifImageView.visibility = View.VISIBLE
                holder.gifImageView.setImageURI(Uri.fromFile(file))
            }
            "image" -> {
                holder.mediaImageView.visibility = View.VISIBLE
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                holder.mediaImageView.setImageBitmap(bitmap)
            }
            "video" -> {
                holder.videoImageView.visibility = View.VISIBLE
                holder.playIcon.visibility = View.VISIBLE
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val bitmap: Bitmap? = retriever.getFrameAtTime(0)
                    holder.videoImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }
        }

        if (mainActivity != null) {
            holder.deleteMediaButton?.visibility = View.VISIBLE
            holder.deleteMediaButton?.setOnClickListener {
                showDeleteConfirmationDialog(holder.itemView.context, currentItem, mainActivity)
            }
        } else {
            holder.deleteMediaButton?.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            Log.d("MediaAdapter", "Item clicked: ${file.name}")
            listener?.onMediaClick(file)
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, mediaItem: MediaItem, mainActivity: MainActivity) {
        AlertDialog.Builder(context)
            .setTitle(R.string.delete_confirmation)
            .setMessage(R.string.delete_confirmation_message)
            .setPositiveButton("Delete") { dialog, _ ->
                deleteMedia(mediaItem, mainActivity)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteMedia(mediaItem: MediaItem, mainActivity: MainActivity) {
        if (mediaItem.file.delete()) {
            mainActivity.loadMedias()
        }
    }

    fun updateData(newMediaList: List<MediaItem>) {
        mediaList = newMediaList
        notifyDataSetChanged()
    }

    override fun getItemCount() = mediaList.size
}