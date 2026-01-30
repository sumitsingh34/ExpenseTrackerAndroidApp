package com.expensetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getExpensesByMonth(month: Int, year: Int): LiveData<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE month = :month AND year = :year")
    fun getTotalExpenseByMonth(month: Int, year: Int): LiveData<Double?>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<Expense>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE month = :month AND year = :year GROUP BY category")
    fun getExpensesByCategory(month: Int, year: Int): LiveData<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE category = :category AND month = :month AND year = :year ORDER BY date DESC")
    fun getExpensesByCategoryName(category: String, month: Int, year: Int): LiveData<List<Expense>>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
