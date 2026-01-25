package com.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val source: String,
    val description: String,
    val date: Long, // timestamp in milliseconds
    val month: Int, // 1-12
    val year: Int
)
