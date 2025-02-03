package com.example.thehappykeyboard

import android.content.ClipDescription
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import kotlin.collections.sortByDescending


class TheHappyKeyboardService : InputMethodService(), MediaAdapter.OnMediaClickListener {

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
    private lateinit var keyboardLayout: LinearLayout
    private lateinit var searchEditText: EditText
    private var originalInputConnection: InputConnection? = null

    override fun onCreateInputView(): View {
        // Inflate the keyboard layout
        val keyboardViewInflated = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)

        // Find the button and set its click listener
        val switchKeyboardButton = keyboardViewInflated.findViewById<Button>(R.id.switch_keyboard_button)
        switchKeyboardButton.setOnClickListener {
            // Show the keyboard picker
            showInputMethodPicker(it.context)
        }

        mediaRecyclerView = keyboardViewInflated.findViewById(R.id.mediaRecyclerView)
        mediaRecyclerView.layoutManager = GridLayoutManager(this, NUMBER_OF_COLUMNS)
        mediaAdapter = MediaAdapter(mediaList, null, this)
        mediaRecyclerView.adapter = mediaAdapter

        gifCheckBox = keyboardViewInflated.findViewById(R.id.gifCheckBox)
        imageCheckBox = keyboardViewInflated.findViewById(R.id.imageCheckBox)
        videoCheckBox = keyboardViewInflated.findViewById(R.id.videoCheckBox)

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

        keyboardLayout = keyboardViewInflated.findViewById(R.id.keyboardLayout)

        searchEditText = keyboardViewInflated.findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnClickListener {
            keyboardLayout.visibility = View.VISIBLE
        }
        searchEditText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                keyboardLayout.visibility = View.GONE
                return@OnEditorActionListener true
            }
            false
        })
        searchEditText.setOnClickListener {
            keyboardLayout.visibility = View.VISIBLE
        }
        keyboardViewInflated.setOnClickListener {
            keyboardLayout.visibility = View.GONE
        }

        // Set click listeners for the keyboard buttons
        setupKeyboardButtons(keyboardViewInflated)

        return keyboardViewInflated
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        keyboardLayout.visibility = View.GONE
    }

    private fun setupKeyboardButtons(keyboardViewInflated: View) {
        val keyIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l, R.id.key_z,
            R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n,
            R.id.key_m, R.id.key_space
        )

        for (keyId in keyIds) {
            val button = keyboardViewInflated.findViewById<Button>(keyId)
            button.setOnClickListener {
                val text = button.text.toString()
                handleKeyPress(text)
            }
        }
        val delButton = keyboardViewInflated.findViewById<Button>(R.id.key_del)
        delButton.setOnClickListener {
            handleDeletePress()
        }
    }

    private fun handleKeyPress(text: String) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(text, 1)
    }

    private fun handleDeletePress() {
        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(1, 0)
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
    override fun onMediaClick(mediaFile: File) {
        val inputConnection: InputConnection? = currentInputConnection
        if (inputConnection != null) {
            try {
                val contentUri: Uri = FileProvider.getUriForFile(this, AUTHORITY, mediaFile)
                val mimeType = when {
                    mediaFile.name.endsWith(".gif") -> "image/gif"
                    mediaFile.name.endsWith(".jpg") || mediaFile.name.endsWith(".jpeg") -> "image/jpeg"
                    mediaFile.name.endsWith(".png") -> "image/png"
                    mediaFile.name.endsWith(".mp4") -> "video/mp4"
                    else -> ""
                }
                val clipDescription = ClipDescription(mediaFile.name, arrayOf(mimeType))
                val inputContentInfo = InputContentInfo(
                    contentUri,
                    clipDescription,
                    null
                )
                val flags = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                val result = inputConnection.commitContent(
                    inputContentInfo,
                    flags,
                    null
                )
                Log.d("TheHappyKeyboardService", "Media sent: $result")
                if (!result) {
                    Toast.makeText(this, "App does not support this media format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("TheHappyKeyboardService", "Error sending media", e)
                Toast.makeText(this, "Error sending media", Toast.LENGTH_SHORT).show()
            }
        }
    }
}