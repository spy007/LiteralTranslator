package com.kirillex.multispeakliteral.domain.model

data class FavoriteModel(
    val id: Int,
    val word: String,
    val translation: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val timestamp: Long
)
