package com.example.thehappykeyboard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "media_path") val mediaPath: String,
    @ColumnInfo(name = "tag_name") val tagName: String
)