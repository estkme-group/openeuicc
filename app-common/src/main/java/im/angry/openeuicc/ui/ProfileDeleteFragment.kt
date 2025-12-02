package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
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

class ProfileDeleteFragment : DialogFragment(), EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "ProfileDeleteFragment"
        private const val FIELD_ICCID = "iccid"
        private const val FIELD_NAME = "name"

        fun newInstance(slotId: Int, portId: Int, seId: EuiccChannel.SecureElementId, iccid: String, name: String) =
            newInstanceEuicc(ProfileDeleteFragment::class.java, slotId, portId, seId) {
                putString(FIELD_ICCID, iccid)
                putString(FIELD_NAME, name)
            }
    }

    private val iccid by lazy {
        requireArguments().getString(FIELD_ICCID)!!
    }

    private val name by lazy {
        requireArguments().getString(FIELD_NAME)!!
    }

    private val editText by lazy {
        EditText(requireContext()).apply {
            hint = Editable.Factory.getInstance()
                .newEditable(getString(R.string.profile_delete_confirm_input, name))
        }
    }

    private val inputMatchesName: Boolean
        get() = editText.text.toString() == name

    private var toast: Toast? = null

    private var deleting = false

    private val alertDialog: AlertDialog
        get() = requireDialog() as AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme).apply {
            setMessage(getString(R.string.profile_delete_confirm, name))
            setView(editText)
            setPositiveButton(android.R.string.ok, null) // Set listener to null to prevent auto closing
            setNegativeButton(android.R.string.cancel, null)
        }.create()

    override fun onResume() {
        super.onResume()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!deleting) delete()
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            if (!deleting) dismiss()
        }
    }

    private fun delete() {
        toast?.cancel()
        if (!inputMatchesName) {
            val resId = R.string.toast_profile_delete_confirm_text_mismatched
            toast = Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).also {
                it.show()
            }
            return
        }
        deleting = true
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false

        requireParentFragment().lifecycleScope.launch {
            ensureEuiccChannelManager()
            euiccChannelManagerService.waitForForegroundTask()
            euiccChannelManagerService.launchProfileDeleteTask(slotId, portId, seId, iccid)
                .onStart {
                    parentFragment?.notifyEuiccProfilesChanged()
                    runCatching(::dismiss)
                }
                .waitDone()
        }
    }
}
