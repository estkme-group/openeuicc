package net.typeblog.lpac_jni

import net.typeblog.lpac_jni.HttpInterface.HttpResponse

interface LocalProfileAssistant {
    @Suppress("ArrayInDataClass")
    data class ProfileDownloadException(
        val lpaErrorReason: String,
        val lastHttpResponse: HttpResponse?,
        val lastHttpException: Exception?,
        val lastApduResponse: ByteArray?,
        val lastApduException: Exception?,
    ) : Exception("Failed to download profile")

    class ProfileRenameException() : Exception("Failed to rename profile")
    class ProfileNameTooLongException() : Exception("Profile name too long")
    class ProfileNameIsInvalidUTF8Exception() : Exception("Profile name is invalid UTF-8")

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

    fun euiccMemoryReset()

    /**
     * Nickname must be valid UTF-8 and shorter than 64 chars.
     *
     * May throw one of: ProfileRenameException, ProfileNameTooLongException, ProfileNameIsInvalidUTF8Exception
     */
    fun setNickname(
        iccid: String, nickname: String
    )

    fun close()
}