package im.angry.openeuicc.ui.wizard

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import org.json.JSONObject
import java.util.Date

class DownloadWizardDiagnosticsFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    override val hasNext: Boolean
        get() = true
    override val hasPrev: Boolean
        get() = false

    private lateinit var diagnosticTextView: TextView

    private val saveDiagnostics =
        setupLogSaving(
            getLogFileName = {
                getString(
                    R.string.download_wizard_diagnostics_file_template,
                    SimpleDateFormat.getDateTimeInstance().format(Date())
                )
            },
            getLogText = { diagnosticTextView.text.toString() }
        )

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_diagnostics, container, false)
        view.requireViewById<View>(R.id.download_wizard_diagnostics_save).setOnClickListener {
            saveDiagnostics()
        }
        diagnosticTextView = view.requireViewById(R.id.download_wizard_diagnostics_text)
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

        ret.appendLine(
            getString(
                R.string.download_wizard_diagnostics_error_code,
                err.lpaErrorReason
            )
        )
        ret.appendLine()

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
                    JSONObject(str).toString(2)
                } else {
                    str
                }
            )

            ret.appendLine()
        }

        err.lastHttpException?.let { e ->
            ret.appendLine(getString(R.string.download_wizard_diagnostics_last_http_exception))
            ret.appendLine()
            ret.appendLine("${e.javaClass.name}: ${e.message}")
            ret.appendLine(e.stackTrace.joinToString("\n"))
            ret.appendLine()
        }

        err.lastApduResponse?.let { resp ->
            val isSuccess =
                resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()

            if (isSuccess) {
                ret.appendLine(getString(R.string.download_wizard_diagnostics_last_apdu_response_success))
            } else {
                // Only show the full APDU response when it's a failure
                // Otherwise it's going to get very crammed
                ret.appendLine(
                    getString(
                        R.string.download_wizard_diagnostics_last_apdu_response,
                        resp.encodeHex()
                    )
                )
                ret.appendLine()

                ret.appendLine(getString(R.string.download_wizard_diagnostics_last_apdu_response_fail))
            }
        }

        err.lastApduException?.let { e ->
            ret.appendLine(getString(R.string.download_wizard_diagnostics_last_apdu_exception))
            ret.appendLine()
            ret.appendLine("${e.javaClass.name}: ${e.message}")
            ret.appendLine(e.stackTrace.joinToString("\n"))
            ret.appendLine()
        }

        ret.toString()
    }
}
