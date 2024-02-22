package net.typeblog.lpac_jni.impl

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    private var contextHandle: Long = LpacJni.createContext(apduInterface, httpInterface)
    init {
        if (LpacJni.es10xInit(contextHandle) < 0) {
            throw IllegalArgumentException("Failed to initialize LPA")
        }

        val pkids = euiccInfo2?.euiccCiPKIdListForVerification ?: arrayOf()
        httpInterface.usePublicKeyIds(pkids)
    }

    private fun tryReconnect(timeoutMillis: Long) = runBlocking {
        withTimeout(timeoutMillis) {
            try {
                LpacJni.es10xFini(contextHandle)
                LpacJni.destroyContext(contextHandle)
                contextHandle = -1
            } catch (e: Exception) {
                // Ignored
            }

            while (true) {
                try {
                    apduInterface.disconnect()
                } catch (e: Exception) {
                    // Ignored
                }

                try {
                    apduInterface.connect()
                    contextHandle = LpacJni.createContext(apduInterface, httpInterface)
                    check(LpacJni.es10xInit(contextHandle) >= 0) { "Reconnect attempt failed" }
                    // Validate that we can actually use the APDU channel by trying to read eID and profiles
                    check(valid) { "Reconnected channel is invalid" }
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (contextHandle != -1L) {
                        try {
                            LpacJni.es10xFini(contextHandle)
                            LpacJni.destroyContext(contextHandle)
                            contextHandle = -1
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                    // continue retrying
                    delay(1000)
                }
            }
        }
    }

    override val profiles: List<LocalProfileInfo>
        get() = LpacJni.es10cGetProfilesInfo(contextHandle)!!.asList()

    override val notifications: List<LocalProfileNotification>
        get() =
            (LpacJni.es10bListNotification(contextHandle) ?: arrayOf())
                .sortedBy { it.seqNumber }.reversed()

    override val eID: String
        get() = LpacJni.es10cGetEid(contextHandle)!!

    override val euiccInfo2: EuiccInfo2?
        get() = LpacJni.es10cexGetEuiccInfo2(contextHandle)

    override fun enableProfile(iccid: String, reconnectTimeout: Long): Boolean {
        val res = LpacJni.es10cEnableProfile(contextHandle, iccid) == 0
        if (reconnectTimeout > 0) {
            try {
                tryReconnect(reconnectTimeout)
            } catch (e: Exception) {
                return false
            }
        }
        return res
    }

    override fun disableProfile(iccid: String, reconnectTimeout: Long): Boolean {
        val res = LpacJni.es10cDisableProfile(contextHandle, iccid) == 0
        if (reconnectTimeout > 0) {
            try {
                tryReconnect(reconnectTimeout)
            } catch (e: Exception) {
                return false
            }
        }
        return res
    }

    override fun deleteProfile(iccid: String): Boolean {
        return LpacJni.es10cDeleteProfile(contextHandle, iccid) == 0
    }

    override fun downloadProfile(smdp: String, matchingId: String?, imei: String?,
                                 confirmationCode: String?, callback: ProfileDownloadCallback): Boolean {
        return LpacJni.downloadProfile(contextHandle, smdp, matchingId, imei, confirmationCode, callback) == 0
    }

    override fun deleteNotification(seqNumber: Long): Boolean =
        LpacJni.es10bDeleteNotification(contextHandle, seqNumber) == 0

    override fun handleNotification(seqNumber: Long): Boolean =
        LpacJni.handleNotification(contextHandle, seqNumber).also {
            Log.d(TAG, "handleNotification $seqNumber = $it")
        } == 0

    override fun setNickname(iccid: String, nickname: String): Boolean {
        return LpacJni.es10cSetNickname(contextHandle, iccid, nickname) == 0
    }

    override fun close() {
        LpacJni.es10xFini(contextHandle)
        LpacJni.destroyContext(contextHandle)
    }
}