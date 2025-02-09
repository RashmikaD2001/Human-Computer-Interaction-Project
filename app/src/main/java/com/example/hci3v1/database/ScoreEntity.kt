package com.example.hci3v1.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_table")
data class ScoreEntity(
    @PrimaryKey val id: Int = 1,
    val score: Int
)