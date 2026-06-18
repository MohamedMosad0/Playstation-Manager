package com.mohamed.playstation.presentation.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.FragmentReportsBinding

/**
 * ReportsFragment — Navigation hub only.
 *
 * Responsibilities:
 *  - Navigate to ReceiptsFragment via the Receipts card.
 *  - Navigate to ExpensesFragment via the Expenses card.
 *  - Show "Coming Soon" placeholder for Revenue and Profit cards.
 *
 * No ViewModel, No Repository, No UseCases, No Database, No business logic.
 */
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        // Receipts → navigate to existing ReceiptsFragment
        binding.cardReceipts.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_receiptsFragment)
        }

        // Expenses → navigate to existing ExpensesFragment
        binding.cardExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_expensesFragment)
        }

        // Revenue → Coming Soon, no navigation
        // Profit → Coming Soon, no navigation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
