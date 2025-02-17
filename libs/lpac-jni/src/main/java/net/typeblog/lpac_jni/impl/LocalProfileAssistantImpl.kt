package net.typeblog.lpac_jni.impl

import android.util.Log
import net.typeblog.lpac_jni.LpacJni
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.HttpInterface.HttpResponse
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileDownloadCallback

class LocalProfileAssistantImpl(
    isdrAid: ByteArray,
    rawApduInterface: ApduInterface,
    rawHttpInterface: HttpInterface
): LocalProfileAssistant {
    companion object {
        private const val TAG = "LocalProfileAssistantImpl"
    }

    /**
     * A thin wrapper over ApduInterface to acquire exceptions and errors transparently
     */
    private class ApduInterfaceWrapper(val apduInterface: ApduInterface) :
        ApduInterface by apduInterface {
        var lastApduResponse: ByteArray? = null
        var lastApduException: Exception? = null

        override fun transmit(tx: ByteArray): ByteArray =
            try {
                apduInterface.transmit(tx).also {
                    lastApduException = null
                    lastApduResponse = it
                }
            } catch (e: Exception) {
                lastApduResponse = null
                lastApduException = e
                throw e
            }
    }

    /**
     * Same for HTTP for diagnostics
     */
    private class HttpInterfaceWrapper(val httpInterface: HttpInterface) :
        HttpInterface by httpInterface {
        /**
         * The last HTTP response we have received from the SM-DP+ server.
         *
         * This is intended for error diagnosis. However, note that most SM-DP+ servers
         * respond with 200 even when there is an error. This needs to be taken into
         * account when designing UI.
         */
        var lastHttpResponse: HttpResponse? = null

        /**
         * The last exception that has been thrown during a HTTP connection
         */
        var lastHttpException: Exception? = null

        override fun transmit(url: String, tx: ByteArray, headers: Array<String>): HttpResponse =
            try {
                httpInterface.transmit(url, tx, headers).also {
                    lastHttpException = null
                    lastHttpResponse = it
                }
            } catch (e: Exception) {
                lastHttpResponse = null
                lastHttpException = e
                throw e
            }
    }

    private val apduInterface = ApduInterfaceWrapper(rawApduInterface)
    private val httpInterface = HttpInterfaceWrapper(rawHttpInterface)

    private var finalized = false
    private var contextHandle: Long = LpacJni.createContext(isdrAid, apduInterface, httpInterface)

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
                LpacJni.euiccInfo2GetSGP22Version(cInfo),
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
            // Construct the error now to store any error information we _can_ access
            val err = LocalProfileAssistant.ProfileDownloadException(
                lpaErrorReason = LpacJni.downloadErrCodeToString(-res),
                httpInterface.lastHttpResponse,
                httpInterface.lastHttpException,
                apduInterface.lastApduResponse,
                apduInterface.lastApduException,
            )

            // Cancel sessions if possible. This will overwrite recorded errors from HTTP and APDU interfaces.
            LpacJni.cancelSessions(contextHandle)

            throw err
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
    override fun setNickname(iccid: String, nickname: String) {
        val encoded = try {
            Charsets.UTF_8.encode(nickname).array()
        } catch (e: CharacterCodingException) {
            throw LocalProfileAssistant.ProfileNameIsInvalidUTF8Exception()
        }

        if (encoded.size >= 64) {
            throw LocalProfileAssistant.ProfileNameTooLongException()
        }

        val encodedNullTerminated = encoded + byteArrayOf(0)

        if (LpacJni.es10cSetNickname(contextHandle, iccid, encodedNullTerminated) != 0) {
            throw LocalProfileAssistant.ProfileRenameException()
        }
    }

    override fun euiccMemoryReset() {
        LpacJni.es10cEuiccMemoryReset(contextHandle)
    }

    @Synchronized
    override fun close() {
        if (!finalized) {
            LpacJni.euiccFini(contextHandle)
            LpacJni.destroyContext(contextHandle)
            finalized = true
        }
    }
}