package com.expensetracker.ui

import android.app.Application
import androidx.lifecycle.*
import com.expensetracker.data.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseTrackerRepository

    private val _currentMonth = MutableLiveData<Int>()
    private val _currentYear = MutableLiveData<Int>()

    val currentMonth: LiveData<Int> = _currentMonth
    val currentYear: LiveData<Int> = _currentYear

    // Combined month/year trigger
    private val monthYearTrigger = MediatorLiveData<Pair<Int, Int>>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseTrackerRepository(
            database.expenseDao(),
            database.incomeDao(),
            database.categoryDao()
        )

        val calendar = Calendar.getInstance()
        _currentMonth.value = calendar.get(Calendar.MONTH) + 1
        _currentYear.value = calendar.get(Calendar.YEAR)

        monthYearTrigger.addSource(_currentMonth) { month ->
            val year = _currentYear.value ?: calendar.get(Calendar.YEAR)
            monthYearTrigger.value = Pair(month, year)
        }
        monthYearTrigger.addSource(_currentYear) { year ->
            val month = _currentMonth.value ?: (calendar.get(Calendar.MONTH) + 1)
            monthYearTrigger.value = Pair(month, year)
        }

        // Initialize default categories on first run
        viewModelScope.launch {
            initializeCategories()
        }
    }

    private suspend fun initializeCategories() {
        val existingCategories = repository.getAllCategoryNames()
        if (existingCategories.isEmpty()) {
            ExpenseCategory.values().forEach { category ->
                repository.addCategory(category.displayName)
            }
        }
    }

    val expenses: LiveData<List<Expense>> = monthYearTrigger.switchMap { (month, year) ->
        repository.getExpensesByMonth(month, year)
    }

    val incomes: LiveData<List<Income>> = monthYearTrigger.switchMap { (month, year) ->
        repository.getIncomesByMonth(month, year)
    }

    val totalExpense: LiveData<Double?> = monthYearTrigger.switchMap { (month, year) ->
        repository.getTotalExpenseByMonth(month, year)
    }

    val totalIncome: LiveData<Double?> = monthYearTrigger.switchMap { (month, year) ->
        repository.getTotalIncomeByMonth(month, year)
    }

    val categoryTotals: LiveData<List<CategoryTotal>> = monthYearTrigger.switchMap { (month, year) ->
        repository.getExpensesByCategory(month, year)
    }

    val categories: LiveData<List<Category>> = repository.getAllCategories()

    fun setMonth(month: Int, year: Int) {
        _currentMonth.value = month
        _currentYear.value = year
    }

    fun previousMonth() {
        val month = _currentMonth.value ?: return
        val year = _currentYear.value ?: return

        if (month == 1) {
            _currentMonth.value = 12
            _currentYear.value = year - 1
        } else {
            _currentMonth.value = month - 1
        }
    }

    fun nextMonth() {
        val month = _currentMonth.value ?: return
        val year = _currentYear.value ?: return

        if (month == 12) {
            _currentMonth.value = 1
            _currentYear.value = year + 1
        } else {
            _currentMonth.value = month + 1
        }
    }

    fun insertExpense(expense: Expense) = viewModelScope.launch {
        repository.insertExpense(expense)
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        repository.updateExpense(expense)
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        repository.deleteExpense(expense)
    }

    fun insertIncome(income: Income) = viewModelScope.launch {
        repository.insertIncome(income)
    }

    fun updateIncome(income: Income) = viewModelScope.launch {
        repository.updateIncome(income)
    }

    fun deleteIncome(income: Income) = viewModelScope.launch {
        repository.deleteIncome(income)
    }

    suspend fun getAllExpenses(): List<Expense> = repository.getAllExpensesSync()

    suspend fun getAllIncomes(): List<Income> = repository.getAllIncomesSync()

    suspend fun getAllCategoryNames(): List<String> = repository.getAllCategoryNames()

    fun addCategory(name: String) = viewModelScope.launch {
        repository.addCategory(name)
    }

    suspend fun getAllCategoriesSync(): List<Category> = repository.getAllCategoriesSync()
}
