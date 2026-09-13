package com.kirillex.multispeakliteral.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_words")
data class FavoriteWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long = System.currentTimeMillis()
)
