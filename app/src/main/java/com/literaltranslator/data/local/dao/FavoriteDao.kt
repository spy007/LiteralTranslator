package com.literaltranslator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.literaltranslator.data.local.entity.FavoriteWord
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteWord)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteWord)

    @Query("SELECT * FROM favorite_words ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteWord>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_words WHERE word = :word AND sourceLang = :sourceLang AND targetLang = :targetLang LIMIT 1)")
    suspend fun isFavorite(word: String, sourceLang: String, targetLang: String): Boolean

    @Query("DELETE FROM favorite_words WHERE word = :word AND sourceLang = :sourceLang AND targetLang = :targetLang")
    suspend fun deleteFavoriteByDetails(word: String, sourceLang: String, targetLang: String)

    @Query("DELETE FROM favorite_words WHERE id = :id")
    suspend fun deleteFavoriteById(id: Int)
}
