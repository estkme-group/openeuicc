package net.typeblog.lpac_jni

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LocalProfileAssistant {
    companion object {
        private const val TAG = "LocalProfileAssistant"
    }

    val profiles: List<LocalProfileInfo>
    val notifications: List<LocalProfileNotification>
    val eID: String
    // Extended EuiccInfo for use with LUIs, containing information such as firmware version
    val euiccInfo2: EuiccInfo2?

    // All blocking functions in this class assume that they are executed on non-Main threads
    // The IO context in Kotlin's coroutine library is recommended.
    fun enableProfile(iccid: String): Boolean
    fun disableProfile(iccid: String): Boolean
    fun deleteProfile(iccid: String): Boolean

    fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                        confirmationCode: String?, callback: ProfileDownloadCallback): Boolean

    fun deleteNotification(seqNumber: Long): Boolean
    fun handleNotification(seqNumber: Long): Boolean

    // Wraps an operation on the eSIM chip (any of the other blocking functions)
    // Handles notifications automatically after the operation, unless the lambda executing
    // the operation returns false, which inhibits automatic notification processing.
    // All code executed within are also wrapped automatically in the IO context.
    suspend fun beginOperation(op: suspend LocalProfileAssistant.() -> Boolean) =
        withContext(Dispatchers.IO) {
            val latestSeq = notifications.firstOrNull()?.seqNumber ?: 0
            Log.d(TAG, "Latest notification is $latestSeq before operation")
            if (op(this@LocalProfileAssistant)) {
                Log.d(TAG, "Operation has requested notification handling")
                notifications.filter { it.seqNumber > latestSeq }.forEach {
                    Log.d(TAG, "Handling notification $it")
                    handleNotification(it.seqNumber)
                }
            }
            Log.d(TAG, "Operation complete")
        }

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}