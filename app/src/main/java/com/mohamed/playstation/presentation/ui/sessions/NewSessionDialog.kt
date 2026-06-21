package com.mohamed.playstation.presentation.ui.sessions

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.domain.usecase.DuplicateDeviceSessionException
import com.mohamed.playstation.databinding.DialogNewSessionBinding
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewSessionDialog : DialogFragment() {

    private var _binding: DialogNewSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels({ requireParentFragment() })

    @javax.inject.Inject
    lateinit var settingsManager: com.mohamed.playstation.data.local.SettingsManager

    private var isModeUpdating = false
    private var isConsoleUpdating = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNewSessionBinding.inflate(layoutInflater)

        setupDeviceNumberSpinner()
        setupListeners()
        observePricing()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_session)
            .setView(binding.root)
            .setPositiveButton(R.string.start_session, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                lifecycleScope.launch {
                    try {
                        if (startSession()) {
                            checkExactAlarmAndDismiss(dialog)
                        }
                    } catch (duplicateSession: DuplicateDeviceSessionException) {
                        Snackbar.make(
                            binding.root,
                            getString(
                                R.string.device_session_exists,
                                duplicateSession.deviceLabel
                            ),
                            Snackbar.LENGTH_LONG
                        ).show()
                    } catch (error: Exception) {
                        Snackbar.make(
                            binding.root,
                            error.message ?: getString(R.string.error_occurred),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        return dialog
    }
    private fun checkExactAlarmAndDismiss(parentDialog: Dialog) {
        val sessionMode = getSelectedSessionMode()
        if (sessionMode != AppConstants.SESSION_MODE_FIXED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            parentDialog.dismiss()
            return
        }

        lifecycleScope.launch {
            val isDismissed = settingsManager.exactAlarmPromptDismissedFlow.first()
            val alarmManager = requireContext().getSystemService(android.app.AlarmManager::class.java)

            if (!isDismissed && !alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("إذن الإنذار الدقيق")
                    .setMessage("للحصول على إنهاء دقيق للجلسات في وقتها بالثانية، يُفضل تفعيل الإنذارات الدقيقة.")
                    .setPositiveButton("تفعيل") { _, _ ->
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                        startActivity(intent)
                        parentDialog.dismiss()
                    }
                    .setNegativeButton("لاحقاً") { _, _ ->
                        lifecycleScope.launch {
                            settingsManager.setExactAlarmPromptDismissed(true)
                            parentDialog.dismiss()
                        }
                    }
                    .setCancelable(false)
                    .show()
            } else {
                parentDialog.dismiss()
            }
        }
    }

    private fun setupDeviceNumberSpinner() {
        val deviceNumbers = (1..10).toList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            deviceNumbers
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDeviceNumber.adapter = adapter
    }

    private fun setupListeners() {
        binding.toggleConsoleType.addOnButtonCheckedListener { _, _, _ ->
            if (!isConsoleUpdating) updatePricePreview()
        }

        binding.toggleSessionMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isModeUpdating && isChecked) {
                val isFixed = checkedId == R.id.btnModeFixed
                binding.tilFixedDuration.isVisible = isFixed
                if (isFixed && binding.etFixedDuration.text.isNullOrBlank()) {
                    binding.etFixedDuration.setText(
                        viewModel.defaultFixedMinutes.value.toString()
                    )
                }
                updatePricePreview()
            }
        }

        binding.switchMultiPlayer.setOnCheckedChangeListener { _, _ ->
            updatePricePreview()
        }

        binding.etFixedDuration.setOnFocusChangeListener { _, _ ->
            updatePricePreview()
        }
    }

    private fun observePricing() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.pricingSettings,
                    viewModel.currency,
                    viewModel.defaultFixedMinutes,
                    viewModel.defaultSessionMode
                ) { pricing, currency, defaultMinutes, defaultMode ->
                    PricingState(pricing, currency, defaultMinutes, defaultMode)
                }.collect { state ->
                    applyDefaults(state.defaultMode, state.defaultMinutes)
                    updatePricePreview(state.pricing, state.currency)
                }
            }
        }
    }

    private fun applyDefaults(defaultMode: String, defaultMinutes: Int) {
        isModeUpdating = true
        if (defaultMode == AppConstants.SESSION_MODE_FIXED) {
            binding.toggleSessionMode.check(R.id.btnModeFixed)
            binding.tilFixedDuration.isVisible = true
            if (binding.etFixedDuration.text.isNullOrBlank()) {
                binding.etFixedDuration.setText(defaultMinutes.toString())
            }
        } else {
            binding.toggleSessionMode.check(R.id.btnModeOpen)
            binding.tilFixedDuration.isVisible = false
        }
        isModeUpdating = false

        isConsoleUpdating = true
        binding.toggleConsoleType.check(R.id.btnPs4)
        isConsoleUpdating = false
    }

    private fun updatePricePreview(
        pricing: SessionPricing.PricingSettings = viewModel.pricingSettings.value,
        currency: String = viewModel.currency.value
    ) {
        val sessionMode = getSelectedSessionMode()
        val deviceType = getSelectedDeviceType()
        val isMultiPlayer = binding.switchMultiPlayer.isChecked
        val fixedMinutes = binding.etFixedDuration.text?.toString()?.toIntOrNull()

        val previewText = if (sessionMode == AppConstants.SESSION_MODE_FIXED) {
            val minutes = fixedMinutes ?: viewModel.defaultFixedMinutes.value
            val amount = SessionPricing.previewAmount(
                sessionMode, pricing, deviceType, isMultiPlayer, minutes
            )
            getString(R.string.price_preview_fixed, CurrencyUtils.formatAmount(amount, currency))
        } else {
            val amount = SessionPricing.previewAmount(
                sessionMode, pricing, deviceType, isMultiPlayer, null
            )
            getString(
                R.string.price_preview_hourly,
                CurrencyUtils.formatAmount(amount, currency)
            )
        }
        binding.tvPricePreview.text = previewText
    }

    private fun getSelectedDeviceType(): String {
        return if (binding.toggleConsoleType.checkedButtonId == R.id.btnPs5) {
            AppConstants.DEVICE_PS5
        } else {
            AppConstants.DEVICE_PS4
        }
    }

    private fun getSelectedSessionMode(): String {
        return if (binding.toggleSessionMode.checkedButtonId == R.id.btnModeFixed) {
            AppConstants.SESSION_MODE_FIXED
        } else {
            AppConstants.SESSION_MODE_OPEN
        }
    }

    private suspend fun startSession(): Boolean {
        val deviceNumber = binding.spinnerDeviceNumber.selectedItem as Int
        val deviceType = getSelectedDeviceType()
        val sessionMode = getSelectedSessionMode()
        val isMultiPlayer = binding.switchMultiPlayer.isChecked

        var fixedDurationMinutes: Int? = null
        if (sessionMode == AppConstants.SESSION_MODE_FIXED) {
            fixedDurationMinutes = binding.etFixedDuration.text?.toString()?.toIntOrNull()
            if (fixedDurationMinutes == null || fixedDurationMinutes <= 0) {
                binding.tilFixedDuration.error = getString(R.string.invalid_duration)
                return false
            }
            binding.tilFixedDuration.error = null
        }

        viewModel.startSession(
            deviceType = deviceType,
            deviceNumber = deviceNumber,
            sessionMode = sessionMode,
            isMultiPlayer = isMultiPlayer,
            fixedDurationMinutes = fixedDurationMinutes
        )
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class PricingState(
        val pricing: SessionPricing.PricingSettings,
        val currency: String,
        val defaultMinutes: Int,
        val defaultMode: String
    )
}
