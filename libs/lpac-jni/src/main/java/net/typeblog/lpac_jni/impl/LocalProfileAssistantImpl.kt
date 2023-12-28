package net.typeblog.lpac_jni.impl

import net.typeblog.lpac_jni.LpacJni
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.ProfileDownloadCallback

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
        get() = LpacJni.es10cGetProfilesInfo(contextHandle)!!.asList()

    override val eID: String
        get() = LpacJni.es10cGetEid(contextHandle)!!

    override val euiccInfo2: EuiccInfo2?
        get() = LpacJni.es10cexGetEuiccInfo2(contextHandle)

    override fun enableProfile(iccid: String): Boolean {
        return LpacJni.es10cEnableProfile(contextHandle, iccid) == 0
    }

    override fun disableProfile(iccid: String): Boolean {
        return LpacJni.es10cDisableProfile(contextHandle, iccid) == 0
    }

    override fun deleteProfile(iccid: String): Boolean {
        return LpacJni.es10cDeleteProfile(contextHandle, iccid) == 0
    }

    override fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                                 confirmationCode: String?, callback: ProfileDownloadCallback): Boolean {
        return LpacJni.downloadProfile(contextHandle, smdp, matchingId, imei, confirmationCode, callback) == 0
    }

    override fun setNickname(iccid: String, nickname: String): Boolean {
        return LpacJni.es10cSetNickname(contextHandle, iccid, nickname) == 0
    }

    override fun close() {
        LpacJni.es10xFini(contextHandle)
        LpacJni.destroyContext(contextHandle)
    }
}