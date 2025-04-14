package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.common.R

class DownloadWizardDetailsFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    private var inputComplete = false

    override val hasNext: Boolean
        get() = inputComplete
    override val hasPrev: Boolean
        get() = true

    private lateinit var smdp: TextInputLayout
    private lateinit var matchingId: TextInputLayout
    private lateinit var confirmationCode: TextInputLayout
    private lateinit var imei: TextInputLayout

    private fun saveState() {
        state.smdp = smdp.editText!!.text.toString().trim()
        // Treat empty inputs as null -- this is important for the download step
        state.matchingId = matchingId.editText!!.text.toString().trim().ifBlank { null }
        state.confirmationCode = confirmationCode.editText!!.text.toString().trim().ifBlank { null }
        state.imei = imei.editText!!.text.toString().ifBlank { null }
    }

    override fun beforeNext() = saveState()

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment =
        DownloadWizardProgressFragment()

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment =
        if (state.skipMethodSelect) {
            DownloadWizardSlotSelectFragment()
        } else {
            DownloadWizardMethodSelectFragment()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_details, container, false)
        smdp = view.requireViewById(R.id.profile_download_server)
        matchingId = view.requireViewById(R.id.profile_download_code)
        confirmationCode = view.requireViewById(R.id.profile_download_confirmation_code)
        imei = view.requireViewById(R.id.profile_download_imei)
        smdp.editText!!.addTextChangedListener {
            updateInputCompleteness()
        }
        confirmationCode.editText!!.addTextChangedListener {
            updateInputCompleteness()
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        smdp.editText!!.setText(state.smdp)
        matchingId.editText!!.setText(state.matchingId)
        confirmationCode.editText!!.setText(state.confirmationCode)
        imei.editText!!.setText(state.imei)
        updateInputCompleteness()

        if (state.confirmationCodeRequired) {
            confirmationCode.editText!!.requestFocus()
            confirmationCode.editText!!.hint =
                getString(R.string.profile_download_confirmation_code_required)
        } else {
            confirmationCode.editText!!.hint =
                getString(R.string.profile_download_confirmation_code)
        }
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }

    private fun updateInputCompleteness() {
        inputComplete = isValidAddress(smdp.editText!!.text)
        if (state.confirmationCodeRequired) {
            inputComplete = inputComplete && confirmationCode.editText!!.text.isNotEmpty()
        }
        refreshButtons()
    }
}

private fun isValidAddress(input: CharSequence): Boolean {
    if (!input.contains('.')) return false
    var fqdn = input
    var port = 443
    if (input.contains(':')) {
        val portIndex = input.lastIndexOf(':')
        fqdn = input.substring(0, portIndex)
        port = input.substring(portIndex + 1, input.length).toIntOrNull(10) ?: 0
    }
    // see https://en.wikipedia.org/wiki/Port_(computer_networking)
    if (port < 1 || port > 0xffff) return false
    // see https://en.wikipedia.org/wiki/Fully_qualified_domain_name
    if (fqdn.isEmpty() || fqdn.length > 255) return false
    for (part in fqdn.split('.')) {
        if (part.isEmpty() || part.length > 64) return false
        if (part.first() == '-' || part.last() == '-') return false
        for (c in part) {
            if (c.isLetterOrDigit() || c == '-') continue
            return false
        }
    }
    return true
}