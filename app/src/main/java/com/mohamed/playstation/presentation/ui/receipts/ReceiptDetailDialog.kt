package com.mohamed.playstation.presentation.ui.receipts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.DialogReceiptDetailBinding
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.presentation.ui.receipts.mapper.ReceiptDisplayMapper
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel
import com.mohamed.playstation.presentation.ui.receipts.state.PdfUiState
import com.mohamed.playstation.presentation.viewmodel.ReceiptViewModel
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptDetailDialog : DialogFragment() {

    private var _binding: DialogReceiptDetailBinding? = null
    private val binding get() = _binding!!

    private val receiptViewModel: ReceiptViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()

    private var uiModel: ReceiptUiModel? = null
    private var receipt: Receipt? = null
    private var receiptId: Long = 0L

    private enum class PendingAction { NONE, PRINT, SHARE_PDF }
    private var pendingAction = PendingAction.NONE

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

        viewLifecycleOwner.lifecycleScope.launch {
            val loadedReceipt = receiptViewModel.getReceiptById(receiptId)
            if (!isAdded) return@launch

            if (loadedReceipt == null) {
                dismissAllowingStateLoss()
                return@launch
            }

            val loadedSession = sessionViewModel.getSessionById(loadedReceipt.sessionId)
            val loadedProducts = receiptViewModel.getProductsBySessionId(loadedReceipt.sessionId)

            val model = ReceiptDisplayMapper.map(
                context = requireContext(),
                receipt = loadedReceipt,
                session = loadedSession,
                products = loadedProducts
            )
            uiModel = model
            receipt = loadedReceipt

            bindUI(model, loadedReceipt.paymentMethod)
            observePdfState()
        }
    }

    private fun observePdfState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                receiptViewModel.pdfUiState.collect { state ->
                    when (state) {
                        is PdfUiState.Loading -> {
                            binding.btnPrint.isEnabled = false
                            binding.btnShare.isEnabled = false
                            // Optional: show a loading indicator if available in layout
                        }
                        is PdfUiState.Success -> {
                            binding.btnPrint.isEnabled = true
                            binding.btnShare.isEnabled = true
                            handlePdfSuccess(state.uri)
                            receiptViewModel.resetPdfState()
                        }
                        is PdfUiState.Error -> {
                            binding.btnPrint.isEnabled = true
                            binding.btnShare.isEnabled = true
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.error)
                                .setMessage(state.message.asString(requireContext()))
                                .setPositiveButton(R.string.ok, null)
                                .show()
                            receiptViewModel.resetPdfState()
                            pendingAction = PendingAction.NONE
                        }
                        is PdfUiState.Idle -> {
                            binding.btnPrint.isEnabled = true
                            binding.btnShare.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun handlePdfSuccess(uri: Uri) {
        when (pendingAction) {
            PendingAction.PRINT -> printPdf(uri)
            PendingAction.SHARE_PDF -> sharePdf(uri)
            PendingAction.NONE -> {}
        }
        pendingAction = PendingAction.NONE
    }

    private fun bindUI(model: ReceiptUiModel, currentPaymentMethod: String?) {
        with(binding) {
            tvReceiptNumber.text = model.receiptNumber
            tvDevice.text = model.deviceName
            tvSessionType.text = model.sessionType
            tvStarted.text = model.startTime
            tvEnded.text = model.endTime
            tvDuration.text = model.duration

            labelPausedDuration.isVisible = model.hasPausedDuration
            tvPausedDuration.isVisible = model.hasPausedDuration
            if (model.hasPausedDuration) {
                tvPausedDuration.text = model.pausedDuration
            }

            tvRate.text = model.ratePerHour
            tvPlayCost.text = model.playCost
            tvProductsCost.text = model.productsCost
            tvTotal.text = model.totalAmount

            tvProductsList.text = model.productsListDisplay
            tvProductsList.isVisible = model.hasProducts
            tvNoProducts.isVisible = !model.hasProducts

            when (currentPaymentMethod) {
                "cash" -> chipCash.isChecked = true
                "card" -> chipCard.isChecked = true
            }

            btnPrint.setOnClickListener {
                pendingAction = PendingAction.PRINT
                val currentModel = uiModel ?: return@setOnClickListener
                receiptViewModel.generateReceiptPdf(
                    currentModel,
                    getString(R.string.app_name),
                    getString(R.string.coming_soon)
                )
            }
            btnShare.setOnClickListener {
                showShareOptions()
            }
            btnClose.setOnClickListener { dismiss() }

            chipGroupPayment.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val newPaymentMethod = when (checkedIds[0]) {
                        R.id.chipCash -> "cash"
                        R.id.chipCard -> "card"
                        else -> "cash"
                    }

                    if (newPaymentMethod != currentPaymentMethod) {
                        val currentReceipt = this@ReceiptDetailDialog.receipt ?: return@setOnCheckedStateChangeListener
                        val updatedReceipt = currentReceipt.copy(paymentMethod = newPaymentMethod)
                        this@ReceiptDetailDialog.receipt = updatedReceipt
                        receiptViewModel.updatePaymentMethod(updatedReceipt, newPaymentMethod)
                    }
                }
            }
        }
    }

    private fun showShareOptions() {
        val options = arrayOf(
            getString(R.string.share_text),
            getString(R.string.share_pdf)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.share)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareText()
                    1 -> {
                        pendingAction = PendingAction.SHARE_PDF
                        val currentModel = uiModel ?: return@setItems
                        receiptViewModel.generateReceiptPdf(
                            currentModel,
                            getString(R.string.app_name),
                            getString(R.string.coming_soon)
                        )
                    }
                }
            }
            .show()
    }

    private fun shareText() {
        val shareText = uiModel?.plainTextShareString ?: return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun sharePdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun printPdf(uri: Uri) {
        val printManager = requireContext().getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${getString(R.string.app_name)} ${uiModel?.receiptNumber}"
        
        try {
            val pda = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    
                    val info = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        requireContext().contentResolver.openInputStream(uri)?.use { input ->
                            destination?.let { dest ->
                                java.io.FileOutputStream(dest.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            
            printManager.print(jobName, pda, null)
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(e.message)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
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
