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
import com.expensetracker.data.CategoryTotal
import com.expensetracker.data.ExpenseCategory
import java.text.NumberFormat
import java.util.*

class CategoryAdapter : ListAdapter<CategoryTotal, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val categoryIconBg: FrameLayout = itemView.findViewById(R.id.categoryIconBg)

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(categoryTotal: CategoryTotal) {
            tvCategory.text = categoryTotal.category
            tvAmount.text = currencyFormat.format(categoryTotal.total)

            // Set category icon and color
            val categoryColor = getCategoryColor(categoryTotal.category)
            tvCategoryIcon.text = categoryTotal.category.first().toString()
            categoryIconBg.background.setTint(ContextCompat.getColor(itemView.context, categoryColor))
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

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem == newItem
        }
    }
}
