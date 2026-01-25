package com.expensetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: Income)

    @Update
    suspend fun update(income: Income)

    @Delete
    suspend fun delete(income: Income)

    @Query("SELECT * FROM incomes WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getIncomesByMonth(month: Int, year: Int): LiveData<List<Income>>

    @Query("SELECT SUM(amount) FROM incomes WHERE month = :month AND year = :year")
    fun getTotalIncomeByMonth(month: Int, year: Int): LiveData<Double?>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): LiveData<List<Income>>
}
