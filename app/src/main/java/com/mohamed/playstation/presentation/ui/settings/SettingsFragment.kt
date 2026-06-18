package com.mohamed.playstation.presentation.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.BuildConfig
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.databinding.FragmentSettingsBinding
import com.mohamed.playstation.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private var isUpdatingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("SettingsAudit", "SettingsFragment.onCreate: savedInstanceState is ${if (savedInstanceState == null) "null" else "NOT null"}")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d("SettingsAudit", "SettingsFragment.onCreateView")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("SettingsAudit", "SettingsFragment.onViewCreated: savedInstanceState is ${if (savedInstanceState == null) "null" else "NOT null"}")

        view.post {
            logViewBounds("switchDarkMode", binding.switchDarkMode)
            logViewBounds("actvCurrency", binding.actvCurrency)
            logViewBounds("tilCurrencyContainer", binding.actvCurrency.parent.parent as View)
        }

        binding.actvCurrency.setOnFocusChangeListener { v, hasFocus ->
            android.util.Log.d("SettingsAudit", "[CURRENCY] Focus changed: $hasFocus, Popup showing: ${binding.actvCurrency.isPopupShowing}, Text: ${binding.actvCurrency.text}")
        }

        binding.switchDarkMode.setOnTouchListener { v, event ->
            android.util.Log.d("SettingsAudit", "[TOUCH] switchDarkMode: Action=${event.action}, x=${event.x}, y=${event.y}")
            false
        }

        binding.actvCurrency.setOnTouchListener { v, event ->
            android.util.Log.d("SettingsAudit", "[TOUCH] actvCurrency: Action=${event.action}, x=${event.x}, y=${event.y}")
            false
        }

        setupAboutSection()
        setupListeners()
        observeData()
    }

    override fun onResume() {
        android.util.Log.d("SettingsAudit", "SettingsFragment.onResume")
        super.onResume()
    }

    override fun onDestroyView() {
        android.util.Log.d("SettingsAudit", "SettingsFragment.onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        android.util.Log.d("SettingsAudit", "SettingsFragment.onDestroy")
        super.onDestroy()
    }

    private fun logViewBounds(name: String, view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        android.util.Log.d("SettingsAudit", "View $name: x=${location[0]}, y=${location[1]}, w=${view.width}, h=${view.height}")
    }

    private fun setupAboutSection() {
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        binding.tvDeveloper.text = getString(R.string.settings_developer, AppConstants.DEVELOPER_NAME)
    }

    private fun setupListeners() {
        // Dark Mode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                viewModel.setDarkMode(isChecked)
            }
        }

        // Notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                viewModel.setNotificationsEnabled(isChecked)
            }
        }

        // Currency
        val currencies = com.mohamed.playstation.domain.model.CurrencyList.currencies
        val displayNames = currencies.map { "${it.nameAr} (${it.code})" }.toTypedArray()
        binding.actvCurrency.setSimpleItems(displayNames)
        
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            if (!isUpdatingUi) {
                viewModel.setCurrency(currencies[position].code)
            }
        }

        // Pricing Inputs with Validation
        setupPriceInput(binding.etPs4HourPrice) { viewModel.setPs4HourPrice(it) }
        setupPriceInput(binding.etPs4MultiHourPrice) { viewModel.setPs4MultiplayerPrice(it) }
        setupPriceInput(binding.etPs5HourPrice) { viewModel.setPs5HourPrice(it) }
        setupPriceInput(binding.etPs5MultiHourPrice) { viewModel.setPs5MultiplayerPrice(it) }

        // Reminder Minutes with Validation
        binding.etReminderMinutes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUi) {
                    val value = s?.toString()?.toIntOrNull()
                    viewModel.setReminderMinutes(value ?: 0)
                }
            }
        })

        // Data Placeholders
        binding.btnBackup.setOnClickListener {
            Toast.makeText(requireContext(), "Backup feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnRestore.setOnClickListener {
            Toast.makeText(requireContext(), "Restore feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Reset
        binding.btnReset.setOnClickListener {
            showResetConfirmation()
        }
    }

    private fun setupPriceInput(editText: com.google.android.material.textfield.TextInputEditText, onSave: (Double?) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUi) {
                    val value = s?.toString()?.toDoubleOrNull()
                    onSave(value)
                }
            }
        })
    }


    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Dark Mode
                launch {
                    viewModel.darkMode.collect { enabled ->
                        updateSwitchState(binding.switchDarkMode, enabled)
                    }
                }
                // Notifications
                launch {
                    viewModel.notificationsEnabled.collect { enabled ->
                        updateSwitchState(binding.switchNotifications, enabled)
                    }
                }
                // Currency
                launch {
                    viewModel.currency.collect { code ->
                        val currency = com.mohamed.playstation.domain.model.CurrencyList.getCurrencyByCode(code)
                        val displayName = "${currency.nameAr} (${currency.code})"
                        if (binding.actvCurrency.text.toString() != displayName) {
                            android.util.Log.d("SettingsAudit", "[CURRENCY] setText starting: $displayName, Popup showing: ${binding.actvCurrency.isPopupShowing}")
                            isUpdatingUi = true
                            binding.actvCurrency.setText(displayName, false)
                            isUpdatingUi = false
                            android.util.Log.d("SettingsAudit", "[CURRENCY] setText finished. Popup showing: ${binding.actvCurrency.isPopupShowing}")
                        }
                    }
                }
                // Prices
                launch {
                    viewModel.ps4HourPrice.collect { price ->
                        updatePriceEditText(binding.etPs4HourPrice, price)
                    }
                }
                launch {
                    viewModel.ps4MultiplayerPrice.collect { price ->
                        updatePriceEditText(binding.etPs4MultiHourPrice, price)
                    }
                }
                launch {
                    viewModel.ps5HourPrice.collect { price ->
                        updatePriceEditText(binding.etPs5HourPrice, price)
                    }
                }
                launch {
                    viewModel.ps5MultiplayerPrice.collect { price ->
                        updatePriceEditText(binding.etPs5MultiHourPrice, price)
                    }
                }
                // Reminder
                launch {
                    viewModel.reminderMinutes.collect { minutes ->
                        if (binding.etReminderMinutes.text.toString() != minutes.toString() && !binding.etReminderMinutes.hasFocus()) {
                            isUpdatingUi = true
                            binding.etReminderMinutes.setText(minutes.toString())
                            isUpdatingUi = false
                        }
                    }
                }
                // Validation Errors
                launch {
                    viewModel.validationErrors.collect { errors ->
                        binding.tilPs4HourPrice.error = errors.ps4HourError?.let { getString(it) }
                        binding.tilPs4MultiHourPrice.error = errors.ps4MultiError?.let { getString(it) }
                        binding.tilPs5HourPrice.error = errors.ps5HourError?.let { getString(it) }
                        binding.tilPs5MultiHourPrice.error = errors.ps5MultiError?.let { getString(it) }
                        binding.tilReminderMinutes.error = errors.reminderError?.let { getString(it) }
                    }
                }
            }
        }
    }

    private fun updateSwitchState(switch: com.google.android.material.materialswitch.MaterialSwitch, state: Boolean) {
        if (switch.isChecked != state) {
            isUpdatingUi = true
            switch.isChecked = state
            isUpdatingUi = false
        }
    }

    private fun updatePriceEditText(editText: com.google.android.material.textfield.TextInputEditText, price: Double) {
        val priceStr = if (price == price.toLong().toDouble()) price.toLong().toString() else price.toString()
        if (editText.text.toString() != priceStr && !editText.hasFocus()) {
            isUpdatingUi = true
            editText.setText(priceStr)
            isUpdatingUi = false
        }
    }

    private fun showResetConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_reset_confirm_title)
            .setMessage(R.string.settings_reset_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.resetSettings()
                Toast.makeText(requireContext(), R.string.settings_reset_success, Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
