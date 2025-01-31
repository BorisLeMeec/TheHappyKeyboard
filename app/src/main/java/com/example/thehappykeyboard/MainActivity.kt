package com.example.thehappykeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 1
        private const val NUMBER_OF_COLUMNS = 3
        private const val MEDIA_FOLDER_NAME = "all_medias"
        private const val TYPE_GIF = "gif"
        private const val TYPE_IMAGE = "image"
        private const val TYPE_VIDEO = "video"
    }

    private lateinit var selectMediaButton: Button
    private lateinit var mediaRecyclerView: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var gifCheckBox: CheckBox
    private lateinit var imageCheckBox: CheckBox
    private lateinit var videoCheckBox: CheckBox

    private val mediaList = mutableListOf<MediaItem>()

    private val openMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                copyMediaToAppFolder(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectMediaButton = findViewById(R.id.selectMediaButton)
        selectMediaButton.setOnClickListener {
            checkPermissionsAndSelectMedia()
        }
        gifCheckBox = findViewById(R.id.gifCheckBox)
        imageCheckBox = findViewById(R.id.imageCheckBox)
        videoCheckBox = findViewById(R.id.videoCheckBox)

        selectMediaButton.setOnClickListener {
            selectMedia()
        }

        gifCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }

        imageCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }

        videoCheckBox.setOnCheckedChangeListener { _, _ ->
            loadMedias()
        }

        mediaRecyclerView = findViewById(R.id.mediaRecyclerView)
        mediaRecyclerView.layoutManager = GridLayoutManager(this, NUMBER_OF_COLUMNS)
        mediaAdapter = MediaAdapter(mediaList, this,null)
        mediaRecyclerView.adapter = mediaAdapter

        loadMedias()
    }

    private fun checkPermissionsAndSelectMedia() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
                selectMedia()
            }
        } else {
            selectMedia()
        }
    }

    private fun selectMedia() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        openMediaLauncher.launch(intent)
    }

    private fun copyMediaToAppFolder(mediaUri: Uri) {
        val mediaFolder = File(filesDir, MEDIA_FOLDER_NAME)
        if (!mediaFolder.exists()) {
            mediaFolder.mkdirs()
        }

        val gifFileName = "selected_media_${System.currentTimeMillis()}.${contentResolver.getType(mediaUri)?.substringAfterLast("/")}"
        val gifFile = File(mediaFolder, gifFileName)

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(mediaUri)
            val outputStream = FileOutputStream(gifFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(this, "Media copied to app folder", Toast.LENGTH_SHORT).show()
            loadMedias()
        } catch (e: IOException) {
            Log.e("MainActivity", "Error copying Media", e)
            Toast.makeText(this, "Error copying Media", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectMedia()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadMedias() {
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
}