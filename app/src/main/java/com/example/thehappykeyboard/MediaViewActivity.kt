package com.example.thehappykeyboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import kotlin.text.isNotEmpty
import kotlin.text.trim
import com.google.android.flexbox.FlexboxLayout
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.text.format

class MediaViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDIA_PATH = "extra_media_path"
    }

    private lateinit var mediaImageView: ImageView
    private lateinit var noTagsTextView: TextView
    private lateinit var addTagButton: Button
    private lateinit var tagsAdapter: TagsAdapter
    private val tagsList = mutableListOf<String>()
    private lateinit var addTagLayout: LinearLayout
    private lateinit var tagEditText: EditText
    private lateinit var validateTagButton: ImageButton
    private lateinit var tagsFlexboxLayout: FlexboxLayout
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_view)

        db = AppDatabase.getDatabase(this)

        mediaImageView = findViewById(R.id.mediaImageView)
        tagsFlexboxLayout = findViewById(R.id.tagsFlexboxLayout)
        noTagsTextView = findViewById(R.id.noTagsTextView)
        addTagButton = findViewById(R.id.addTagButton)
        addTagLayout = findViewById(R.id.addTagLayout)
        tagEditText = findViewById(R.id.tagEditText)
        validateTagButton = findViewById(R.id.validateTagButton)

        val mediaPath = intent.getStringExtra(EXTRA_MEDIA_PATH)
        if (mediaPath != null) {
            val mediaFile = File(mediaPath)
            if (mediaFile.exists()) {
                displayMedia(mediaFile)
            }
        }

        updateTagVisibility()

        addTagButton.setOnClickListener {
            showAddTagLayout()
        }

        validateTagButton.setOnClickListener {
            addTag()
        }

        tagEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTag()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun displayMedia(mediaFile: File) {
        val uri = Uri.fromFile(mediaFile)
        Glide.with(this)
            .load(uri)
            .into(mediaImageView)
    }

    private fun updateTagVisibility() {
        addTagButton.visibility = View.VISIBLE
        val mediaPath = intent.getStringExtra(EXTRA_MEDIA_PATH) ?: return
        lifecycleScope.launch {
            val tags = db.tagDao().getTagsForMedia(mediaPath)
            tagsList.clear()
            tagsList.addAll(tags.map { it.tagName })
            if (tagsList.isEmpty()) {
                tagsFlexboxLayout.visibility = View.GONE
                noTagsTextView.visibility = View.VISIBLE
            } else {
                tagsFlexboxLayout.visibility = View.VISIBLE
                noTagsTextView.visibility = View.GONE
            }
            displayTags()
        }
    }

    private fun showAddTagLayout() {
        addTagLayout.visibility = View.VISIBLE
        addTagButton.visibility = View.GONE
        tagEditText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(tagEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun addTag() {
        val tagText = tagEditText.text.toString().trim()
        if (tagText.isNotEmpty()) {
            val mediaPath = intent.getStringExtra(EXTRA_MEDIA_PATH) ?: return
            val tag = Tag(mediaPath = mediaPath, tagName = tagText)

            lifecycleScope.launch {
                db.tagDao().insert(tag)
                tagsList.add(tagText)
                tagEditText.text.clear()
                addTagLayout.visibility = View.GONE
                updateTagVisibility()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(tagEditText.windowToken, 0)
            }
        }
    }

    private fun displayTags() {
        tagsFlexboxLayout.removeAllViews()
        val mediaPath = intent.getStringExtra(EXTRA_MEDIA_PATH) ?: return
        for (tag in tagsList) {
            val tagView =
                LayoutInflater.from(this).inflate(R.layout.item_tag, tagsFlexboxLayout, false)
            val tagTextView = tagView.findViewById<TextView>(R.id.tagTextView)
            val deleteTagButton = tagView.findViewById<ImageView>(R.id.deleteTagButton)
            tagTextView.text = tag
            deleteTagButton.setOnClickListener {
                lifecycleScope.launch {
                    db.tagDao().deleteTag(mediaPath, tag)
                    updateTagVisibility()
                }
            }
            tagsFlexboxLayout.addView(tagView)
        }
    }
}