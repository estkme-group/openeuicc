package im.angry.openeuicc.ui.wizard

import androidx.annotation.StringRes
import im.angry.openeuicc.common.R
import net.typeblog.lpac_jni.LocalProfileAssistant
import org.json.JSONObject
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class SimplifiedErrorMessages(
    @StringRes val titleResId: Int,
    @StringRes val suggestResId: Int?
) {
    ICCIDAlreadyInUse(
        R.string.download_wizard_error_iccid_already,
        R.string.download_wizard_error_suggest_profile_installed
    ),
    InsufficientMemory(
        R.string.download_wizard_error_insufficient_memory,
        R.string.download_wizard_error_suggest_insufficient_memory
    ),
    UnsupportedProfile(
        R.string.download_wizard_error_unsupported_profile,
        null
    ),
    CardInternalError(
        R.string.download_wizard_error_card_internal_error,
        null
    ),
    EIDNotSupported(
        R.string.download_wizard_error_eid_not_supported,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    EIDMismatch(
        R.string.download_wizard_error_eid_mismatch,
        R.string.download_wizard_error_suggest_contact_reissue
    ),
    UnreleasedProfile(
        R.string.download_wizard_error_profile_unreleased,
        R.string.download_wizard_error_suggest_contact_reissue
    ),
    UnavailableProfile(
        R.string.download_wizard_error_profile_unavailable,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    MatchingIDRefused(
        R.string.download_wizard_error_matching_id_refused,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    ProfileRetriesExceeded(
        R.string.download_wizard_error_profile_retries_exceeded,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    ConfirmationCodeMissing(
        R.string.download_wizard_error_confirmation_code_missing,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    ConfirmationCodeRefused(
        R.string.download_wizard_error_confirmation_code_refused,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    ConfirmationCodeRetriesExceeded(
        R.string.download_wizard_error_confirmation_code_retries_exceeded,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    ProfileExpired(
        R.string.download_wizard_error_profile_expired,
        R.string.download_wizard_error_suggest_contact_carrier
    ),
    UnknownHost(
        R.string.download_wizard_error_unknown_hostname,
        null
    ),
    NetworkUnreachable(
        R.string.download_wizard_error_network_unreachable,
        R.string.download_wizard_error_suggest_network_unreachable
    ),
    TLSError(
        R.string.download_wizard_error_tls_certificate,
        null
    );

    companion object {
        private val httpErrors = buildMap {
            // @formatter:off
            // Stage: InitiateAuthentication
            put("8.8.1" to "3.8", UnknownHost) // Invalid SM-DP+ Address.
            put("8.8.2" to "3.1", UnsupportedProfile) // None of the proposed Public Key Identifiers is supported by the SM-DP+.
            put("8.8.3" to "3.1", UnsupportedProfile) // The SVN indicated by the eUICC is not supported by the SM-DP+.
            put("8.8.4" to "3.7", UnsupportedProfile) // The SM-DP+ has no CERT.DPAuth.ECDSA signed by one of the GSMA CI Public Key supported by the eUICC.

            // Stage: AuthenticateClient
            put("8.1" to "4.8", InsufficientMemory) // eUICC does not have sufficient space for this Profile.
            put("8.1.1" to "2.1", EIDNotSupported) // eUICC does not support the EID.
            put("8.1.1" to "3.8", EIDMismatch) // EID doesn't match the expected value.
            put("8.1.2" to "6.1", UnsupportedProfile) // EUM Certificate is invalid.
            put("8.1.2" to "6.3", UnsupportedProfile) // EUM Certificate has expired.
            put("8.1.3" to "6.1", UnsupportedProfile) // eUICC Certificate is invalid.
            put("8.1.3" to "6.3", UnsupportedProfile) // eUICC Certificate has expired.
            put("8.2" to "1.2", UnreleasedProfile) // Profile has not yet been released.
            put("8.2.5" to "4.3", UnavailableProfile) // No eligible Profile for this eUICC/Device.
            put("8.2.6" to "3.8", MatchingIDRefused) // MatchingID (AC_Token or EventID) is refused.
            put("8.8" to "4.2", EIDNotSupported) // eUICC is not supported by the SM-DP+.
            put("8.8.5" to "6.4", ProfileRetriesExceeded) // The maximum number of retries for the Profile download order has been exceeded.
            put("8.10.1" to "3.9", UnsupportedProfile) // The RSP session identified by the TransactionID is unknown.
            put("8.11.1" to "3.9", UnsupportedProfile) // Unknown CI Public Key.

            // Stage: GetBoundProfilePackage
            put("8.2" to "3.7", UnavailableProfile) // BPP is not available for a new binding.
            put("8.2.7" to "2.2", ConfirmationCodeMissing) // Confirmation Code is missing.
            put("8.2.7" to "3.8", ConfirmationCodeRefused) // Confirmation Code is refused.
            put("8.2.7" to "6.4", ConfirmationCodeRetriesExceeded) // The maximum number of retries for the Confirmation Code has been exceeded.

            // Stage: AuthenticateClient, GetBoundProfilePackage
            put("8.1" to "6.1", UnsupportedProfile) // eUICC Signature is invalid.
            put("8.8.5" to "4.10", ProfileExpired) // The Download order has expired.
            // @formatter:on
        }

        fun fromDownloadError(exc: LocalProfileAssistant.ProfileDownloadException) = when {
            exc.lpaErrorReason != "ES10B_ERROR_REASON_UNDEFINED" -> fromLPAErrorReason(exc.lpaErrorReason)
            exc.lastHttpResponse?.rcode == 200 -> fromHTTPResponse(exc.lastHttpResponse!!)
            exc.lastHttpException != null -> fromHTTPException(exc.lastHttpException!!)
            exc.lastApduResponse != null -> fromAPDUResponse(exc.lastApduResponse!!)
            else -> null
        }

        private fun fromLPAErrorReason(reason: String) = when (reason) {
            "ES10B_ERROR_REASON_UNSUPPORTED_CRT_VALUES" -> UnsupportedProfile
            "ES10B_ERROR_REASON_UNSUPPORTED_REMOTE_OPERATION_TYPE" -> UnsupportedProfile
            "ES10B_ERROR_REASON_UNSUPPORTED_PROFILE_CLASS" -> UnsupportedProfile
            "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_ICCID_ALREADY_EXISTS_ON_EUICC" -> ICCIDAlreadyInUse
            "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INSUFFICIENT_MEMORY_FOR_PROFILE" -> InsufficientMemory
            "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INTERRUPTION" -> CardInternalError
            "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_PE_PROCESSING_ERROR" -> CardInternalError
            else -> null
        }

        private fun fromHTTPResponse(httpResponse: net.typeblog.lpac_jni.HttpInterface.HttpResponse): SimplifiedErrorMessages? {
            if (httpResponse.data.first().toInt() != '{'.code) return null
            val response = JSONObject(httpResponse.data.decodeToString())
            val statusCodeData = response.optJSONObject("header")
                ?.optJSONObject("functionExecutionStatus")
                ?.optJSONObject("statusCodeData")
                ?: return null
            val subjectCode = statusCodeData.optString("subjectCode")
            val reasonCode = statusCodeData.optString("reasonCode")
            return httpErrors[subjectCode to reasonCode]
        }

        private fun fromHTTPException(exc: Exception) = when (exc) {
            is SSLException -> TLSError
            is UnknownHostException -> UnknownHost
            is NoRouteToHostException -> NetworkUnreachable
            is PortUnreachableException -> NetworkUnreachable
            is SocketTimeoutException -> NetworkUnreachable
            is SocketException -> exc.message
                ?.contains("Connection reset", ignoreCase = true)
                ?.let { if (it) NetworkUnreachable else null }

            else -> null
        }

        private fun fromAPDUResponse(resp: ByteArray): SimplifiedErrorMessages? {
            val isSuccess = resp.size >= 2 &&
                resp[resp.size - 2] == 0x90.toByte() &&
                resp[resp.size - 1] == 0x00.toByte()
            if (isSuccess) return null
            return CardInternalError
        }
    }
}
