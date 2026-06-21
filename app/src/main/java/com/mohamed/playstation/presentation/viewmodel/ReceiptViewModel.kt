package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.domain.usecase.ReceiptUseCases
import com.mohamed.playstation.domain.usecase.SessionProductUseCases
import com.mohamed.playstation.presentation.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel للفواتير
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val receiptUseCases: ReceiptUseCases,
    private val sessionProductUseCases: SessionProductUseCases,
    private val settingsManager: SettingsManager
) : ViewModel() {

    // كل الفواتير
    private val _allReceipts = MutableStateFlow<UiState<List<Receipt>>>(UiState.Loading)
    val allReceipts: StateFlow<UiState<List<Receipt>>> = _allReceipts.asStateFlow()

    // فواتير اليوم
    private val _todayReceipts = MutableStateFlow<UiState<List<Receipt>>>(UiState.Loading)
    val todayReceipts: StateFlow<UiState<List<Receipt>>> = _todayReceipts.asStateFlow()

    // إجمالي الإيرادات اليوم
    private val _todayRevenue = MutableStateFlow(0.0)
    val todayRevenue: StateFlow<Double> = _todayRevenue.asStateFlow()

    val productSummaries: StateFlow<Map<Long, SessionProductSummary>> = sessionProductUseCases
        .getAllSessionProductSummaries()
        .map { summaries -> summaries.associateBy { it.sessionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // العملة من الإعدادات — reactive من DataStore
    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    init {
        loadAllReceipts()
        loadTodayReceipts()
        loadTodayRevenue()
    }

    /**
     * تحميل كل الفواتير
     */
    private fun loadAllReceipts() {
        viewModelScope.launch {
            receiptUseCases.getAllReceipts()
                .catch { e ->
                    Timber.e(e, "Error loading all receipts")
                    _allReceipts.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { receipts ->
                    _allReceipts.value = if (receipts.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(receipts)
                    }
                }
        }
    }

    /**
     * تحميل فواتير اليوم
     */
    private fun loadTodayReceipts() {
        viewModelScope.launch {
            receiptUseCases.getTodayReceipts()
                .catch { e ->
                    Timber.e(e, "Error loading today receipts")
                    _todayReceipts.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { receipts ->
                    _todayReceipts.value = if (receipts.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(receipts)
                    }
                }
        }
    }

    /**
     * تحميل إجمالي الإيرادات اليوم
     */
    private fun loadTodayRevenue() {
        viewModelScope.launch {
            receiptUseCases.getTodayTotalRevenue()
                .catch { e ->
                    Timber.e(e, "Error loading today revenue")
                }
                .collect { revenue ->
                    _todayRevenue.value = revenue
                }
        }
    }

    /**
     * تحديث طريقة الدفع
     */
    fun updatePaymentMethod(receipt: Receipt, paymentMethod: String) {
        viewModelScope.launch {
            try {
                receiptUseCases.updatePaymentMethod(receipt, paymentMethod)
                Timber.d("Payment method updated for receipt: ${receipt.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error updating payment method")
            }
        }
    }

    /**
     * حذف فاتورة
     */
    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                receiptUseCases.deleteReceipt(receipt)
                Timber.d("Receipt deleted: ${receipt.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting receipt")
            }
        }
    }

    // ---------------------------
    // الوظائف المضافة لحل المشكلة
    // ---------------------------

    /**
     * دالة وسيطة تُعرّض getReceiptById من الـ usecases عبر الـ ViewModel.
     * تُستخدم كمثال داخل lifecycleScope.launch { viewModel.getReceiptById(id) }
     */
    suspend fun getReceiptById(receiptId: Long): Receipt? {
        return try {
            receiptUseCases.getReceiptById(receiptId)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching receipt by id: $receiptId")
            null
        }
    }

    suspend fun getProductsBySessionId(sessionId: Long): List<SessionProduct> {
        return try {
            sessionProductUseCases.getProductsBySessionIdOnce(sessionId)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching products for session id: $sessionId")
            emptyList()
        }
    }

    /**
     * بديل غير-suspend لو أردت استخدام LiveData/Flow لاحقاً.
     * يمكنك إضافته حسب حاجة ال-UI.
     */
    // fun getReceiptFlowById(receiptId: Long): Flow<Receipt?> =
    //     flow { emit(receiptUseCases.getReceiptById(receiptId)) }.catch { emit(null) }
}
