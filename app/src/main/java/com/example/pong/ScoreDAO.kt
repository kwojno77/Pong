package com.example.pong

import androidx.room.*

@Dao
interface ScoreDAO {

    @Query("select * from score")
    fun getAll() : List<Score>

    @Insert
    fun insert(score: Score)

    @Update
    fun updateScore(score: Score)

    @Query("delete from score")
    fun deleteAll()
}