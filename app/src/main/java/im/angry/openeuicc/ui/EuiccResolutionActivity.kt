package im.angry.openeuicc.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.service.euicc.EuiccService
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.R
import im.angry.openeuicc.util.*

/**
 * LUI resolution activity, started by com.android.phone's EuiccResolutionUiDispatcherActivity when
 * the platform needs the user's consent (or a confirmation code) for an EuiccManager operation
 * requested by an app. The answer goes back through EuiccManager.continueOperation(), with the
 * intent's extras untouched; declining (or leaving) the dialog fails the operation.
 */
class EuiccResolutionActivity : BaseEuiccAccessActivity() {
    companion object {
        private const val TAG = "EuiccResolutionActivity"
    }

    private var resolved = false

    private val resolutionCallingPackage: String? by lazy {
        intent.getStringExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE)
    }

    private val callingAppLabel: CharSequence by lazy {
        // Without QUERY_ALL_PACKAGES, the calling app may not be visible to us
        resolutionCallingPackage?.let {
            runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0))
            }.getOrDefault(it)
        } ?: getString(R.string.euicc_resolution_unknown_app)
    }

    // Downloads (and metadata lookups) come with INVALID_SUBSCRIPTION_ID
    private val isSwitch: Boolean by lazy {
        intent.getIntExtra(
            EuiccService.EXTRA_RESOLUTION_SUBSCRIPTION_ID,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        ) != SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onInit() {
        val builder = AlertDialog.Builder(this, im.angry.openeuicc.common.R.style.AlertDialogTheme)
        val dialog = when (intent.action) {
            EuiccService.ACTION_RESOLVE_NO_PRIVILEGES -> builder
                .setTitle(
                    getString(
                        if (isSwitch) R.string.euicc_resolution_no_privileges_switch_title
                        else R.string.euicc_resolution_no_privileges_title,
                        callingAppLabel
                    )
                )
                .setMessage(
                    getString(
                        if (isSwitch) R.string.euicc_resolution_no_privileges_switch_message
                        else R.string.euicc_resolution_no_privileges_message,
                        callingAppLabel
                    )
                )
                .setNegativeButton(R.string.euicc_resolution_deny) { _, _ -> resolve(false) }
                .setPositiveButton(R.string.euicc_resolution_allow) { _, _ -> resolve(true) }
                .apply {
                    if (resolutionCallingPackage != null) {
                        setNeutralButton(R.string.euicc_resolution_app_info, null)
                    }
                }
                .create()
                .apply {
                    // Set here so that the button doesn't dismiss the dialog
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", resolutionCallingPackage, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }

            EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM -> builder
                .setTitle(R.string.euicc_resolution_deactivate_sim_title)
                .setMessage(getString(R.string.euicc_resolution_deactivate_sim_message, callingAppLabel))
                .setNegativeButton(R.string.euicc_resolution_cancel) { _, _ -> resolve(false) }
                .setPositiveButton(R.string.euicc_resolution_continue) { _, _ -> resolve(true) }
                .create()

            EuiccService.ACTION_RESOLVE_RESOLVABLE_ERRORS -> {
                val errors = intent.getIntExtra(EuiccService.EXTRA_RESOLVABLE_ERRORS, 0)
                val needsCode = errors and EuiccService.RESOLVABLE_ERROR_CONFIRMATION_CODE != 0
                val needsPolicyRules = errors and EuiccService.RESOLVABLE_ERROR_POLICY_RULES != 0
                val codeView = if (needsCode) {
                    layoutInflater.inflate(R.layout.dialog_euicc_resolution_code, null)
                } else {
                    null
                }
                val codeInput = codeView?.requireViewById<EditText>(R.id.euicc_resolution_code)
                if (intent.getBooleanExtra(EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE_RETRIED, false)) {
                    codeView?.requireViewById<TextInputLayout>(R.id.euicc_resolution_code_layout)?.error =
                        getString(R.string.euicc_resolution_confirmation_code_retry)
                }

                builder
                    .setTitle(R.string.euicc_resolution_resolvable_errors_title)
                    .setMessage(
                        listOfNotNull(
                            if (needsCode) {
                                getString(R.string.euicc_resolution_confirmation_code_message, callingAppLabel)
                            } else null,
                            if (needsPolicyRules) getString(R.string.euicc_resolution_policy_rules_message) else null,
                        ).joinToString("\n\n")
                    )
                    .setView(codeView)
                    .setNegativeButton(R.string.euicc_resolution_cancel) { _, _ -> resolve(false) }
                    .setPositiveButton(R.string.euicc_resolution_continue) { _, _ ->
                        resolve(true, Bundle().apply {
                            if (needsCode) {
                                putString(
                                    EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE,
                                    codeInput?.text?.toString()?.trim()
                                )
                            }
                            if (needsPolicyRules) {
                                putBoolean(EuiccService.EXTRA_RESOLUTION_ALLOW_POLICY_RULES, true)
                            }
                        })
                    }
                    .create()
                    .apply {
                        // The platform fails the operation with an empty confirmation code
                        if (codeInput != null) {
                            setOnShowListener {
                                val positive = getButton(DialogInterface.BUTTON_POSITIVE)
                                positive.isEnabled = !codeInput.text.isNullOrBlank()
                                codeInput.doAfterTextChanged { positive.isEnabled = !it.isNullOrBlank() }
                            }
                        }
                    }
            }

            else -> {
                Log.w(TAG, "Unsupported resolution action ${intent.action}")
                finish()
                return
            }
        }

        // Back declines, but a stray tap outside the dialog doesn't
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnCancelListener { resolve(false) }
        dialog.show()
    }

    override fun onDestroy() {
        // Never leave the operation (and the calling app) hanging
        if (isFinishing && !resolved) {
            resolve(false)
        }
        super.onDestroy()
    }

    private fun resolve(consent: Boolean, extras: Bundle = Bundle()) {
        if (resolved) return
        resolved = true
        Log.i(TAG, "Resolution ${intent.action} for $resolutionCallingPackage: consent=$consent")

        if (!consent && !isSwitch && euiccChannelManagerLoaded.isCompleted) {
            // The platform doesn't tell OpenEuiccService; cancel the download it may have left
            // waiting for confirmation
            euiccChannelManagerService.recoverRunningForegroundTask()
                ?.takeIf { it.key != null }?.backChannel?.trySend(false)
        }

        val resolutionExtras = Bundle(extras).apply {
            putBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT, consent)
            // For RESOLVE_NO_PRIVILEGES, the platform may leave the choice of the port to us
            if (intent.action == EuiccService.ACTION_RESOLVE_NO_PRIVILEGES &&
                intent.getIntExtra(EuiccService.EXTRA_RESOLUTION_PORT_INDEX, TelephonyManager.DEFAULT_PORT_INDEX) < 0
            ) {
                putInt(EuiccService.EXTRA_RESOLUTION_PORT_INDEX, TelephonyManager.DEFAULT_PORT_INDEX)
            }
        }
        try {
            val cardId = intent.getIntExtra(EuiccService.EXTRA_RESOLUTION_CARD_ID, TelephonyManager.UNSUPPORTED_CARD_ID)
            getSystemService(EuiccManager::class.java)!!
                .let { if (cardId >= 0) it.createForCardId(cardId) else it }
                .continueOperation(intent, resolutionExtras)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to continue eUICC operation", e)
        }

        setResult(if (consent) RESULT_OK else RESULT_CANCELED)
        finish()
    }
}
