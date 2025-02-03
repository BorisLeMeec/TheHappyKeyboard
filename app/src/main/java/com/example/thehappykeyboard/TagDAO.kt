package com.example.thehappykeyboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TagDao {
    @Insert
    suspend fun insert(tag: Tag)

    @Query("SELECT * FROM tags WHERE media_path = :mediaPath")
    suspend fun getTagsForMedia(mediaPath: String): List<Tag>

    @Query("DELETE FROM tags WHERE media_path = :mediaPath AND tag_name = :tagName")
    suspend fun deleteTag(mediaPath: String, tagName: String)
}