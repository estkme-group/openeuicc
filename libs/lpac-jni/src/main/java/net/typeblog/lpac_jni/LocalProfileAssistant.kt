package net.typeblog.lpac_jni

interface LocalProfileAssistant {
    val profiles: List<LocalProfileInfo>
    val eID: String

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