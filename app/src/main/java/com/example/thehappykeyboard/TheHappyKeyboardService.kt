package com.example.thehappykeyboard

import android.content.ClipDescription
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import kotlin.collections.sortByDescending


class TheHappyKeyboardService : InputMethodService(),MediaAdapter.OnMediaClickListener {

    companion object {
        private const val MEDIA_FOLDER_NAME = "all_medias"
        private const val NUMBER_OF_COLUMNS = 3
        private const val AUTHORITY = "com.example.thehappykeyboard.fileprovider"
        private const val TYPE_GIF = "gif"
        private const val TYPE_IMAGE = "image"
        private const val TYPE_VIDEO = "video"
    }

    private lateinit var mediaRecyclerView: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private val mediaList = mutableListOf<MediaItem>()
    private lateinit var gifCheckBox: CheckBox
    private lateinit var imageCheckBox: CheckBox
    private lateinit var videoCheckBox: CheckBox

    override fun onCreateInputView(): View {
        // Inflate the keyboard layout
        val keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)

        // Find the button and set its click listener
        val switchKeyboardButton = keyboardView.findViewById<Button>(R.id.switch_keyboard_button)
        switchKeyboardButton.setOnClickListener {
            // Show the keyboard picker
            showInputMethodPicker(it.context)
        }

        mediaRecyclerView = keyboardView.findViewById(R.id.mediaRecyclerView)
        mediaRecyclerView.layoutManager = GridLayoutManager(this, NUMBER_OF_COLUMNS)
        mediaAdapter = MediaAdapter(mediaList, null, this)
        mediaRecyclerView.adapter = mediaAdapter

        gifCheckBox = keyboardView.findViewById(R.id.gifCheckBox)
        imageCheckBox = keyboardView.findViewById(R.id.imageCheckBox)
        videoCheckBox = keyboardView.findViewById(R.id.videoCheckBox)

        gifCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }

        imageCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }

        videoCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }
        loadMedias()

        return keyboardView
    }

    private fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    private fun loadMedias() {
        val mediaFolder = File(filesDir, MEDIA_FOLDER_NAME)
        mediaList.clear()
        if (mediaFolder.exists() && mediaFolder.isDirectory) {
            val mediaFiles = mediaFolder.listFiles { file ->
                file.isFile && (file.name.endsWith(".gif") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png") || file.name.endsWith(".mp4"))
            }
            mediaFiles?.forEach { file ->
                val type = when {
                    file.name.endsWith(".gif") -> TYPE_GIF
                    file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png") -> TYPE_IMAGE
                    file.name.endsWith(".mp4") -> TYPE_VIDEO
                    else -> ""
                }
                if ((imageCheckBox.isChecked && type == TYPE_IMAGE) || (gifCheckBox.isChecked && type == TYPE_GIF) || (videoCheckBox.isChecked && type == TYPE_VIDEO)) {
                    mediaList.add(MediaItem(file.name, file, type))
                }
            }
        }
        mediaList.sortByDescending { it.file.lastModified() }
        mediaAdapter.notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onMediaClick(gifFile: File) {
        val inputConnection: InputConnection? = currentInputConnection
        if (inputConnection != null) {
            try {
                val contentUri: Uri = FileProvider.getUriForFile(this, AUTHORITY, gifFile)
                val mimeType = when {
                    gifFile.name.endsWith(".gif") -> "image/gif"
                    gifFile.name.endsWith(".jpg") || gifFile.name.endsWith(".jpeg") || gifFile.name.endsWith(".png") -> "image/*"
                    gifFile.name.endsWith(".mp4") -> "video/*"
                    else -> ""
                }
                val clipDescription = ClipDescription("Media", arrayOf(mimeType))
                val inputContentInfo = InputContentInfo(
                    contentUri,
                    clipDescription,
                    null
                )
                val flags = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                inputConnection.commitContent(
                    inputContentInfo,
                    flags,
                    null
                )
            } catch (e: IOException) {
                Log.e("TheHappyKeyboardService", "Error sending media", e)
                Toast.makeText(this, "Error sending media", Toast.LENGTH_SHORT).show()
            }
        }
    }
}