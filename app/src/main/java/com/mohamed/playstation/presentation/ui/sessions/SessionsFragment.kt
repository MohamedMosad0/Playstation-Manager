package com.mohamed.playstation.presentation.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.FragmentSessionsBinding
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Fragment لعرض وإدارة الجلسات
 *
 * Running tab  — existing implementation, untouched.
 * Completed tab — filters via existing SessionViewModel.completedSessions StateFlow.
 *                  No new DAO / Repository / UseCase / ViewModel.
 */
@AndroidEntryPoint
class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var completedAdapter: CompletedSessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabs()
        setupClickListeners()
        observeData()
    }

    /**
     * إعداد RecyclerViews
     */
    private fun setupRecyclerViews() {
        // Running sessions — grid (existing behavior)
        sessionAdapter = SessionAdapter(
            tick = 0L,
            onCardClick = { session ->
                val bundle = Bundle().apply {
                    putLong("sessionId", session.id)
                }
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.sessionsFragment) {
                    navController.navigate(
                        R.id.action_sessionsFragment_to_sessionDetailsFragment,
                        bundle
                    )
                }
            }
        )

        binding.rvSessions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
            setHasFixedSize(true)
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        // Completed sessions — vertical list, static (no ticker)
        completedAdapter = CompletedSessionAdapter(
            currency = viewModel.currency.value
        )
        binding.rvCompletedSessions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * إعداد التبويبات (Chips)
     */
    private fun setupTabs() {
        binding.chipGroupTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val showCompleted = checkedId == R.id.chipCompleted
                binding.layoutRunning.isVisible = !showCompleted
                binding.layoutCompleted.isVisible = showCompleted
                
                if (showCompleted) {
                    binding.fabNewSession.hide()
                } else {
                    binding.fabNewSession.show()
                }
            }
        }
    }

    /**
     * إعداد أزرار الضغط
     */
    private fun setupClickListeners() {
        binding.fabNewSession.setOnClickListener {
            showNewSessionDialog()
        }
    }

    /**
     * مراقبة البيانات من ViewModel
     */
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // --- Running sessions (existing logic, unchanged) ---
                var cachedSessions: List<Session> = emptyList()
                // Track previous inner list references to skip redundant submitList on tick-only updates.
                // The combine creates a new concatenated list every emission via `+`, so we must
                // track the source lists whose identity only changes when Room emits new data.
                var lastActiveRef: List<Session>? = null
                var lastPausedRef: List<Session>? = null
                
                launch {
                    combine(
                        viewModel.activeSessions,
                        viewModel.pausedSessions
                    ) { activeState, pausedState ->
                        val isLoading =
                            activeState is UiState.Loading && pausedState is UiState.Loading

                        val activeSessions = when (activeState) {
                            is UiState.Success -> activeState.data.first
                            is UiState.Empty, is UiState.Loading -> emptyList()
                            else -> null
                        }
                        val activeTick =
                            if (activeState is UiState.Success) activeState.data.second else 0L

                        val pausedSessions = when (pausedState) {
                            is UiState.Success -> pausedState.data.first
                            is UiState.Empty, is UiState.Loading -> emptyList()
                            else -> null
                        }

                        if (activeState is UiState.Error) {
                            android.widget.Toast.makeText(requireContext(), activeState.message.asString(requireContext()), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        if (pausedState is UiState.Error) {
                            android.widget.Toast.makeText(requireContext(), pausedState.message.asString(requireContext()), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        
                        val newActive = activeSessions ?: cachedSessions.filter { it.status == "active" }
                        val newPaused = pausedSessions ?: cachedSessions.filter { it.status == "paused" }
                        
                        val allSessions = newActive + newPaused
                        cachedSessions = allSessions

                        // Return inner list references alongside combined result for identity tracking
                        data class SessionTick(
                            val isLoading: Boolean,
                            val sessions: List<Session>,
                            val tick: Long,
                            val activeRef: List<Session>,
                            val pausedRef: List<Session>
                        )
                        SessionTick(isLoading, allSessions, activeTick, newActive, newPaused)
                    }.collect { result ->
                        if (result.isLoading) {
                            binding.progressBar.isVisible = true
                            binding.rvSessions.isVisible = false
                            binding.tvEmptyState.isVisible = false
                        } else if (result.sessions.isEmpty()) {
                            binding.progressBar.isVisible = false
                            binding.rvSessions.isVisible = false
                            binding.tvEmptyState.isVisible = true
                            lastActiveRef = null
                            lastPausedRef = null
                        } else {
                            binding.progressBar.isVisible = false
                            binding.tvEmptyState.isVisible = false
                            binding.rvSessions.isVisible = true

                            // Only run DiffUtil when the session data actually changed.
                            // On tick-only updates the inner Room lists keep the same reference,
                            // so this skips redundant DiffUtil comparisons every second.
                            val listChanged = result.activeRef !== lastActiveRef || result.pausedRef !== lastPausedRef
                            if (listChanged) {
                                sessionAdapter.submitList(result.sessions)
                                lastActiveRef = result.activeRef
                                lastPausedRef = result.pausedRef
                                binding.chipRunning.text = getString(R.string.tab_running) + " (${result.sessions.size})"
                            }
                            sessionAdapter.updateTick(result.tick)
                        }
                    }
                }

                // --- Completed sessions (new — reuses existing flow) ---
                launch {
                    combine(
                        viewModel.completedSessions,
                        viewModel.currency
                    ) { sessions, currency ->
                        Pair(sessions, currency)
                    }.collect { (sessions, currency) ->
                        completedAdapter.updateCurrency(currency)
                        val isEmpty = sessions.isEmpty()
                        binding.layoutEmptyCompleted.isVisible = isEmpty
                        binding.rvCompletedSessions.isVisible = !isEmpty
                        if (!isEmpty) {
                            completedAdapter.submitList(sessions)
                            binding.chipCompleted.text = getString(R.string.tab_completed) + " (${sessions.size})"
                        } else {
                            binding.chipCompleted.text = getString(R.string.tab_completed)
                        }
                    }
                }
            }
        }
    }

    /**
     * عرض Dialog لإضافة جلسة جديدة
     */
    private fun showNewSessionDialog() {
        val dialog = NewSessionDialog()
        dialog.show(childFragmentManager, "NewSessionDialog")
    }

    override fun onDestroyView() {
        binding.rvSessions.adapter = null
        binding.rvCompletedSessions.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
