package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.pdf.ReceiptPdfGenerator
import com.mohamed.playstation.core.pdf.mapper.ReceiptPdfMapper
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.domain.usecase.ReceiptUseCases
import com.mohamed.playstation.domain.usecase.SessionProductUseCases
import com.mohamed.playstation.presentation.ui.UiState
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel
import com.mohamed.playstation.presentation.ui.receipts.state.PdfUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ViewModel للفواتير
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val receiptUseCases: ReceiptUseCases,
    private val sessionProductUseCases: SessionProductUseCases,
    private val settingsManager: SettingsManager,
    private val pdfGenerator: ReceiptPdfGenerator
) : ViewModel() {

    // العملة من الإعدادات — reactive من DataStore
    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    private val _dateFilterFlow = MutableStateFlow(DateRangeFilter.TODAY)
    val dateFilterFlow: StateFlow<DateRangeFilter> = _dateFilterFlow.asStateFlow()

    val productSummaries: StateFlow<Map<Long, SessionProductSummary>> = sessionProductUseCases
        .getAllSessionProductSummaries()
        .map { summaries -> summaries.associateBy { it.sessionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _customStart = MutableStateFlow<Long>(0L)
    private val _customEnd = MutableStateFlow<Long>(0L)

    private val filterTrigger = combine(
        dateFilterFlow,
        _customStart,
        _customEnd
    ) { filter, start, end ->
        Triple(filter, start, end)
    }

    val receipts: StateFlow<UiState<List<Receipt>>> = filterTrigger.flatMapLatest { (filter, customStart, customEnd) ->
        val (start, end) = getRangeForFilter(filter, customStart, customEnd)
        receiptUseCases.getReceiptsInRange(start, end)
            .map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { e ->
                Timber.e(e, "Error loading receipts")
                emit(UiState.Error(e.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.error_occurred)))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val periodRevenue: StateFlow<Double> = filterTrigger.flatMapLatest { (filter, customStart, customEnd) ->
        val (start, end) = getRangeForFilter(filter, customStart, customEnd)
        receiptUseCases.getTotalRevenueInRange(start, end)
            .catch { emit(0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // PDF UI State
    private val _pdfUiState = MutableStateFlow<PdfUiState>(PdfUiState.Idle)
    val pdfUiState: StateFlow<PdfUiState> = _pdfUiState.asStateFlow()

    fun setDateFilter(filter: DateRangeFilter) {
        _dateFilterFlow.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStart.value = start
        _customEnd.value = end
        setDateFilter(DateRangeFilter.CUSTOM)
    }

    private fun getRangeForFilter(
        range: DateRangeFilter,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        return when (range) {
            DateRangeFilter.TODAY -> DateUtils.todayRange()
            DateRangeFilter.THIS_WEEK -> DateUtils.thisWeekRange()
            DateRangeFilter.LAST_7_DAYS -> DateUtils.last7DaysRange()
            DateRangeFilter.THIS_MONTH -> DateUtils.thisMonthRange()
            DateRangeFilter.LAST_MONTH -> DateUtils.lastMonthRange()
            DateRangeFilter.LAST_30_DAYS -> DateUtils.last30DaysRange()
            DateRangeFilter.LAST_3_MONTHS -> DateUtils.last3MonthsRange()
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.CUSTOM -> {
                val endCal = java.util.Calendar.getInstance()
                endCal.timeInMillis = customEnd
                if (endCal.get(java.util.Calendar.HOUR_OF_DAY) == 0 && endCal.get(java.util.Calendar.MINUTE) == 0) {
                    endCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    endCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    endCal.set(java.util.Calendar.MINUTE, 0)
                    endCal.set(java.util.Calendar.SECOND, 0)
                    endCal.set(java.util.Calendar.MILLISECOND, 0)
                }
                Pair(customStart, endCal.timeInMillis)
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
            } catch (e: Exception) {
                Timber.e(e, "Error deleting receipt")
            }
        }
    }

    /**
     * Generates a PDF for the given receipt UI model.
     */
    fun generateReceiptPdf(uiModel: ReceiptUiModel, appName: String, footerMessage: String) {
        if (_pdfUiState.value is PdfUiState.Loading) return
        
        viewModelScope.launch {
            _pdfUiState.value = PdfUiState.Loading
            try {
                val uri = withContext(Dispatchers.IO) {
                    val pdfModel = ReceiptPdfMapper.mapToPdfModel(uiModel, appName, footerMessage)
                    pdfGenerator.generate(pdfModel)
                }
                
                if (uri != null) {
                    _pdfUiState.value = PdfUiState.Success(uri)
                } else {
                    _pdfUiState.value = PdfUiState.Error(UiText.StringResource(R.string.error_generating_pdf))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error generating PDF")
                _pdfUiState.value = PdfUiState.Error(UiText.DynamicString(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Resets the PDF UI state to Idle.
     */
    fun resetPdfState() {
        _pdfUiState.value = PdfUiState.Idle
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
}
