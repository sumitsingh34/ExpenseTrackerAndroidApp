package com.expensetracker.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.expensetracker.R
import com.expensetracker.data.BackupManager
import com.expensetracker.data.Expense
import com.expensetracker.data.ExpenseCategory
import com.expensetracker.data.Income
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private lateinit var viewPager: ViewPager2
    private lateinit var backupManager: BackupManager

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importDataFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        backupManager = BackupManager(this)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Month navigation
        val btnPrevMonth = findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val tvCurrentMonth = findViewById<TextView>(R.id.tvCurrentMonth)

        btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        tvCurrentMonth.setOnClickListener { showMonthPickerDialog() }

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
                0 -> showExpenseDialog(null)
                1 -> showIncomeDialog(null)
                2 -> showExpenseDialog(null)
            }
        }
    }

    private fun observeViewModel() {
        val tvCurrentMonth = findViewById<TextView>(R.id.tvCurrentMonth)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvTotalExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)

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
        tvBalance.setTextColor(getColor(if (balance >= 0) R.color.income else R.color.expense))
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val btnExportData = dialog.findViewById<MaterialButton>(R.id.btnExportData)
        val btnImportData = dialog.findViewById<MaterialButton>(R.id.btnImportData)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnClose)

        btnExportData.setOnClickListener {
            dialog.dismiss()
            exportData()
        }

        btnImportData.setOnClickListener {
            dialog.dismiss()
            importFileLauncher.launch("application/json")
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showMonthPickerDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_month_picker)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvYear = dialog.findViewById<TextView>(R.id.tvYear)
        val btnPrevYear = dialog.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = dialog.findViewById<ImageButton>(R.id.btnNextYear)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)

        var selectedYear = viewModel.currentYear.value ?: Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = viewModel.currentMonth.value ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

        tvYear.text = selectedYear.toString()

        btnPrevYear.setOnClickListener {
            selectedYear--
            tvYear.text = selectedYear.toString()
            updateMonthHighlight(dialog, currentMonth, selectedYear)
        }

        btnNextYear.setOnClickListener {
            selectedYear++
            tvYear.text = selectedYear.toString()
            updateMonthHighlight(dialog, currentMonth, selectedYear)
        }

        // Month buttons
        val monthIds = listOf(
            R.id.monthJan, R.id.monthFeb, R.id.monthMar, R.id.monthApr,
            R.id.monthMay, R.id.monthJun, R.id.monthJul, R.id.monthAug,
            R.id.monthSep, R.id.monthOct, R.id.monthNov, R.id.monthDec
        )

        monthIds.forEachIndexed { index, id ->
            val monthView = dialog.findViewById<TextView>(id)
            monthView.setOnClickListener {
                viewModel.setMonth(index + 1, selectedYear)
                dialog.dismiss()
            }
        }

        // Highlight current selection
        updateMonthHighlight(dialog, currentMonth, selectedYear)

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun updateMonthHighlight(dialog: Dialog, currentMonth: Int, selectedYear: Int) {
        val monthIds = listOf(
            R.id.monthJan, R.id.monthFeb, R.id.monthMar, R.id.monthApr,
            R.id.monthMay, R.id.monthJun, R.id.monthJul, R.id.monthAug,
            R.id.monthSep, R.id.monthOct, R.id.monthNov, R.id.monthDec
        )

        val currentSelectedMonth = viewModel.currentMonth.value ?: currentMonth
        val currentSelectedYear = viewModel.currentYear.value ?: Calendar.getInstance().get(Calendar.YEAR)

        monthIds.forEachIndexed { index, id ->
            val monthView = dialog.findViewById<TextView>(id)
            if (index + 1 == currentSelectedMonth && selectedYear == currentSelectedYear) {
                monthView.setBackgroundResource(R.drawable.month_selected_background)
                monthView.setTextColor(getColor(android.R.color.white))
            } else {
                monthView.setBackgroundResource(android.R.color.transparent)
                monthView.setTextColor(getColor(R.color.text_primary))
            }
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            val expenses = viewModel.getAllExpenses()
            val incomes = viewModel.getAllIncomes()

            if (expenses.isEmpty() && incomes.isEmpty()) {
                Toast.makeText(this@MainActivity, "No data to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = backupManager.exportToDownloads(expenses, incomes)
            result.onSuccess { path ->
                Toast.makeText(this@MainActivity, "Data exported to Downloads folder", Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Export failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importDataFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.readText() ?: return@launch

                val gson = com.google.gson.Gson()
                val backupData = gson.fromJson(json, com.expensetracker.data.BackupData::class.java)

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Import Data")
                    .setMessage("This will add ${backupData.expenses.size} expenses and ${backupData.incomes.size} incomes. Continue?")
                    .setPositiveButton("Import") { _, _ ->
                        lifecycleScope.launch {
                            backupData.expenses.forEach { expense ->
                                viewModel.insertExpense(expense.copy(id = 0))
                            }
                            backupData.incomes.forEach { income ->
                                viewModel.insertIncome(income.copy(id = 0))
                            }
                            Toast.makeText(this@MainActivity, "Data imported successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showExpenseDialog(expense: Expense?) {
        val isEdit = expense != null
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

        tvTitle.text = if (isEdit) "Edit Expense" else "Add Expense"
        btnSave.text = if (isEdit) "Update" else "Save"
        btnDelete.visibility = if (isEdit) View.VISIBLE else View.GONE

        // Setup category autocomplete
        val categoryList = mutableListOf<String>()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryList)
        autoCompleteCategory.setAdapter(categoryAdapter)
        autoCompleteCategory.threshold = 1 // Start showing suggestions after 1 character

        // Load categories from database
        lifecycleScope.launch {
            val categories = viewModel.getAllCategoryNames()
            categoryList.clear()
            categoryList.addAll(categories)
            categoryAdapter.notifyDataSetChanged()

            // Set existing category if editing
            if (isEdit && expense != null) {
                autoCompleteCategory.setText(expense.category, false)
            }
        }

        // Show dropdown when clicked
        autoCompleteCategory.setOnClickListener {
            autoCompleteCategory.showDropDown()
        }

        autoCompleteCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteCategory.showDropDown()
            }
        }

        // Add new category button
        btnAddCategory.setOnClickListener {
            showAddCategoryDialog { newCategory ->
                if (newCategory.isNotBlank()) {
                    viewModel.addCategory(newCategory)
                    categoryList.add(newCategory)
                    categoryAdapter.notifyDataSetChanged()
                    autoCompleteCategory.setText(newCategory, false)
                    Toast.makeText(this, "Category '$newCategory' added", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val selectedDate = Calendar.getInstance()
        if (isEdit) {
            etAmount.setText(expense!!.amount.toString())
            etDescription.setText(expense.description)
            selectedDate.timeInMillis = expense.date
        } else {
            val viewMonth = viewModel.currentMonth.value ?: (selectedDate.get(Calendar.MONTH) + 1)
            val viewYear = viewModel.currentYear.value ?: selectedDate.get(Calendar.YEAR)
            selectedDate.set(Calendar.MONTH, viewMonth - 1)
            selectedDate.set(Calendar.YEAR, viewYear)
        }

        btnSelectDate.text = dateFormat.format(selectedDate.time)

        btnSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
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

            val category = autoCompleteCategory.text.toString().trim()
            if (category.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate category exists in the list
            if (!categoryList.contains(category)) {
                Toast.makeText(this, "Please select a valid category from the list", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newExpense = Expense(
                id = expense?.id ?: 0,
                amount = amount,
                category = category,
                description = etDescription.text.toString(),
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

    fun showIncomeDialog(income: Income?) {
        val isEdit = income != null
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_income)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
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
        btnDelete.visibility = if (isEdit) View.VISIBLE else View.GONE

        val selectedDate = Calendar.getInstance()
        if (isEdit) {
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

        btnSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                btnSelectDate.text = dateFormat.format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

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

            val newIncome = Income(
                id = income?.id ?: 0,
                amount = amount,
                source = source,
                description = etDescription.text.toString(),
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
