package net.typeblog.lpac_jni

data class LocalProfileInfo(
    val iccid: String,
    val state: State,
    val name: String,
    val nickName: String,
    val providerName: String,
    val isdpAID: String,
    val profileClass: ProfileClass
) {
    enum class State {
        Enabled,
        Disabled;

        companion object {
            @JvmStatic
            fun fromString(str: String?) =
                when (str?.lowercase()) {
                    "enabled" -> Enabled
                    "disabled" -> Disabled
                    else -> Disabled
                }
        }
    }

}
