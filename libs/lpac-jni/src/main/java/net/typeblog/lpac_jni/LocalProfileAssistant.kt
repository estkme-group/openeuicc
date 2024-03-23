package net.typeblog.lpac_jni

interface LocalProfileAssistant {
    val valid: Boolean
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

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}