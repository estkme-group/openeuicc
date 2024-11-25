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
    private val httpInterface: HttpInterface
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

    override fun setEs10xMss(mss: Byte) {
        LpacJni.euiccSetMss(contextHandle, mss)
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
        @Synchronized
        get() {
            val head = LpacJni.es10cGetProfilesInfo(contextHandle)
            var curr = head
            val ret = mutableListOf<LocalProfileInfo>()
            while (curr != 0L) {
                val state = LocalProfileInfo.State.fromString(LpacJni.profileGetStateString(curr))
                val clazz = LocalProfileInfo.Clazz.fromString(LpacJni.profileGetClassString(curr))
                ret.add(LocalProfileInfo(
                    LpacJni.profileGetIccid(curr),
                    state,
                    LpacJni.profileGetName(curr),
                    LpacJni.profileGetNickname(curr),
                    LpacJni.profileGetServiceProvider(curr),
                    LpacJni.profileGetIsdpAid(curr),
                    clazz
                ))
                curr = LpacJni.profilesNext(curr)
            }

            LpacJni.profilesFree(curr)
            return ret
        }

    override val notifications: List<LocalProfileNotification>
        @Synchronized
        get() {
            val head = LpacJni.es10bListNotification(contextHandle)
            var curr = head
            val ret = mutableListOf<LocalProfileNotification>()
            while (curr != 0L) {
                ret.add(LocalProfileNotification(
                    LpacJni.notificationGetSeq(curr),
                    LocalProfileNotification.Operation.fromString(LpacJni.notificationGetOperationString(curr)),
                    LpacJni.notificationGetAddress(curr),
                    LpacJni.notificationGetIccid(curr),
                ))
                curr = LpacJni.notificationsNext(curr)
            }
            LpacJni.notificationsFree(head)
            return ret.sortedBy { it.seqNumber }.reversed()
        }

    override val eID: String
        @Synchronized
        get() = LpacJni.es10cGetEid(contextHandle)!!

    override val euiccInfo2: EuiccInfo2?
        @Synchronized
        get() {
            val cInfo = LpacJni.es10cexGetEuiccInfo2(contextHandle)
            if (cInfo == 0L) return null

            val euiccCiPKIdListForSigning = mutableListOf<String>()
            var curr = LpacJni.euiccInfo2GetEuiccCiPKIdListForSigning(cInfo)
            while (curr != 0L) {
                euiccCiPKIdListForSigning.add(LpacJni.stringDeref(curr))
                curr = LpacJni.stringArrNext(curr)
            }

            val euiccCiPKIdListForVerification = mutableListOf<String>()
            curr = LpacJni.euiccInfo2GetEuiccCiPKIdListForVerification(cInfo)
            while (curr != 0L) {
                euiccCiPKIdListForVerification.add(LpacJni.stringDeref(curr))
                curr = LpacJni.stringArrNext(curr)
            }

            val ret = EuiccInfo2(
                LpacJni.euiccInfo2GetProfileVersion(cInfo),
                LpacJni.euiccInfo2GetEuiccFirmwareVersion(cInfo),
                LpacJni.euiccInfo2GetGlobalPlatformVersion(cInfo),
                LpacJni.euiccInfo2GetSasAcreditationNumber(cInfo),
                LpacJni.euiccInfo2GetPpVersion(cInfo),
                LpacJni.euiccInfo2GetFreeNonVolatileMemory(cInfo).toInt(),
                LpacJni.euiccInfo2GetFreeVolatileMemory(cInfo).toInt(),
                euiccCiPKIdListForSigning.toTypedArray(),
                euiccCiPKIdListForVerification.toTypedArray()
            )

            LpacJni.euiccInfo2Free(cInfo)

            return ret
        }

    @Synchronized
    override fun enableProfile(iccid: String, refresh: Boolean): Boolean =
        LpacJni.es10cEnableProfile(contextHandle, iccid, refresh) == 0

    @Synchronized
    override fun disableProfile(iccid: String, refresh: Boolean): Boolean =
        LpacJni.es10cDisableProfile(contextHandle, iccid, refresh) == 0

    @Synchronized
    override fun deleteProfile(iccid: String): Boolean =
        LpacJni.es10cDeleteProfile(contextHandle, iccid) == 0

    @Synchronized
    override fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                                 confirmationCode: String?, callback: ProfileDownloadCallback) {
        val res = LpacJni.downloadProfile(
            contextHandle,
            smdp,
            matchingId,
            imei,
            confirmationCode,
            callback
        )

        if (res != 0) {
            throw LocalProfileAssistant.ProfileDownloadException(
                httpInterface.lastHttpResponse,
                httpInterface.lastHttpException
            )
        }
    }

    @Synchronized
    override fun deleteNotification(seqNumber: Long): Boolean =
        LpacJni.es10bDeleteNotification(contextHandle, seqNumber) == 0

    @Synchronized
    override fun handleNotification(seqNumber: Long): Boolean =
        LpacJni.handleNotification(contextHandle, seqNumber).also {
            Log.d(TAG, "handleNotification $seqNumber = $it")
        } == 0

    @Synchronized
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