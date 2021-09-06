package com.example.pong

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score")
data class Score (
    @ColumnInfo(name = "playerId") var playerId: Int,
    @ColumnInfo(name = "score") var score: Int,
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = false) var id: Long = 0
)