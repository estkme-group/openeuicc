package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*

class DownloadWizardDetailsFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    private var inputComplete = false

    override val hasNext: Boolean
        get() = inputComplete
    override val hasPrev: Boolean
        get() = true

    private val address: EditText by lazy {
        requireView().requireViewById<TextInputLayout>(R.id.profile_download_server).editText!!
    }
    private val matchingId: EditText by lazy {
        requireView().requireViewById<TextInputLayout>(R.id.profile_download_code).editText!!
    }
    private val confirmationCodeInput: TextInputLayout by lazy {
        requireView().requireViewById(R.id.profile_download_confirmation_code)
    }
    private val confirmationCode: EditText by lazy {
        confirmationCodeInput.editText!!
    }
    private val imei: EditText by lazy {
        requireView().requireViewById<TextInputLayout>(R.id.profile_download_imei).editText!!
    }

    private fun saveState() {
        state.smdp = address.text.toString().trim()
        // Treat empty inputs as null -- this is important for the download step
        state.matchingId = matchingId.text.toString().trim().ifBlank { null }
        state.confirmationCode = confirmationCode.text.toString().trim().ifBlank { null }
        state.imei = imei.text.toString().ifBlank { null }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_download_details, container, /* attachToRoot = */ false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        address.addTextChangedListener(onTextChanged = ::handlePasteLPAString)
        address.addTextChangedListener { updateInputCompleteness() }
        matchingId.addTextChangedListener(onTextChanged = ::handlePasteLPAString)
        confirmationCode.addTextChangedListener { updateInputCompleteness() }
    }

    override fun onStart() {
        super.onStart()
        address.setText(state.smdp)
        matchingId.setText(state.matchingId)
        confirmationCode.setText(state.confirmationCode)
        imei.setText(state.imei)
        updateInputCompleteness()

        if (state.confirmationCodeRequired) {
            confirmationCodeInput.requestFocus()
            confirmationCodeInput.setHint(R.string.profile_download_confirmation_code_required)
        } else {
            confirmationCodeInput.setHint(R.string.profile_download_confirmation_code)
        }
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }

    private fun handlePasteLPAString(text: CharSequence?, start: Int, before: Int, count: Int) {
        if (start > 0 || before > 0) return // only handle insertions at the beginning
        if (text == null || !text.startsWith("LPA:", ignoreCase = true)) return
        val parsed = LPAString.parse(text)
        address.setText(parsed.address)
        matchingId.setText(parsed.matchingId)
        if (parsed.confirmationCodeRequired) confirmationCode.requestFocus()
    }

    private fun updateInputCompleteness() {
        inputComplete = isValidAddress(address.text)
        if (state.confirmationCodeRequired) {
            inputComplete = inputComplete && confirmationCode.text.isNotEmpty()
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
        fqdn = input.take(portIndex)
        port = input.substring(portIndex + 1, input.length).toIntOrNull(10) ?: 0
    }
    // see https://en.wikipedia.org/wiki/Port_(computer_networking)
    if (port !in 1..0xffff) return false
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
