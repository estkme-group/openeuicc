package im.angry.openeuicc.core

import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileDownloadCallback

class LocalProfileAssistantWrapper(orig: LocalProfileAssistant) :
    LocalProfileAssistant {
    private var _inner: LocalProfileAssistant? = orig

    private val lpa: LocalProfileAssistant
        get() {
            if (_inner == null) {
                throw IllegalStateException("This wrapper has been invalidated")
            }

            return _inner!!
        }

    override val lastHttpResponse: HttpInterface.HttpResponse?
        get() = lpa.lastHttpResponse

    override val valid: Boolean
        get() = lpa.valid
    override val profiles: List<LocalProfileInfo>
        get() = lpa.profiles
    override val notifications: List<LocalProfileNotification>
        get() = lpa.notifications
    override val eID: String
        get() = lpa.eID
    override val euiccInfo2: EuiccInfo2?
        get() = lpa.euiccInfo2

    override fun setEs10xMss(mss: Byte) = lpa.setEs10xMss(mss)

    override fun enableProfile(iccid: String, refresh: Boolean): Boolean =
        lpa.enableProfile(iccid, refresh)

    override fun disableProfile(iccid: String, refresh: Boolean): Boolean =
        lpa.disableProfile(iccid, refresh)

    override fun deleteProfile(iccid: String): Boolean = lpa.deleteProfile(iccid)

    override fun downloadProfile(
        smdp: String,
        matchingId: String?,
        imei: String?,
        confirmationCode: String?,
        callback: ProfileDownloadCallback
    ): Boolean = lpa.downloadProfile(smdp, matchingId, imei, confirmationCode, callback)

    override fun deleteNotification(seqNumber: Long): Boolean = lpa.deleteNotification(seqNumber)

    override fun handleNotification(seqNumber: Long): Boolean = lpa.handleNotification(seqNumber)

    override fun setNickname(iccid: String, nickname: String): Boolean =
        lpa.setNickname(iccid, nickname)

    override fun close() = lpa.close()

    fun invalidateWrapper() {
        _inner = null
    }
}