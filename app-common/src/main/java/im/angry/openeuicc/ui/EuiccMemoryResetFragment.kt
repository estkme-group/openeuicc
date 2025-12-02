package im.angry.openeuicc.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.service.EuiccChannelManagerService.Companion.waitDone
import im.angry.openeuicc.util.EuiccChannelFragmentMarker
import im.angry.openeuicc.util.ensureEuiccChannelManager
import im.angry.openeuicc.util.euiccChannelManagerService
import im.angry.openeuicc.util.newInstanceEuicc
import im.angry.openeuicc.util.notifyEuiccProfilesChanged
import im.angry.openeuicc.util.portId
import im.angry.openeuicc.util.seId
import im.angry.openeuicc.util.slotId
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class EuiccMemoryResetFragment : DialogFragment(), EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "EuiccMemoryResetFragment"

        private const val FIELD_EID = "eid"

        fun newInstance(slotId: Int, portId: Int, seId: EuiccChannel.SecureElementId, eid: String) =
            newInstanceEuicc(EuiccMemoryResetFragment::class.java, slotId, portId, seId) {
                putString(FIELD_EID, eid)
            }
    }

    private val eid: String by lazy { requireArguments().getString(FIELD_EID)!! }

    private val confirmText: String by lazy {
        getString(R.string.euicc_memory_reset_confirm_text, eid.takeLast(8))
    }

    private inline val isMatched: Boolean
        get() = editText.text.toString() == confirmText

    private var confirmed = false

    private var toast: Toast? = null
        set(value) {
            toast?.cancel()
            field = value
            value?.show()
        }

    private val editText by lazy {
        EditText(requireContext()).apply {
            isLongClickable = false
            typeface = Typeface.MONOSPACE
            hint = Editable.Factory.getInstance()
                .newEditable(getString(R.string.euicc_memory_reset_hint_text, confirmText))
        }
    }

    private inline val alertDialog: AlertDialog
        get() = requireDialog() as AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(R.string.euicc_memory_reset_title)
            .setMessage(getString(R.string.euicc_memory_reset_message, eid, confirmText))
            .setView(editText)
            // Set listener to null to prevent auto closing
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.euicc_memory_reset_invoke_button, null)
            .create()

    override fun onResume() {
        super.onResume()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener { if (!confirmed) confirmation() }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener { if (!confirmed) dismiss() }
    }

    private fun confirmation() {
        toast?.cancel()
        if (!isMatched) {
            Log.d(TAG, buildString {
                appendLine("User input is mismatch:")
                appendLine(editText.text)
                appendLine(confirmText)
            })
            val resId = R.string.toast_euicc_memory_reset_confirm_text_mismatched
            toast = Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG)
            return
        }
        confirmed = true
        preventUserAction()

        requireParentFragment().lifecycleScope.launch {
            ensureEuiccChannelManager()
            euiccChannelManagerService.waitForForegroundTask()

            euiccChannelManagerService.launchMemoryReset(slotId, portId, seId)
                .onStart {
                    parentFragment?.notifyEuiccProfilesChanged()

                    val resId = R.string.toast_euicc_memory_reset_finitshed
                    toast = Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG)

                    runCatching(::dismiss)
                }
                .waitDone()
        }
    }

    private fun preventUserAction() {
        editText.isEnabled = false
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
    }
}
