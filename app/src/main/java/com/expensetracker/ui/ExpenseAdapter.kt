package com.expensetracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.R
import com.expensetracker.data.Expense
import com.expensetracker.data.ExpenseCategory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val onItemClick: (Expense) -> Unit,
    private val onItemLongClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val categoryIconBg: FrameLayout = itemView.findViewById(R.id.categoryIconBg)

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(expense: Expense) {
            tvCategory.text = expense.category
            tvDescription.text = expense.description.ifEmpty { "No description" }
            tvDate.text = dateFormat.format(Date(expense.date))
            tvAmount.text = "-${currencyFormat.format(expense.amount)}"

            // Set category icon and color
            val categoryColor = getCategoryColor(expense.category)
            tvCategoryIcon.text = expense.category.first().toString()
            categoryIconBg.background.setTint(ContextCompat.getColor(itemView.context, categoryColor))

            itemView.setOnClickListener { onItemClick(expense) }
            itemView.setOnLongClickListener {
                onItemLongClick(expense)
                true
            }
        }

        private fun getCategoryColor(category: String): Int {
            return when (category) {
                ExpenseCategory.GROCERIES.displayName -> R.color.cat_groceries
                ExpenseCategory.ENTERTAINMENT.displayName -> R.color.cat_entertainment
                ExpenseCategory.CAB_RIDE.displayName -> R.color.cat_cab
                ExpenseCategory.RESTAURANT.displayName -> R.color.cat_restaurant
                ExpenseCategory.TRAVEL.displayName -> R.color.cat_travel
                ExpenseCategory.GIFTS.displayName -> R.color.cat_gifts
                ExpenseCategory.UTILITIES.displayName -> R.color.cat_utilities
                ExpenseCategory.SHOPPING.displayName -> R.color.cat_shopping
                ExpenseCategory.HEALTH.displayName -> R.color.cat_health
                else -> R.color.cat_other
            }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
