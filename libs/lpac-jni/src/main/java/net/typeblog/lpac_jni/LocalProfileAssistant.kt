package net.typeblog.lpac_jni

import net.typeblog.lpac_jni.HttpInterface.HttpResponse

interface LocalProfileAssistant {
    data class ProfileDownloadException(
        val lastHttpResponse: HttpResponse?,
        val lastHttpException: Exception?,
    ) : Exception("Failed to download profile")

    val valid: Boolean
    val profiles: List<LocalProfileInfo>
    val notifications: List<LocalProfileNotification>
    val eID: String
    // Extended EuiccInfo for use with LUIs, containing information such as firmware version
    val euiccInfo2: EuiccInfo2?

    /**
     * Set the max segment size (mss) for all es10x commands. This can help with removable
     * eUICCs that may run at a baud rate too fast for the modem.
     * By default, this is set to 60 by libeuicc.
     */
    fun setEs10xMss(mss: Byte)

    // All blocking functions in this class assume that they are executed on non-Main threads
    // The IO context in Kotlin's coroutine library is recommended.
    fun enableProfile(iccid: String, refresh: Boolean = true): Boolean
    fun disableProfile(iccid: String, refresh: Boolean = true): Boolean
    fun deleteProfile(iccid: String): Boolean

    fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                        confirmationCode: String?, callback: ProfileDownloadCallback)

    fun deleteNotification(seqNumber: Long): Boolean
    fun handleNotification(seqNumber: Long): Boolean

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}