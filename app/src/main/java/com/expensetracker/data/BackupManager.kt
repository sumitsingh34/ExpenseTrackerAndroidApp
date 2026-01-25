package com.expensetracker.data

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupData(
    val expenses: List<Expense>,
    val incomes: List<Income>,
    val backupDate: Long = System.currentTimeMillis()
)

class BackupManager(private val context: Context) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // Get the backup directory in external storage
    private fun getBackupDir(): File {
        val dir = File(context.getExternalFilesDir(null), "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Export data to JSON file
    suspend fun exportToFile(expenses: List<Expense>, incomes: List<Income>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val backupData = BackupData(expenses, incomes)
                val json = gson.toJson(backupData)

                val fileName = "expense_backup_${dateFormat.format(Date())}.json"
                val file = File(getBackupDir(), fileName)
                file.writeText(json)

                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Import data from JSON file
    suspend fun importFromFile(filePath: String): Result<BackupData> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found"))
                }

                val json = file.readText()
                val backupData = gson.fromJson(json, BackupData::class.java)
                Result.success(backupData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get list of available backup files
    fun getBackupFiles(): List<File> {
        val dir = getBackupDir()
        return dir.listFiles { file -> file.extension == "json" }?.toList() ?: emptyList()
    }

    // Export to Downloads folder (more accessible)
    suspend fun exportToDownloads(expenses: List<Expense>, incomes: List<Income>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val backupData = BackupData(expenses, incomes)
                val json = gson.toJson(backupData)

                val fileName = "expense_backup_${dateFormat.format(Date())}.json"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(json)

                Result.success(file.absolutePath)
            } catch (e: Exception) {
                // Fallback to app-specific storage
                exportToFile(expenses, incomes)
            }
        }
    }

    // Save to SharedPreferences as backup (always available)
    fun saveToPreferences(expenses: List<Expense>, incomes: List<Income>) {
        val prefs = context.getSharedPreferences("expense_tracker_backup", Context.MODE_PRIVATE)
        val backupData = BackupData(expenses, incomes)
        val json = gson.toJson(backupData)
        prefs.edit().putString("backup_data", json).apply()
    }

    // Load from SharedPreferences
    fun loadFromPreferences(): BackupData? {
        val prefs = context.getSharedPreferences("expense_tracker_backup", Context.MODE_PRIVATE)
        val json = prefs.getString("backup_data", null) ?: return null
        return try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
