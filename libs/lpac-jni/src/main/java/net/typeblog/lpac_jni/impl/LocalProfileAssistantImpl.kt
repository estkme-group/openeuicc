package net.typeblog.lpac_jni.impl

import net.typeblog.lpac_jni.LpacJni
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo

class LocalProfileAssistantImpl(
    apduInterface: ApduInterface,
    httpInterface: HttpInterface
): LocalProfileAssistant {
    private val contextHandle: Long = LpacJni.createContext(apduInterface, httpInterface)
    init {
        if (LpacJni.es10xInit(contextHandle) < 0) {
            throw IllegalArgumentException("Failed to initialize LPA")
        }
    }

    override val profiles: List<LocalProfileInfo>
        get() = LpacJni.es10cGetProfilesInfo(contextHandle)!!.asList() // TODO: Maybe we need better error handling

    override val eID: String by lazy {
        LpacJni.es10cGetEid(contextHandle)!!
    }

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
        LpacJni.es10xFini(contextHandle)
        LpacJni.destroyContext(contextHandle)
    }
}