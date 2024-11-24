package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*

class DownloadWizardDiagnosticsFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    override val hasNext: Boolean
        get() = true
    override val hasPrev: Boolean
        get() = false

    private lateinit var diagnosticTextView: TextView

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_diagnostics, container, false)
        diagnosticTextView = view.requireViewById<TextView>(R.id.download_wizard_diagnostics_text)
        return view
    }

    override fun onStart() {
        super.onStart()
        val str = buildDiagnosticsText()
        if (str == null) {
            requireActivity().finish()
            return
        }

        diagnosticTextView.text = str
    }

    private fun buildDiagnosticsText(): String? = state.downloadError?.let { err ->
        val ret = StringBuilder()

        err.lastHttpResponse?.let { resp ->
            if (resp.rcode != 200) {
                // Only show the status if it's not 200
                // Because we can have errors even if the rcode is 200 due to SM-DP+ servers being dumb
                // and showing 200 might mislead users
                ret.appendLine(
                    getString(
                        R.string.download_wizard_diagnostics_last_http_status,
                        resp.rcode
                    )
                )
                ret.appendLine()
            }

            ret.appendLine(getString(R.string.download_wizard_diagnostics_last_http_response))
            ret.appendLine()

            val str = resp.data.decodeToString(throwOnInvalidSequence = false)
            ret.appendLine(
                if (str.startsWith('{')) {
                    str.prettyPrintJson()
                } else {
                    str
                }
            )
        }

        ret.toString()
    }
}