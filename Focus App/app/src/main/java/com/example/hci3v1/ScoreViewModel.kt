package com.example.hci3v1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.hci3v1.database.ScoreDatabase
import com.example.hci3v1.database.ScoreEntity
import kotlinx.coroutines.launch

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ScoreDatabase.getDatabase(application)
    private val dao = db.scoreDao()
    val score: LiveData<Int> = dao.getScore()

    init {
        viewModelScope.launch {
            val currentScore = dao.getLatestScore() ?: 0  // Get last saved score
            dao.insert(ScoreEntity(score = currentScore))
        }
    }

    fun increaseScore(value: Int) {
        viewModelScope.launch {
            val currentScore = dao.getLatestScore() ?: 0
            dao.insert(ScoreEntity(score = currentScore + value))
        }
    }

    fun decreaseScore(value: Int) {
        viewModelScope.launch {
            val currentScore = dao.getLatestScore() ?: 0
            dao.insert(ScoreEntity(score = currentScore - value))
        }
    }
}