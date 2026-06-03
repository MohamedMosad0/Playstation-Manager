package com.mohamed.playstation.presentation.ui.receipts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.DialogReceiptDetailBinding
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.presentation.viewmodel.ReceiptViewModel
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ReceiptDetailDialog : DialogFragment() {

    private var _binding: DialogReceiptDetailBinding? = null
    private val binding get() = _binding!!

    private val receiptViewModel: ReceiptViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()

    private var receipt: Receipt? = null
    private var session: Session? = null
    private var products: List<SessionProduct> = emptyList()
    private var receiptId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PlayStation_Dialog_FullScreen)
        receiptId = arguments?.getLong(ARG_RECEIPT_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReceiptDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (receiptId == 0L) {
            dismiss()
            return
        }

        lifecycleScope.launch {
            val loadedReceipt = receiptViewModel.getReceiptById(receiptId)
            if (!isAdded || loadedReceipt == null) {
                dismiss()
                return@launch
            }

            val loadedSession = sessionViewModel.sessionUseCases.getSessionById(loadedReceipt.sessionId)
            val loadedProducts = receiptViewModel.getProductsBySessionId(loadedReceipt.sessionId)

            receipt = loadedReceipt
            session = loadedSession
            products = loadedProducts

            setupUI(loadedReceipt, loadedSession, loadedProducts)
        }
    }

    private fun setupUI(
        currentReceipt: Receipt,
        relatedSession: Session?,
        currentProducts: List<SessionProduct>
    ) {
        with(binding) {
            tvReceiptNumber.text = getString(R.string.receipt_number, currentReceipt.receiptNumber)
            tvDevice.text = "${currentReceipt.deviceType} #${currentReceipt.deviceNumber}"

            tvSessionType.text = when (currentReceipt.sessionType) {
                "single" -> getString(R.string.single_player)
                "multi" -> getString(R.string.multiplayer)
                else -> currentReceipt.sessionType
            }

            tvStarted.text = currentReceipt.getFormattedStartTime()
            tvEnded.text = currentReceipt.getFormattedEndTime()
            tvDuration.text = currentReceipt.getFormattedDuration()

            val pausedMinutes = relatedSession?.totalPausedMinutes ?: 0L
            val hasPausedDuration = pausedMinutes > 0
            labelPausedDuration.isVisible = hasPausedDuration
            tvPausedDuration.isVisible = hasPausedDuration
            if (hasPausedDuration) {
                tvPausedDuration.text = formatPausedDuration(pausedMinutes)
            }

            val currencySymbol = CurrencyUtils.getCurrencySymbol(currentReceipt.currencyCode)
            tvRate.text = getString(
                R.string.receipt_rate_per_hour,
                String.format(Locale.getDefault(), "%.2f", currentReceipt.pricePerHour),
                currencySymbol
            )
            val playAmount = (currentReceipt.durationMinutes / 60.0) * currentReceipt.pricePerHour
            val productsAmount = currentProducts.sumOf { it.getLineTotal() }
            tvPlayCost.text = CurrencyUtils.formatAmount(playAmount, currentReceipt.currencyCode)
            tvProductsCost.text = CurrencyUtils.formatAmount(productsAmount, currentReceipt.currencyCode)
            tvTotal.text = "${String.format(Locale.getDefault(), "%.2f", currentReceipt.totalAmount)} $currencySymbol"
            tvProductsList.text = currentProducts.joinToString("\n") { product ->
                "${product.name} x${product.quantity} = ${
                    CurrencyUtils.formatAmount(product.getLineTotal(), currentReceipt.currencyCode)
                }"
            }
            tvProductsList.isVisible = currentProducts.isNotEmpty()
            tvNoProducts.isVisible = currentProducts.isEmpty()

            when (currentReceipt.paymentMethod) {
                "cash" -> chipCash.isChecked = true
                "card" -> chipCard.isChecked = true
            }

            btnPrint.setOnClickListener { printReceipt() }
            btnShare.setOnClickListener {
                shareReceipt(this@ReceiptDetailDialog.receipt ?: currentReceipt, relatedSession, currentProducts)
            }
            btnClose.setOnClickListener { dismiss() }

            chipGroupPayment.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val newPaymentMethod = when (checkedIds[0]) {
                        R.id.chipCash -> "cash"
                        R.id.chipCard -> "card"
                        else -> "cash"
                    }

                    if (newPaymentMethod != currentReceipt.paymentMethod) {
                        val updatedReceipt = currentReceipt.copy(paymentMethod = newPaymentMethod)
                        this@ReceiptDetailDialog.receipt = updatedReceipt
                        receiptViewModel.updatePaymentMethod(updatedReceipt, newPaymentMethod)
                    }
                }
            }
        }
    }

    private fun formatPausedDuration(totalPausedMinutes: Long): String {
        val hours = totalPausedMinutes / 60
        val minutes = totalPausedMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun printReceipt() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("طباعة الفاتورة")
            .setMessage("سيتم إضافة ميزة الطباعة قريباً")
            .setPositiveButton("موافق", null)
            .show()
    }

    private fun shareReceipt(
        receipt: Receipt,
        relatedSession: Session?,
        currentProducts: List<SessionProduct>
    ) {
        val shareText = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🎮 ${getString(R.string.app_name)}")
            appendLine(getString(R.string.receipt_number, receipt.receiptNumber))
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("${getString(R.string.device)}: ${receipt.deviceType} #${receipt.deviceNumber}")
            appendLine(
                "النوع: ${
                    if (receipt.sessionType == "single") {
                        getString(R.string.single_player)
                    } else {
                        getString(R.string.multiplayer)
                    }
                }"
            )
            appendLine("${getString(R.string.started)}: ${receipt.getFormattedStartTime()}")
            appendLine("${getString(R.string.ended)}: ${receipt.getFormattedEndTime()}")
            appendLine("${getString(R.string.duration)}: ${receipt.getFormattedDuration()}")
            if ((relatedSession?.totalPausedMinutes ?: 0L) > 0) {
                appendLine(
                    "${getString(R.string.pause_duration)}: ${
                        formatPausedDuration(relatedSession?.totalPausedMinutes ?: 0L)
                    }"
                )
            }
            appendLine()
            val currencySymbol = CurrencyUtils.getCurrencySymbol(receipt.currencyCode)
            appendLine(
                "${getString(R.string.rate)}: ${
                    getString(
                        R.string.receipt_rate_per_hour,
                        String.format(Locale.getDefault(), "%.2f", receipt.pricePerHour),
                        currencySymbol
                    )
                }"
            )
            appendLine(
                "${getString(R.string.play_cost)}: ${
                    CurrencyUtils.formatAmount(
                        (receipt.durationMinutes / 60.0) * receipt.pricePerHour,
                        receipt.currencyCode
                    )
                }"
            )
            val productsAmount = currentProducts.sumOf { it.getLineTotal() }
            appendLine(
                "${getString(R.string.products_cost)}: ${
                    CurrencyUtils.formatAmount(productsAmount, receipt.currencyCode)
                }"
            )
            if (currentProducts.isNotEmpty()) {
                appendLine(getString(R.string.products))
                currentProducts.forEach { product ->
                    appendLine(
                        "- ${product.name} x${product.quantity} = ${
                            CurrencyUtils.formatAmount(product.getLineTotal(), receipt.currencyCode)
                        }"
                    )
                }
            }
            appendLine(
                "${getString(R.string.total)}: ${
                    String.format(Locale.getDefault(), "%.2f", receipt.totalAmount)
                } $currencySymbol"
            )
            appendLine()
            appendLine(
                "طريقة الدفع: ${
                    if (receipt.paymentMethod == "cash") "نقداً" else "كارت"
                }"
            )
            appendLine("━━━━━━━━━━━━━━━━━━━━")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RECEIPT_ID = "receipt_id"

        fun newInstance(receiptId: Long): ReceiptDetailDialog {
            return ReceiptDetailDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_RECEIPT_ID, receiptId)
                }
            }
        }
    }
}
