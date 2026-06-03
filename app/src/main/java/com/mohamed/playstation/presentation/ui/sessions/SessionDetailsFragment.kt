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
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.databinding.FragmentSessionDetailsBinding
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionDetailsFragment : Fragment() {

    private var _binding: FragmentSessionDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    private var sessionId: Long = 0L
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = arguments?.getLong("sessionId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProductsList()
        setupClickListeners()
        observeData()
    }

    private fun setupProductsList() {
        productAdapter = ProductAdapter(currencyCode = viewModel.currency.value)
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = productAdapter
    }

    private fun setupClickListeners() {
        binding.btnAddProduct.setOnClickListener {
            AddProductDialog.newInstance(sessionId)
                .show(childFragmentManager, "AddProductDialog")
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        viewModel.activeSessions,
                        viewModel.pausedSessions,
                        viewModel.currency,
                        viewModel.pricingSettings
                    ) { activeState, pausedState, currency, pricing ->
                        BaseSessionUIData(activeState, pausedState, currency, pricing)
                    }.combine(viewModel.getProductsForSession(sessionId)) { baseData, products ->
                        val activeSessions = if (baseData.activeState is UiState.Success) {
                            baseData.activeState.data.first
                        } else {
                            emptyList()
                        }
                        val activeTick = if (baseData.activeState is UiState.Success) {
                            baseData.activeState.data.second
                        } else {
                            0L
                        }
                        val pausedSessions = if (baseData.pausedState is UiState.Success) {
                            baseData.pausedState.data.first
                        } else {
                            emptyList()
                        }
                        val currentTick = if (activeTick > 0L) activeTick else System.currentTimeMillis()
                        val session = (activeSessions + pausedSessions).find { it.id == sessionId }
                        val isLoading = baseData.activeState is UiState.Loading ||
                            baseData.pausedState is UiState.Loading

                        SessionUIData(
                            isLoading = isLoading,
                            session = session,
                            currentTick = currentTick,
                            currency = baseData.currency,
                            pricing = baseData.pricing,
                            products = products
                        )
                    }.collect { data -> updateUI(data) }
                }
            }
        }
    }

    private fun updateUI(data: SessionUIData) {
        if (data.isLoading) return

        val session = data.session
        if (session == null || session.isEnded()) {
            findNavController().popBackStack()
            return
        }

        binding.tvDeviceName.text = buildString {
            append(session.deviceType)
            append(" #")
            append(session.deviceNumber)
        }

        binding.tvSessionModeLabel.isVisible = true
        binding.tvSessionModeLabel.text = buildString {
            append(if (session.isFixed()) {
                getString(R.string.session_mode_fixed)
            } else {
                getString(R.string.session_mode_open)
            })
            append(" • ")
            append(
                if (session.isMultiPlayer) {
                    getString(R.string.multiplayer)
                } else {
                    getString(R.string.single_player)
                }
            )
        }

        when {
            session.isActive() -> {
                binding.tvStatus.text = getString(R.string.status_running)
                binding.tvStatus.setTextColor(requireContext().getColor(R.color.status_active))
                binding.viewStatusDot.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        requireContext().getColor(R.color.status_active)
                    )
                binding.btnPauseResume.text = getString(R.string.pause_session)
                binding.btnPauseResume.setIconResource(R.drawable.ic_pause)
                binding.btnPauseResume.setOnClickListener { viewModel.pauseSession(session) }
            }
            session.isPaused() -> {
                binding.tvStatus.text = getString(R.string.status_paused)
                binding.tvStatus.setTextColor(requireContext().getColor(R.color.status_paused))
                binding.viewStatusDot.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        requireContext().getColor(R.color.status_paused)
                    )
                binding.btnPauseResume.text = getString(R.string.resume_session)
                binding.btnPauseResume.setIconResource(R.drawable.ic_play)
                binding.btnPauseResume.setOnClickListener { viewModel.resumeSession(session) }
            }
            else -> {
                binding.btnPauseResume.visibility = View.GONE
            }
        }

        binding.btnEndSession.setOnClickListener {
            showEndSessionDialog(session)
        }

        updateTimer(session, data.currentTick)

        val playCost = viewModel.playCostForSession(session, data.currentTick, data.pricing)
        val productCost = data.products.sumOf { it.getLineTotal() }
        val totalCost = playCost + productCost

        productAdapter.updateCurrency(data.currency)
        productAdapter.submitList(data.products)
        binding.tvNoProducts.isVisible = data.products.isEmpty()
        binding.rvProducts.isVisible = data.products.isNotEmpty()

        binding.tvPlayCost.text = CurrencyUtils.formatAmount(playCost, data.currency)
        binding.tvTotalCost.text = CurrencyUtils.formatAmount(totalCost, data.currency)
        binding.tvProductCost.text = CurrencyUtils.formatAmount(productCost, data.currency)
    }

    private fun updateTimer(session: Session, currentTick: Long) {
        binding.tvLargeTimer.text = SessionTimer.formatForSession(session, currentTick)

        if (session.isFixed()) {
            binding.tvTimerLabel.text = getString(R.string.time_remaining)
            val remaining = SessionTimer.getRemainingMs(session, currentTick) ?: 0L
            binding.tvLargeTimer.setTextColor(
                requireContext().getColor(
                    if (remaining <= 5 * 60_000 && session.isActive()) {
                        R.color.status_paused
                    } else {
                        R.color.ps_blue_primary
                    }
                )
            )
        } else {
            binding.tvTimerLabel.text = getString(R.string.elapsed_time)
            binding.tvLargeTimer.setTextColor(requireContext().getColor(R.color.ps_blue_primary))
        }
    }

    private fun showEndSessionDialog(session: Session) {
        EndSessionDialog.newInstance(session.id)
            .show(childFragmentManager, "EndSessionDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class SessionUIData(
        val isLoading: Boolean,
        val session: Session?,
        val currentTick: Long,
        val currency: String,
        val pricing: com.mohamed.playstation.core.utils.SessionPricing.PricingSettings,
        val products: List<SessionProduct>
    )

    private data class BaseSessionUIData(
        val activeState: UiState<Pair<List<Session>, Long>>,
        val pausedState: UiState<Pair<List<Session>, Long>>,
        val currency: String,
        val pricing: com.mohamed.playstation.core.utils.SessionPricing.PricingSettings
    )
}
