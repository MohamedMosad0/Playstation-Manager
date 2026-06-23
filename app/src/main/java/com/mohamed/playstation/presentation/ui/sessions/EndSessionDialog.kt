package com.mohamed.playstation.presentation.ui.sessions

import android.app.Dialog
import android.os.Bundle
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
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.end_session_title))
            .setMessage(requireContext().getString(R.string.end_session_message))
            .setPositiveButton(R.string.end_session) { _, _ ->
                endSession()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun endSession() {
        lifecycleScope.launch {
            val session = viewModel.getSessionById(sessionId)
            if (session != null) {
                viewModel.endSession(session) { receiptId ->
                    dismiss()
                    if (isAdded) {
                        ReceiptDetailDialog.newInstance(receiptId)
                            .show(parentFragmentManager, "ReceiptDetailDialog")
                    }
                }
            } else {
                dismiss()
            }
        }
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