package com.expensetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.R

class IncomeListFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: IncomeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_income_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = IncomeAdapter(
            onItemClick = { income ->
                // Open edit dialog
                (activity as? MainActivity)?.showIncomeDialog(income)
            },
            onItemLongClick = { income ->
                // Also open edit dialog on long click
                (activity as? MainActivity)?.showIncomeDialog(income)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.incomes.observe(viewLifecycleOwner) { incomes ->
            adapter.submitList(incomes)
            tvEmpty.visibility = if (incomes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
