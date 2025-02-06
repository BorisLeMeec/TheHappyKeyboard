package com.example.thehappykeyboard

import java.io.File

data class MediaItem(val name: String, val file: File, val type: String,val tags: List<Tag>)