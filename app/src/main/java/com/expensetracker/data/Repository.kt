package com.expensetracker.data

import androidx.lifecycle.LiveData

class ExpenseTrackerRepository(
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val categoryDao: CategoryDao
) {

    // Expense operations
    suspend fun insertExpense(expense: Expense) = expenseDao.insert(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    fun getExpensesByMonth(month: Int, year: Int): LiveData<List<Expense>> =
        expenseDao.getExpensesByMonth(month, year)

    fun getTotalExpenseByMonth(month: Int, year: Int): LiveData<Double?> =
        expenseDao.getTotalExpenseByMonth(month, year)

    fun getAllExpenses(): LiveData<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByCategory(month: Int, year: Int): LiveData<List<CategoryTotal>> =
        expenseDao.getExpensesByCategory(month, year)

    fun getExpensesByCategoryName(category: String, month: Int, year: Int): LiveData<List<Expense>> =
        expenseDao.getExpensesByCategoryName(category, month, year)

    // Income operations
    suspend fun insertIncome(income: Income) = incomeDao.insert(income)
    suspend fun updateIncome(income: Income) = incomeDao.update(income)
    suspend fun deleteIncome(income: Income) = incomeDao.delete(income)

    fun getIncomesByMonth(month: Int, year: Int): LiveData<List<Income>> =
        incomeDao.getIncomesByMonth(month, year)

    fun getTotalIncomeByMonth(month: Int, year: Int): LiveData<Double?> =
        incomeDao.getTotalIncomeByMonth(month, year)

    fun getAllIncomes(): LiveData<List<Income>> = incomeDao.getAllIncomes()

    suspend fun getAllExpensesSync(): List<Expense> = expenseDao.getAllExpensesSync()

    suspend fun getAllIncomesSync(): List<Income> = incomeDao.getAllIncomesSync()

    // Category operations
    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun getAllCategoryNames(): List<String> = categoryDao.getAllCategoryNames()

    suspend fun addCategory(name: String) {
        if (categoryDao.categoryExists(name) == 0) {
            categoryDao.insert(Category(name = name, isCustom = true))
        }
    }

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllCategoriesSync()
}
