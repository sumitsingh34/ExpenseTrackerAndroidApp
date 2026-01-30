package com.expensetracker.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.R
import com.expensetracker.data.AppDatabase
import com.expensetracker.data.Expense
import com.expensetracker.data.ExpenseTrackerRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CategoryExpensesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_CATEGORY_TOTAL = "category_total"
        const val EXTRA_MONTH = "month"
        const val EXTRA_YEAR = "year"
    }

    private lateinit var repository: ExpenseTrackerRepository
    private lateinit var adapter: CategoryExpenseAdapter
    private lateinit var expensesLiveData: LiveData<List<Expense>>

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private var categoryName: String = ""
    private var month: Int = 1
    private var year: Int = 2024

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_expenses)

        // Get intent extras
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
        val categoryTotal = intent.getDoubleExtra(EXTRA_CATEGORY_TOTAL, 0.0)
        month = intent.getIntExtra(EXTRA_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1)
        year = intent.getIntExtra(EXTRA_YEAR, Calendar.getInstance().get(Calendar.YEAR))

        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        repository = ExpenseTrackerRepository(
            database.expenseDao(),
            database.incomeDao(),
            database.categoryDao()
        )

        setupViews(categoryTotal)
        observeExpenses()
    }

    private fun setupViews(categoryTotal: Double) {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Header
        findViewById<TextView>(R.id.tvCategoryName).text = categoryName

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.YEAR, year)
        findViewById<TextView>(R.id.tvMonthYear).text = monthFormat.format(calendar.time)

        // Total amount
        findViewById<TextView>(R.id.tvTotalAmount).text = currencyFormat.format(categoryTotal)

        // RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = CategoryExpenseAdapter { expense ->
            showExpenseDialog(expense)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeExpenses() {
        expensesLiveData = repository.getExpensesByCategoryName(categoryName, month, year)
        expensesLiveData.observe(this) { expenses ->
            adapter.submitList(expenses)

            // Update count
            val countText = when (expenses.size) {
                0 -> "No expenses"
                1 -> "1 expense"
                else -> "${expenses.size} expenses"
            }
            findViewById<TextView>(R.id.tvExpenseCount).text = countText

            // Update total
            val total = expenses.sumOf { it.amount }
            findViewById<TextView>(R.id.tvTotalAmount).text = currencyFormat.format(total)

            // Show/hide empty state
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (expenses.isEmpty()) View.VISIBLE else View.GONE
            findViewById<RecyclerView>(R.id.recyclerView).visibility =
                if (expenses.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showExpenseDialog(expense: Expense) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_expense)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val autoCompleteCategory = dialog.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        val btnAddCategory = dialog.findViewById<ImageButton>(R.id.btnAddCategory)
        val etDescription = dialog.findViewById<EditText>(R.id.etDescription)
        val btnSelectDate = dialog.findViewById<Button>(R.id.btnSelectDate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)

        tvTitle.text = "Edit Expense"
        btnSave.text = "Update"
        btnDelete.visibility = View.VISIBLE

        // Setup category autocomplete
        val categoryList = mutableListOf<String>()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryList)
        autoCompleteCategory.setAdapter(categoryAdapter)
        autoCompleteCategory.threshold = 1

        // Load categories from database
        lifecycleScope.launch {
            val categories = repository.getAllCategoryNames()
            categoryList.clear()
            categoryList.addAll(categories)
            categoryAdapter.notifyDataSetChanged()
            autoCompleteCategory.setText(expense.category, false)
        }

        autoCompleteCategory.setOnClickListener {
            autoCompleteCategory.showDropDown()
        }

        autoCompleteCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteCategory.showDropDown()
            }
        }

        btnAddCategory.setOnClickListener {
            showAddCategoryDialog { newCategory ->
                if (newCategory.isNotBlank()) {
                    lifecycleScope.launch {
                        repository.addCategory(newCategory)
                    }
                    categoryList.add(newCategory)
                    categoryAdapter.notifyDataSetChanged()
                    autoCompleteCategory.setText(newCategory, false)
                    Toast.makeText(this, "Category '$newCategory' added", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val selectedDate = Calendar.getInstance()
        etAmount.setText(expense.amount.toString())
        etDescription.setText(expense.description)
        selectedDate.timeInMillis = expense.date
        btnSelectDate.text = dateFormat.format(selectedDate.time)

        btnSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, pickedYear, pickedMonth, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, pickedYear)
                selectedDate.set(Calendar.MONTH, pickedMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                btnSelectDate.text = dateFormat.format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteExpense(expense)
                    }
                    dialog.dismiss()
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString()
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = autoCompleteCategory.text.toString().trim()
            if (category.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!categoryList.contains(category)) {
                Toast.makeText(this, "Please select a valid category from the list", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedExpense = Expense(
                id = expense.id,
                amount = amount,
                category = category,
                description = etDescription.text.toString(),
                date = selectedDate.timeInMillis,
                month = selectedDate.get(Calendar.MONTH) + 1,
                year = selectedDate.get(Calendar.YEAR)
            )

            lifecycleScope.launch {
                repository.updateExpense(updatedExpense)
            }
            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddCategoryDialog(onCategoryAdded: (String) -> Unit) {
        val editText = EditText(this)
        editText.hint = "Enter category name"
        editText.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = editText.text.toString().trim()
                if (categoryName.isNotBlank()) {
                    onCategoryAdded(categoryName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
