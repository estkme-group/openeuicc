package net.typeblog.lpac_jni

interface LocalProfileAssistant {
    val profiles: List<LocalProfileInfo>
    val eID: String

    fun enableProfile(iccid: String): Boolean
    fun disableProfile(iccid: String): Boolean
    fun deleteProfile(iccid: String): Boolean

    fun downloadProfile(matchingId: String, imei: String)

    fun setNickname(
        iccid: String, nickname: String
    ): Boolean

    fun close()
}