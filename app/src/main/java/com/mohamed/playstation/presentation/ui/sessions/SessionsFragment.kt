package com.mohamed.playstation.presentation.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.mohamed.playstation.databinding.FragmentSessionsBinding
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Fragment لعرض وإدارة الجلسات
 */
@AndroidEntryPoint
class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    private lateinit var sessionAdapter: SessionAdapter

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
        setupClickListeners()
        observeData()
    }

    /**
     * إعداد RecyclerViews
     */
    private fun setupRecyclerViews() {
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

                }
            }
        )

        binding.rvSessions.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = sessionAdapter
            setHasFixedSize(true)
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
                // Combine active and paused sessions to form one flat list
                launch {
                    combine(
                        viewModel.activeSessions,
                        viewModel.pausedSessions
                    ) { activeState, pausedState ->
                        val isLoading =
                            activeState is UiState.Loading && pausedState is UiState.Loading

                        val activeSessions =
                            if (activeState is UiState.Success) activeState.data.first else emptyList()
                        val activeTick =
                            if (activeState is UiState.Success) activeState.data.second else 0L

                        val pausedSessions =
                            if (pausedState is UiState.Success) pausedState.data.first else emptyList()

                        val allSessions = activeSessions + pausedSessions
                        Triple(isLoading, allSessions, activeTick)
                    }.collect { (isLoading, sessions, tick) ->
                        if (isLoading) {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvSessions.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        } else if (sessions.isEmpty()) {
                            binding.progressBar.visibility = View.GONE
                            binding.rvSessions.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvSessions.visibility = View.VISIBLE

                            sessionAdapter.updateTick(tick)
                            sessionAdapter.submitList(sessions)
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