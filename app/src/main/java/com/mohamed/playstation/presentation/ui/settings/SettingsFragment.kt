package com.mohamed.playstation.presentation.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.BuildConfig
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.databinding.FragmentSettingsBinding
import com.mohamed.playstation.domain.model.CurrencyList
import com.mohamed.playstation.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private var isUpdatingUi = false
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private var restoreDialog: androidx.appcompat.app.AlertDialog? = null
    private var savedIndicatorJob: Job? = null

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.exportBackup(it)
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showRestoreConfirmation(it)
        }
    }

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

        setupAboutSection()
        setupListeners()
        observeData()
    }

    private fun setupAboutSection() {
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        binding.tvDeveloper.text = getString(R.string.settings_developer, AppConstants.DEVELOPER_NAME)
    }

    private fun setupListeners() {
        // Dark Mode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.darkMode.value != isChecked) {
                viewModel.setDarkMode(isChecked)
            }
        }

        // Notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.notificationsEnabled.value != isChecked) {
                viewModel.setNotificationsEnabled(isChecked)
            }
        }

        // Currency
        val currencies = CurrencyList.currencies
        val displayNames = currencies.map { "${getString(it.displayNameRes)} (${it.code})" }.toTypedArray()
        binding.actvCurrency.setSimpleItems(displayNames)
        
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            if (!isUpdatingUi) {
                viewModel.setCurrency(currencies[position].code)
            }
        }

        // Language
        val languageItems = viewModel.languageList
        val languageNames = languageItems.map { getString(it.nameResId) }.toTypedArray()
        binding.actvLanguage.setSimpleItems(languageNames)
        
        binding.actvLanguage.setOnItemClickListener { _, _, position, _ ->
            if (!isUpdatingUi) {
                val selectedCode = languageItems[position].code
                viewModel.setLanguage(selectedCode)
            }
        }

        // Pricing Inputs with Validation
        setupPriceInput(binding.etPs4HourPrice) { viewModel.setPs4HourPrice(it) }
        setupPriceInput(binding.etPs4MultiExtra) { viewModel.setPs4MultiExtra(it) }
        setupPriceInput(binding.etPs5HourPrice) { viewModel.setPs5HourPrice(it) }
        setupPriceInput(binding.etPs5MultiExtra) { viewModel.setPs5MultiExtra(it) }

        // Reminder Minutes with Validation
        binding.etReminderMinutes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUi) {
                    hideSavedIndicator()
                    val value = AppFormatters.parseInteger(s)
                    viewModel.setReminderMinutes(value ?: 0)
                }
            }
        })

        // Backup & Restore
        binding.btnBackup.setOnClickListener {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.ENGLISH)
            val fileName = "playstation_backup_${sdf.format(java.util.Date())}.json"
            exportLauncher.launch(fileName)
        }
        binding.btnRestore.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
        }

    }

    private fun setupPriceInput(editText: com.google.android.material.textfield.TextInputEditText, onSave: (Double?) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUi) {
                    hideSavedIndicator()
                    val value = AppFormatters.parseDecimal(s)
                    onSave(value)
                }
            }
        })
    }

    private fun showLoadingDialog(messageRes: Int) {
        if (progressDialog == null) {
            val view = layoutInflater.inflate(R.layout.dialog_loading, null)
            val tvMessage = view.findViewById<android.widget.TextView>(R.id.tvLoadingMessage)
            tvMessage.setText(messageRes)
            
            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create()
        }
        progressDialog?.show()
    }

    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
        progressDialog = null
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
                        val currency = CurrencyList.getCurrencyByCode(code)
                        val displayName = "${getString(currency.displayNameRes)} (${currency.code})"
                        if (binding.actvCurrency.text.toString() != displayName) {
                            isUpdatingUi = true
                            binding.actvCurrency.setText(displayName, false)
                            isUpdatingUi = false
                        }
                    }
                }
                // Language
                launch {
                    viewModel.language.collect { code ->
                        val languageItem = viewModel.languageList.find { it.code == code }
                            ?: viewModel.languageList.first()
                        val displayName = getString(languageItem.nameResId)
                        if (binding.actvLanguage.text.toString() != displayName) {
                            isUpdatingUi = true
                            binding.actvLanguage.setText(displayName, false)
                            isUpdatingUi = false
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
                    viewModel.ps4MultiExtra.collect { price ->
                        updatePriceEditText(binding.etPs4MultiExtra, price)
                    }
                }
                launch {
                    viewModel.ps5HourPrice.collect { price ->
                        updatePriceEditText(binding.etPs5HourPrice, price)
                    }
                }
                launch {
                    viewModel.ps5MultiExtra.collect { price ->
                        updatePriceEditText(binding.etPs5MultiExtra, price)
                    }
                }
                // Reminder
                launch {
                    viewModel.reminderMinutes.collect { minutes ->
                        val formattedMinutes = AppFormatters.formatInteger(requireContext(), minutes)
                        if (binding.etReminderMinutes.text.toString() != formattedMinutes && !binding.etReminderMinutes.hasFocus()) {
                            isUpdatingUi = true
                            binding.etReminderMinutes.setText(formattedMinutes)
                            isUpdatingUi = false
                        }
                    }
                }
                // Validation Errors
                launch {
                    viewModel.validationErrors.collect { errors ->
                        binding.tilPs4HourPrice.error = errors.ps4HourError?.let { getString(it) }
                        binding.tilPs4MultiExtra.error = errors.ps4MultiError?.let { getString(it) }
                        binding.tilPs5HourPrice.error = errors.ps5HourError?.let { getString(it) }
                        binding.tilPs5MultiExtra.error = errors.ps5MultiError?.let { getString(it) }
                        binding.tilReminderMinutes.error = errors.reminderError?.let { getString(it) }
                    }
                }
                // Editable settings acknowledgement
                launch {
                    viewModel.editableSettingSaved.collect { setting ->
                        showSavedIndicator(setting)
                    }
                }
                // Backup State
                launch {
                    viewModel.backupUiState.collect { state ->
                        when (state) {
                            is BackupUiState.Idle -> { dismissLoadingDialog() }
                            is BackupUiState.Loading -> {
                                showLoadingDialog(R.string.backup_loading)
                            }
                            is BackupUiState.Success -> {
                                dismissLoadingDialog()
                                Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_LONG).show()
                                viewModel.resetBackupUiState()
                            }
                            is BackupUiState.RestoreSuccess -> {
                                dismissLoadingDialog()
                                Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_LONG).show()
                                viewModel.resetBackupUiState()
                                requireActivity().recreate()
                            }
                            is BackupUiState.Error -> {
                                dismissLoadingDialog()
                                Toast.makeText(requireContext(), state.message.asString(requireContext()), Toast.LENGTH_LONG).show()
                                viewModel.resetBackupUiState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateSwitchState(switch: com.google.android.material.materialswitch.MaterialSwitch, state: Boolean) {
        if (switch.isChecked != state) {
            switch.isChecked = state
        }
    }

    private fun updatePriceEditText(editText: com.google.android.material.textfield.TextInputEditText, price: Double) {
        val priceStr = AppFormatters.formatEditableAmount(requireContext(), price)
        if (editText.text.toString() != priceStr && !editText.hasFocus()) {
            isUpdatingUi = true
            editText.setText(priceStr)
            isUpdatingUi = false
        }
    }

    private fun showSavedIndicator(setting: SettingsViewModel.EditableSetting) {
        hideSavedIndicator()
        savedIndicatorFor(setting).isVisible = true
        savedIndicatorJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SAVED_INDICATOR_DURATION_MS)
            savedIndicatorFor(setting).isVisible = false
            savedIndicatorJob = null
        }
    }

    private fun hideSavedIndicator() {
        savedIndicatorJob?.cancel()
        savedIndicatorJob = null
        binding.tvPs4HourPriceSaved.isVisible = false
        binding.tvPs4MultiExtraSaved.isVisible = false
        binding.tvPs5HourPriceSaved.isVisible = false
        binding.tvPs5MultiExtraSaved.isVisible = false
        binding.tvReminderMinutesSaved.isVisible = false
    }

    private fun savedIndicatorFor(setting: SettingsViewModel.EditableSetting): TextView = when (setting) {
        SettingsViewModel.EditableSetting.PS4_HOUR_PRICE -> binding.tvPs4HourPriceSaved
        SettingsViewModel.EditableSetting.PS4_MULTI_EXTRA -> binding.tvPs4MultiExtraSaved
        SettingsViewModel.EditableSetting.PS5_HOUR_PRICE -> binding.tvPs5HourPriceSaved
        SettingsViewModel.EditableSetting.PS5_MULTI_EXTRA -> binding.tvPs5MultiExtraSaved
        SettingsViewModel.EditableSetting.REMINDER_MINUTES -> binding.tvReminderMinutesSaved
    }

    private fun showRestoreConfirmation(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.hasActiveSessions()) {
                restoreDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.active_sessions_warning)
                    .setMessage(R.string.restore_confirmation)
                    .setPositiveButton(R.string.restore_backup_button) { _, _ ->
                        executeRestore(uri)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                restoreDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.restore)
                    .setMessage(R.string.restore_confirmation)
                    .setPositiveButton(R.string.restore_backup_button) { _, _ ->
                        executeRestore(uri)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun executeRestore(uri: android.net.Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        viewModel.importBackup(uri, inputStream)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        savedIndicatorJob?.cancel()
        savedIndicatorJob = null
        dismissLoadingDialog()
        restoreDialog?.dismiss()
        restoreDialog = null
        _binding = null
    }

    private companion object {
        const val SAVED_INDICATOR_DURATION_MS = 1_500L
    }
}
