package com.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long, // timestamp in milliseconds
    val month: Int, // 1-12
    val year: Int
)

enum class ExpenseCategory(val displayName: String) {
    GROCERIES("Groceries"),
    ENTERTAINMENT("Entertainment"),
    CAB_RIDE("Cab Ride"),
    RESTAURANT("Restaurant"),
    TRAVEL("Travel"),
    GIFTS("Gifts"),
    UTILITIES("Utilities"),
    SHOPPING("Shopping"),
    HEALTH("Health"),
    OTHER("Other")
}
