package com.mohamed.playstation.presentation.ui.sessions

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohamed.playstation.R
import com.mohamed.playstation.presentation.ui.receipts.ReceiptDetailDialog
import com.mohamed.playstation.presentation.viewmodel.SessionViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Dialog لتأكيد إنهاء الجلسة
 */
@AndroidEntryPoint
class EndSessionDialog : DialogFragment() {

    private val viewModel: SessionViewModel by viewModels({ requireParentFragment() })

    private var sessionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = requireArguments().getLong(ARG_SESSION_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.end_session_title))
            .setMessage(requireContext().getString(R.string.end_session_message))
            .setPositiveButton(R.string.end_session, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                setCompletionInProgress(dialog)
                endSession(dialog)
            }
        }
        return dialog
    }

    private fun setCompletionInProgress(dialog: AlertDialog) {
        isCancelable = false
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false
        dialog.setMessage(getString(R.string.finishing_session_progress))
    }

    private fun endSession(dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val session = viewModel.getSessionById(sessionId)
                if (session != null) {
                    viewModel.endSession(
                        session = session,
                        onReceiptCreated = { receiptId ->
                            dismissAllowingStateLoss()
                            if (isAdded) {
                                ReceiptDetailDialog.newInstance(receiptId)
                                    .show(parentFragmentManager, "ReceiptDetailDialog")
                            }
                        },
                        onError = {
                            showCompletionError(dialog)
                        }
                    )
                } else {
                    showCompletionError(dialog)
                }
            } catch (_: Exception) {
                showCompletionError(dialog)
            }
        }
    }

    private fun showCompletionError(dialog: AlertDialog) {
        if (!isAdded) return
        isCancelable = true
        dialog.setMessage(getString(R.string.end_session_failed))
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = true
    }

    companion object {
        private const val ARG_SESSION_ID = "session_id"

        fun newInstance(sessionId: Long): EndSessionDialog {
            return EndSessionDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SESSION_ID, sessionId)
                }
            }
        }
    }
}
