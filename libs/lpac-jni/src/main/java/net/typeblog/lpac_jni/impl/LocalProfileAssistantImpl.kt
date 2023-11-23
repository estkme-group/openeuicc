package net.typeblog.lpac_jni.impl

import net.typeblog.lpac_jni.LpacJni
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo

class LocalProfileAssistantImpl(val apduInterface: ApduInterface, val httpInterface: HttpInterface): LocalProfileAssistant {
    override val profiles: List<LocalProfileInfo>
        get() = listOf()
    override val eID: String
        get() = "1234567890"

    override fun enableProfile(iccid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun disableProfile(iccid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteProfile(iccid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun downloadProfile(matchingId: String, imei: String) {
        TODO("Not yet implemented")
    }

    override fun setNickname(iccid: String, nickname: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        // TODO: use es10x_fini
    }
}