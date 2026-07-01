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
import com.mohamed.playstation.databinding.FragmentSessionsBinding
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
                try {
                    findNavController().navigate(
                        com.mohamed.playstation.R.id.action_sessionsFragment_to_sessionDetailsFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    // ignore duplicate navigation
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
                val showCompleted = checkedId == com.mohamed.playstation.R.id.chipCompleted
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
                var cachedSessions: List<com.mohamed.playstation.domain.model.Session> = emptyList()
                
                launch {
                    combine(
                        viewModel.activeSessions,
                        viewModel.pausedSessions
                    ) { activeState, pausedState ->
                        val isLoading =
                            activeState is UiState.Loading && pausedState is UiState.Loading

                        val activeSessions =
                            if (activeState is UiState.Success) activeState.data.first else if (activeState is UiState.Loading) emptyList() else null
                        val activeTick =
                            if (activeState is UiState.Success) activeState.data.second else 0L

                        val pausedSessions =
                            if (pausedState is UiState.Success) pausedState.data.first else if (pausedState is UiState.Loading) emptyList() else null

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

                        Triple(isLoading, allSessions, activeTick)
                    }.collect { (isLoading, sessions, tick) ->
                        if (isLoading) {
                            binding.progressBar.isVisible = true
                            binding.rvSessions.isVisible = false
                            binding.tvEmptyState.isVisible = false
                        } else if (sessions.isEmpty()) {
                            binding.progressBar.isVisible = false
                            binding.rvSessions.isVisible = false
                            binding.tvEmptyState.isVisible = true
                        } else {
                            binding.progressBar.isVisible = false
                            binding.tvEmptyState.isVisible = false
                            binding.rvSessions.isVisible = true

                            sessionAdapter.updateTick(tick)
                            sessionAdapter.submitList(sessions)

                            binding.chipRunning.text = getString(com.mohamed.playstation.R.string.tab_running) + " (${sessions.size})"
                        }
                    }
                }

                // --- Completed sessions (new — reuses existing flow) ---
                launch {
                    viewModel.completedSessions.collect { sessions ->
                        val isEmpty = sessions.isEmpty()
                        binding.layoutEmptyCompleted.isVisible = isEmpty
                        binding.rvCompletedSessions.isVisible = !isEmpty
                        if (!isEmpty) {
                            completedAdapter.submitList(sessions)
                            binding.chipCompleted.text = getString(com.mohamed.playstation.R.string.tab_completed) + " (${sessions.size})"
                            if (binding.layoutCompleted.isVisible && binding.rvCompletedSessions.adapter?.itemCount != sessions.size) {
                                // removed scheduleLayoutAnimation
                            }
                        } else {
                            binding.chipCompleted.text = getString(com.mohamed.playstation.R.string.tab_completed)
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
        super.onDestroyView()
        _binding = null
    }
}