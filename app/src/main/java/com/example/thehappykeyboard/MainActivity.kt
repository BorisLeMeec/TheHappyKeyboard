package com.example.thehappykeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), MediaAdapter.OnMediaClickListener {

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 1
        private const val NUMBER_OF_COLUMNS = 3
        private const val MEDIA_FOLDER_NAME = "all_medias"
        private const val TYPE_GIF = "gif"
        private const val TYPE_IMAGE = "image"
        private const val TYPE_VIDEO = "video"
    }

    private lateinit var addMediaFab: FloatingActionButton
    private lateinit var mediaRecyclerView: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var filterButton: ImageButton
    private lateinit var searchEditText: EditText
    private lateinit var db: AppDatabase

    private var isGifChecked = true
    private var isImageChecked = true
    private var isVideoChecked = true

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

        db = AppDatabase.getDatabase(this)

        addMediaFab = findViewById(R.id.addMediaFab)
        addMediaFab.setOnClickListener {
            checkPermissionsAndSelectMedia()
        }

        filterButton = findViewById(R.id.filterButton)
        searchEditText = findViewById(R.id.searchEditText)
        filterButton.setOnClickListener {
            showFilterPopup(it)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mediaRecyclerView = findViewById(R.id.mediaRecyclerView)
        mediaRecyclerView.layoutManager = GridLayoutManager(this, NUMBER_OF_COLUMNS)
        mediaAdapter = MediaAdapter(mediaList, this,this)
        mediaRecyclerView.adapter = mediaAdapter

        loadMedias()

        // Handle the shared intent
        handleSharedIntent(intent)
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            if (intent.type?.startsWith("image/") == true) {
                val imageUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (imageUri != null) {
                    // Handle the shared image URI
                    handleSharedImage(imageUri)
                } else {
                    val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    if (imageUris != null) {
                        for (uri in imageUris) {
                            handleSharedImage(uri)
                        }
                    }
                }
            }
        }
    }

    private fun handleSharedImage(imageUri: Uri) {
        copyMediaToAppFolder(imageUri)
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
            val intent = Intent(this, MediaViewActivity::class.java)
            intent.putExtra(MediaViewActivity.EXTRA_MEDIA_PATH, gifFile.absolutePath)
            startActivity(intent)
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

    private fun showFilterPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.filter_menu, popup.menu)

        // Set initial checked states
        popup.menu.findItem(R.id.menu_gif).isChecked = isGifChecked
        popup.menu.findItem(R.id.menu_image).isChecked = isImageChecked
        popup.menu.findItem(R.id.menu_video).isChecked = isVideoChecked

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_gif -> {
                    isGifChecked = !isGifChecked
                    item.isChecked = isGifChecked
                }
                R.id.menu_image -> {
                    isImageChecked = !isImageChecked
                    item.isChecked = isImageChecked
                }
                R.id.menu_video -> {
                    isVideoChecked = !isVideoChecked
                    item.isChecked = isVideoChecked
                }
            }
            loadMedias()
            false
        }

        popup.setOnDismissListener {
            // This listener is called when the popup is dismissed
        }

        popup.show()
    }

    fun loadMedias() {
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaFolder = File(filesDir, MEDIA_FOLDER_NAME)
            val newMediaList = mutableListOf<MediaItem>()
            if (mediaFolder.exists() && mediaFolder.isDirectory) {
                val mediaFiles = mediaFolder.listFiles { file ->
                    file.isFile && (file.name.endsWith(".gif") || file.name.endsWith(".jpg") || file.name.endsWith(
                        ".jpeg"
                    ) || file.name.endsWith(".png") || file.name.endsWith(".mp4"))
                }
                mediaFiles?.forEach { file ->
                    val type = when {
                        file.name.endsWith(".gif") -> TYPE_GIF
                        file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(
                            ".png"
                        ) -> TYPE_IMAGE

                        file.name.endsWith(".mp4") -> TYPE_VIDEO
                        else -> ""
                    }
                    if ((isImageChecked && type == TYPE_IMAGE) || (isGifChecked && type == TYPE_GIF) || (isVideoChecked && type == TYPE_VIDEO)) {
                        val tags = db.tagDao().getTagsForMedia(file.path)
                        newMediaList.add(MediaItem(file.name, file, type, tags))
                    }
                }
            }
            newMediaList.sortByDescending { it.file.lastModified() }
            withContext(Dispatchers.Main) {
                mediaList.clear()
                mediaList.addAll(newMediaList)
                mediaAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onMediaClick(mediaFile: File) {
        val intent = Intent(this, MediaViewActivity::class.java)
        intent.putExtra(MediaViewActivity.EXTRA_MEDIA_PATH, mediaFile.absolutePath)
        startActivity(intent)
    }
}