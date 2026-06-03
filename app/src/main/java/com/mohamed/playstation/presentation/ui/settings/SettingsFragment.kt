package com.mohamed.playstation.presentation.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.databinding.FragmentSettingsBinding
import com.mohamed.playstation.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fragment للإعدادات — Phase 1
 * المظهر · أسعار PS4 · أسعار PS5 · إعدادات الجلسة · التنبيهات · العملة
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    // Focus flags — prevent overwriting user input while editing
    private var isPs4HourFocused = false
    private var isPs4HalfHourFocused = false
    private var isPs4MultiExtraFocused = false
    private var isPs5HourFocused = false
    private var isPs5HalfHourFocused = false
    private var isPs5MultiExtraFocused = false
    private var isFixedMinutesFocused = false
    private var isWarningMinutesFocused = false

    // Programmatic-update guards — prevent listener re-entry
    private var isDarkModeUpdatingProgrammatically = false
    private var isWarningsEnabledUpdating = false
    private var isWarningSoundUpdating = false
    private var isWarningNotificationUpdating = false
    private var isSessionModeUpdating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrencyDropdown()
        setupListeners()
        observeData()
    }

    // ──────────────────────── Setup ────────────────────────

    private fun setupCurrencyDropdown() {
        val displayNames = viewModel.currencyList.map { item ->
            val resId = resources.getIdentifier(item.displayResName, "string", requireContext().packageName)
            if (resId != 0) getString(resId) else item.code
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            displayNames
        )
        binding.actvCurrency.setAdapter(adapter)
    }

    private fun setupListeners() {
        // ── Dark Mode ──
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isDarkModeUpdatingProgrammatically) return@setOnCheckedChangeListener
            viewModel.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            Timber.d("Dark mode toggled: $isChecked")
        }

        // ── PS4 Pricing ──
        binding.etPs4HourPrice.setOnFocusChangeListener { _, hasFocus ->
            isPs4HourFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs4HourPrice.text.toString()) { viewModel.setPs4HourPrice(it) }
        }
        binding.etPs4HalfHourPrice.setOnFocusChangeListener { _, hasFocus ->
            isPs4HalfHourFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs4HalfHourPrice.text.toString()) { viewModel.setPs4HalfHourPrice(it) }
        }
        binding.etPs4MultiExtra.setOnFocusChangeListener { _, hasFocus ->
            isPs4MultiExtraFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs4MultiExtra.text.toString()) { viewModel.setPs4MultiExtra(it) }
        }

        // ── PS5 Pricing ──
        binding.etPs5HourPrice.setOnFocusChangeListener { _, hasFocus ->
            isPs5HourFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs5HourPrice.text.toString()) { viewModel.setPs5HourPrice(it) }
        }
        binding.etPs5HalfHourPrice.setOnFocusChangeListener { _, hasFocus ->
            isPs5HalfHourFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs5HalfHourPrice.text.toString()) { viewModel.setPs5HalfHourPrice(it) }
        }
        binding.etPs5MultiExtra.setOnFocusChangeListener { _, hasFocus ->
            isPs5MultiExtraFocused = hasFocus
            if (!hasFocus) savePrice(binding.etPs5MultiExtra.text.toString()) { viewModel.setPs5MultiExtra(it) }
        }

        // ── Session Mode (ToggleGroup) ──
        binding.toggleSessionMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isSessionModeUpdating || !isChecked) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btnModeOpen) AppConstants.SESSION_MODE_OPEN
                       else AppConstants.SESSION_MODE_FIXED
            viewModel.setSessionMode(mode)
            updateFixedMinutesVisibility(mode)
            Timber.d("Session mode: $mode")
        }

        // ── Fixed Minutes ──
        binding.etFixedMinutes.setOnFocusChangeListener { _, hasFocus ->
            isFixedMinutesFocused = hasFocus
            if (!hasFocus) {
                val value = binding.etFixedMinutes.text.toString().toIntOrNull()
                if (value != null && value > 0) {
                    viewModel.setDefaultFixedMinutes(value)
                    Timber.d("Fixed minutes saved: $value")
                }
            }
        }

        // ── Warning Switches ──
        binding.switchWarningsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isWarningsEnabledUpdating) return@setOnCheckedChangeListener
            viewModel.setWarningsEnabled(isChecked)
            updateWarningChildrenEnabled(isChecked)
            Timber.d("Warnings enabled: $isChecked")
        }
        binding.switchWarningSound.setOnCheckedChangeListener { _, isChecked ->
            if (isWarningSoundUpdating) return@setOnCheckedChangeListener
            viewModel.setWarningSoundEnabled(isChecked)
            Timber.d("Warning sound: $isChecked")
        }
        binding.switchWarningNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isWarningNotificationUpdating) return@setOnCheckedChangeListener
            viewModel.setWarningNotificationEnabled(isChecked)
            Timber.d("Warning notification: $isChecked")
        }

        // ── Warning Minutes ──
        binding.etWarningMinutes.setOnFocusChangeListener { _, hasFocus ->
            isWarningMinutesFocused = hasFocus
            if (!hasFocus) {
                val value = binding.etWarningMinutes.text.toString().toIntOrNull()
                if (value != null && value > 0) {
                    viewModel.setWarningMinutes(value)
                    Timber.d("Warning minutes saved: $value")
                }
            }
        }

        // ── Currency ──
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = viewModel.currencyList[position].code
            viewModel.setCurrency(selectedCode)
            Timber.d("Currency selected: $selectedCode")
        }
    }

    // ──────────────────────── Observe ────────────────────────

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Dark Mode
                launch {
                    viewModel.darkMode.collect { enabled ->
                        if (binding.switchDarkMode.isChecked != enabled) {
                            isDarkModeUpdatingProgrammatically = true
                            binding.switchDarkMode.isChecked = enabled
                            isDarkModeUpdatingProgrammatically = false
                        }
                    }
                }

                // ── PS4 Pricing ──
                launch {
                    viewModel.ps4HourPrice.collect { price ->
                        if (!isPs4HourFocused) setTextIfChanged(binding.etPs4HourPrice, formatPrice(price))
                    }
                }
                launch {
                    viewModel.ps4HalfHourPrice.collect { price ->
                        if (!isPs4HalfHourFocused) setTextIfChanged(binding.etPs4HalfHourPrice, formatPrice(price))
                    }
                }
                launch {
                    viewModel.ps4MultiExtra.collect { price ->
                        if (!isPs4MultiExtraFocused) setTextIfChanged(binding.etPs4MultiExtra, formatPrice(price))
                    }
                }

                // ── PS5 Pricing ──
                launch {
                    viewModel.ps5HourPrice.collect { price ->
                        if (!isPs5HourFocused) setTextIfChanged(binding.etPs5HourPrice, formatPrice(price))
                    }
                }
                launch {
                    viewModel.ps5HalfHourPrice.collect { price ->
                        if (!isPs5HalfHourFocused) setTextIfChanged(binding.etPs5HalfHourPrice, formatPrice(price))
                    }
                }
                launch {
                    viewModel.ps5MultiExtra.collect { price ->
                        if (!isPs5MultiExtraFocused) setTextIfChanged(binding.etPs5MultiExtra, formatPrice(price))
                    }
                }

                // ── Session Mode ──
                launch {
                    viewModel.sessionMode.collect { mode ->
                        isSessionModeUpdating = true
                        val buttonId = if (mode == AppConstants.SESSION_MODE_OPEN) R.id.btnModeOpen
                                       else R.id.btnModeFixed
                        binding.toggleSessionMode.check(buttonId)
                        updateFixedMinutesVisibility(mode)
                        isSessionModeUpdating = false
                    }
                }

                // ── Fixed Minutes ──
                launch {
                    viewModel.defaultFixedMinutes.collect { minutes ->
                        if (!isFixedMinutesFocused) setTextIfChanged(binding.etFixedMinutes, minutes.toString())
                    }
                }

                // ── Warning Settings ──
                launch {
                    viewModel.warningsEnabled.collect { enabled ->
                        if (binding.switchWarningsEnabled.isChecked != enabled) {
                            isWarningsEnabledUpdating = true
                            binding.switchWarningsEnabled.isChecked = enabled
                            isWarningsEnabledUpdating = false
                        }
                        updateWarningChildrenEnabled(enabled)
                    }
                }
                launch {
                    viewModel.warningSoundEnabled.collect { enabled ->
                        if (binding.switchWarningSound.isChecked != enabled) {
                            isWarningSoundUpdating = true
                            binding.switchWarningSound.isChecked = enabled
                            isWarningSoundUpdating = false
                        }
                    }
                }
                launch {
                    viewModel.warningNotificationEnabled.collect { enabled ->
                        if (binding.switchWarningNotification.isChecked != enabled) {
                            isWarningNotificationUpdating = true
                            binding.switchWarningNotification.isChecked = enabled
                            isWarningNotificationUpdating = false
                        }
                    }
                }
                launch {
                    viewModel.warningMinutes.collect { minutes ->
                        if (!isWarningMinutesFocused) setTextIfChanged(binding.etWarningMinutes, minutes.toString())
                    }
                }

                // ── Currency ──
                launch {
                    viewModel.currency.collect { code ->
                        val index = viewModel.currencyList.indexOfFirst { it.code == code }
                        if (index != -1) {
                            val resId = resources.getIdentifier(
                                viewModel.currencyList[index].displayResName,
                                "string",
                                requireContext().packageName
                            )
                            val displayName = if (resId != 0) getString(resId) else code
                            if (binding.actvCurrency.text.toString() != displayName) {
                                binding.actvCurrency.setText(displayName, false)
                            }
                        }
                    }
                }
            }
        }
    }

    // ──────────────────────── Helpers ────────────────────────

    /**
     * Generic price saver — validates ≥ 0 before calling the provided setter.
     */
    private fun savePrice(text: String, setter: (Double) -> Unit) {
        val value = text.toDoubleOrNull()
        if (value != null && value >= 0) {
            setter(value)
        }
    }

    /**
     * Sets text on an EditText only if the current text differs — prevents infinite loops.
     */
    private fun setTextIfChanged(editText: com.google.android.material.textfield.TextInputEditText, text: String) {
        if (editText.text.toString() != text) {
            editText.setText(text)
        }
    }

    /**
     * Show/hide the fixed-minutes field based on session mode.
     */
    private fun updateFixedMinutesVisibility(mode: String) {
        binding.tilFixedMinutes.visibility =
            if (mode == AppConstants.SESSION_MODE_FIXED) View.VISIBLE else View.GONE
    }

    /**
     * Enable/disable warning sub-switches and minutes field based on master toggle.
     */
    private fun updateWarningChildrenEnabled(masterEnabled: Boolean) {
        binding.switchWarningSound.isEnabled = masterEnabled
        binding.switchWarningNotification.isEnabled = masterEnabled
        binding.etWarningMinutes.isEnabled = masterEnabled
        binding.tilWarningMinutes.isEnabled = masterEnabled
    }

    /**
     * Format price — display without decimal if it's a whole number.
     */
    private fun formatPrice(price: Double): String {
        return if (price == price.toLong().toDouble()) {
            price.toLong().toString()
        } else {
            price.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
