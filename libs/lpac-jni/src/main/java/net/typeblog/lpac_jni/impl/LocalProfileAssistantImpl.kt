package net.typeblog.lpac_jni.impl

import android.util.Log
import net.typeblog.lpac_jni.LpacJni
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileDownloadCallback

class LocalProfileAssistantImpl(
    private val apduInterface: ApduInterface,
    httpInterface: HttpInterface
): LocalProfileAssistant {
    companion object {
        private const val TAG = "LocalProfileAssistantImpl"
    }

    private var finalized = false
    private var contextHandle: Long = LpacJni.createContext(apduInterface, httpInterface)

    init {
        if (LpacJni.euiccInit(contextHandle) < 0) {
            throw IllegalArgumentException("Failed to initialize LPA")
        }

        val pkids = euiccInfo2?.euiccCiPKIdListForVerification ?: arrayOf()
        httpInterface.usePublicKeyIds(pkids)
    }

    override val valid: Boolean
        get() = !finalized && apduInterface.valid && try {
            // If we can read both eID and euiccInfo2 properly, we are likely looking at
            // a valid LocalProfileAssistant
            eID
            euiccInfo2!!
            true
        } catch (e: Exception) {
            false
        }

    override val profiles: List<LocalProfileInfo>
        get() = LpacJni.es10cGetProfilesInfo(contextHandle)?.asList() ?: listOf()

    override val notifications: List<LocalProfileNotification>
        get() =
            (LpacJni.es10bListNotification(contextHandle) ?: arrayOf())
                .sortedBy { it.seqNumber }.reversed()

    override val eID: String
        get() = LpacJni.es10cGetEid(contextHandle)!!

    override val euiccInfo2: EuiccInfo2?
        get() = LpacJni.es10cexGetEuiccInfo2(contextHandle)

    override fun enableProfile(iccid: String, refresh: Boolean): Boolean =
        LpacJni.es10cEnableProfile(contextHandle, iccid, refresh) == 0

    override fun disableProfile(iccid: String, refresh: Boolean): Boolean =
        LpacJni.es10cDisableProfile(contextHandle, iccid, refresh) == 0

    override fun deleteProfile(iccid: String): Boolean =
        LpacJni.es10cDeleteProfile(contextHandle, iccid) == 0

    @Synchronized
    override fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                                 confirmationCode: String?, callback: ProfileDownloadCallback): Boolean {
        return LpacJni.downloadProfile(
            contextHandle,
            smdp,
            matchingId,
            imei,
            confirmationCode,
            callback
        ) == 0
    }

    override fun deleteNotification(seqNumber: Long): Boolean =
        LpacJni.es10bDeleteNotification(contextHandle, seqNumber) == 0

    @Synchronized
    override fun handleNotification(seqNumber: Long): Boolean =
        LpacJni.handleNotification(contextHandle, seqNumber).also {
            Log.d(TAG, "handleNotification $seqNumber = $it")
        } == 0

    override fun setNickname(iccid: String, nickname: String): Boolean =
        LpacJni.es10cSetNickname(contextHandle, iccid, nickname) == 0

    @Synchronized
    override fun close() {
        if (!finalized) {
            LpacJni.euiccFini(contextHandle)
            LpacJni.destroyContext(contextHandle)
            finalized = true
        }
    }
}