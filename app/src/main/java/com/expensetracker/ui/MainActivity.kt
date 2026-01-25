package com.expensetracker.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.expensetracker.R
import com.expensetracker.data.Expense
import com.expensetracker.data.ExpenseCategory
import com.expensetracker.data.Income
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Month navigation
        val btnPrevMonth = findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)

        btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        btnNextMonth.setOnClickListener { viewModel.nextMonth() }

        // ViewPager and Tabs
        viewPager = findViewById(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Expenses"
                1 -> "Income"
                2 -> "Categories"
                else -> ""
            }
        }.attach()

        // FAB - opens based on current tab
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> showExpenseDialog(null)  // Expenses tab
                1 -> showIncomeDialog(null)   // Income tab
                2 -> showExpenseDialog(null)  // Categories tab - default to expense
            }
        }
    }

    private fun observeViewModel() {
        val tvCurrentMonth = findViewById<TextView>(R.id.tvCurrentMonth)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvTotalExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)

        // Observe month changes
        viewModel.currentMonth.observe(this) { month ->
            viewModel.currentYear.value?.let { year ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.YEAR, year)
                tvCurrentMonth.text = monthFormat.format(calendar.time)
            }
        }

        viewModel.currentYear.observe(this) { year ->
            viewModel.currentMonth.value?.let { month ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.YEAR, year)
                tvCurrentMonth.text = monthFormat.format(calendar.time)
            }
        }

        // Observe totals
        viewModel.totalIncome.observe(this) { income ->
            val incomeValue = income ?: 0.0
            tvTotalIncome.text = currencyFormat.format(incomeValue)
            updateBalance(incomeValue, viewModel.totalExpense.value ?: 0.0, tvBalance)
        }

        viewModel.totalExpense.observe(this) { expense ->
            val expenseValue = expense ?: 0.0
            tvTotalExpense.text = currencyFormat.format(expenseValue)
            updateBalance(viewModel.totalIncome.value ?: 0.0, expenseValue, tvBalance)
        }
    }

    private fun updateBalance(income: Double, expense: Double, tvBalance: TextView) {
        val balance = income - expense
        tvBalance.text = currencyFormat.format(balance)
        tvBalance.setTextColor(
            getColor(if (balance >= 0) R.color.income else R.color.expense)
        )
    }

    // Show expense dialog for add or edit
    fun showExpenseDialog(expense: Expense?) {
        val isEdit = expense != null
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_expense)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        // Make dialog wider
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val spinnerCategory = dialog.findViewById<Spinner>(R.id.spinnerCategory)
        val etDescription = dialog.findViewById<EditText>(R.id.etDescription)
        val btnSelectDate = dialog.findViewById<Button>(R.id.btnSelectDate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)

        tvTitle.text = if (isEdit) "Edit Expense" else "Add Expense"
        btnSave.text = if (isEdit) "Update" else "Save"

        // Show delete button only in edit mode
        btnDelete.visibility = if (isEdit) android.view.View.VISIBLE else android.view.View.GONE

        // Setup category spinner
        val categories = ExpenseCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Initialize date
        val selectedDate = Calendar.getInstance()
        if (isEdit) {
            // Pre-fill data for editing
            etAmount.setText(expense!!.amount.toString())
            etDescription.setText(expense.description)
            selectedDate.timeInMillis = expense.date

            // Select the correct category
            val categoryIndex = categories.indexOf(expense.category)
            if (categoryIndex >= 0) {
                spinnerCategory.setSelection(categoryIndex)
            }
        } else {
            val viewMonth = viewModel.currentMonth.value ?: (selectedDate.get(Calendar.MONTH) + 1)
            val viewYear = viewModel.currentYear.value ?: selectedDate.get(Calendar.YEAR)
            selectedDate.set(Calendar.MONTH, viewMonth - 1)
            selectedDate.set(Calendar.YEAR, viewYear)
        }

        btnSelectDate.text = dateFormat.format(selectedDate.time)

        // Date picker
        btnSelectDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    btnSelectDate.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        // Delete button
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteExpense(expense!!)
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

            val category = spinnerCategory.selectedItem.toString()
            val description = etDescription.text.toString()

            val newExpense = Expense(
                id = expense?.id ?: 0,
                amount = amount,
                category = category,
                description = description,
                date = selectedDate.timeInMillis,
                month = selectedDate.get(Calendar.MONTH) + 1,
                year = selectedDate.get(Calendar.YEAR)
            )

            if (isEdit) {
                viewModel.updateExpense(newExpense)
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertExpense(newExpense)
                Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // Show income dialog for add or edit
    fun showIncomeDialog(income: Income?) {
        val isEdit = income != null
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_income)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        // Make dialog wider
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val etSource = dialog.findViewById<EditText>(R.id.etSource)
        val etDescription = dialog.findViewById<EditText>(R.id.etDescription)
        val btnSelectDate = dialog.findViewById<Button>(R.id.btnSelectDate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)

        tvTitle.text = if (isEdit) "Edit Income" else "Add Income"
        btnSave.text = if (isEdit) "Update" else "Save"

        // Show delete button only in edit mode
        btnDelete.visibility = if (isEdit) android.view.View.VISIBLE else android.view.View.GONE

        // Initialize date
        val selectedDate = Calendar.getInstance()
        if (isEdit) {
            // Pre-fill data for editing
            etAmount.setText(income!!.amount.toString())
            etSource.setText(income.source)
            etDescription.setText(income.description)
            selectedDate.timeInMillis = income.date
        } else {
            val viewMonth = viewModel.currentMonth.value ?: (selectedDate.get(Calendar.MONTH) + 1)
            val viewYear = viewModel.currentYear.value ?: selectedDate.get(Calendar.YEAR)
            selectedDate.set(Calendar.MONTH, viewMonth - 1)
            selectedDate.set(Calendar.YEAR, viewYear)
        }

        btnSelectDate.text = dateFormat.format(selectedDate.time)

        // Date picker
        btnSelectDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    btnSelectDate.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        // Delete button
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Income")
                .setMessage("Are you sure you want to delete this income?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteIncome(income!!)
                    dialog.dismiss()
                    Toast.makeText(this, "Income deleted", Toast.LENGTH_SHORT).show()
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

            val source = etSource.text.toString()
            if (source.isEmpty()) {
                Toast.makeText(this, "Please enter a source", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val description = etDescription.text.toString()

            val newIncome = Income(
                id = income?.id ?: 0,
                amount = amount,
                source = source,
                description = description,
                date = selectedDate.timeInMillis,
                month = selectedDate.get(Calendar.MONTH) + 1,
                year = selectedDate.get(Calendar.YEAR)
            )

            if (isEdit) {
                viewModel.updateIncome(newIncome)
                Toast.makeText(this, "Income updated", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertIncome(newIncome)
                Toast.makeText(this, "Income added", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}
