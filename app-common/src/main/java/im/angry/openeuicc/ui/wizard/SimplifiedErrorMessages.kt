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
            // Stage: AuthenticateClient
            put("8.1" to "4.8", InsufficientMemory)
            put("8.1.1" to "2.1", EIDNotSupported)
            put("8.1.1" to "3.8", EIDMismatch)
            put("8.2" to "1.2", UnreleasedProfile)
            put("8.2.6" to "3.8", MatchingIDRefused)
            put("8.8.5" to "6.4", ProfileRetriesExceeded)

            // Stage: GetBoundProfilePackage
            put("8.2.7" to "2.2", ConfirmationCodeMissing)
            put("8.2.7" to "3.8", ConfirmationCodeRefused)
            put("8.2.7" to "6.4", ConfirmationCodeRetriesExceeded)

            // Stage: AuthenticateClient, GetBoundProfilePackage
            put("8.8.5" to "4.10", ProfileExpired)
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
