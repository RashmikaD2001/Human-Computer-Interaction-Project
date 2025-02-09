package com.example.hci3v1.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScoreDao {
    @Query("SELECT score FROM score_table ORDER BY id DESC LIMIT 1")
    suspend fun getLatestScore(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scoreEntity: ScoreEntity)

    @Query("SELECT score FROM score_table ORDER BY id DESC LIMIT 1")
    fun getScore(): LiveData<Int>
}