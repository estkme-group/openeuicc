package net.typeblog.lpac_jni

interface LocalProfileAssistant {
    val profiles: List<LocalProfileInfo>
    val eID: String
    // Extended EuiccInfo for use with LUIs, containing information such as firmware version
    val euiccInfo2: EuiccInfo2?

    fun enableProfile(iccid: String): Boolean
    fun disableProfile(iccid: String): Boolean
    fun deleteProfile(iccid: String): Boolean

    fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                        confirmationCode: String?, callback: ProfileDownloadCallback): Boolean

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}