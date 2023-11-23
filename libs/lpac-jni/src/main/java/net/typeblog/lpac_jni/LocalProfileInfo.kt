package net.typeblog.lpac_jni

import java.lang.IllegalArgumentException

data class LocalProfileInfo(
    val iccid: String,
    val state: State,
    val name: String,
    val nickName: String,
    val providerName: String,
    val isdpAID: String,
    val profileClass: Clazz
) {
    enum class State {
        Enabled,
        Disabled
    }

    enum class Clazz {
        Testing,
        Provisioning,
        Operational
    }

    companion object {
        fun stateFromString(state: String?): State =
            if (state == "0") {
                State.Disabled
            } else {
                State.Enabled
            }

        fun classFromString(clazz: String?): Clazz =
            when (clazz) {
                "0" -> Clazz.Testing
                "1" -> Clazz.Provisioning
                "2" -> Clazz.Operational
                else -> throw IllegalArgumentException("Unknown profile class $clazz")
            }
    }
}