package net.typeblog.lpac_jni

interface LocalProfileAssistant {
    val profiles: List<LocalProfileInfo>
    val notifications: List<LocalProfileNotification>
    val eID: String
    // Extended EuiccInfo for use with LUIs, containing information such as firmware version
    val euiccInfo2: EuiccInfo2?

    fun enableProfile(iccid: String): Boolean
    fun disableProfile(iccid: String): Boolean
    fun deleteProfile(iccid: String): Boolean

    fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                        confirmationCode: String?, callback: ProfileDownloadCallback): Boolean

    fun deleteNotification(seqNumber: Long): Boolean
    fun handleNotification(seqNumber: Long): Boolean
    // Handle the latest entry of a particular type of notification
    // Note that this is not guaranteed to always be reliable and no feedback will be provided on errors.
    fun handleLatestNotification(operation: LocalProfileNotification.Operation)

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}