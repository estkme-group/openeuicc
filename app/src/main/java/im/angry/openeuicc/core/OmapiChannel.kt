package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.util.Log
import com.truphone.lpa.LocalProfileAssistant
import com.truphone.lpa.impl.LocalProfileAssistantImpl
import java.lang.Exception

class OmapiChannel private constructor(
    info: EuiccChannelInfo,
    private val channel: Channel
) : EuiccChannel(info) {
    companion object {
        private const val TAG = "OmapiChannel"
        private val APPLET_ID = byteArrayOf(-96, 0, 0, 5, 89, 16, 16, -1, -1, -1, -1, -119, 0, 0, 1, 0)

        fun tryConnect(service: SEService, info: EuiccChannelInfo): OmapiChannel? {
            try {
                val reader = service.getUiccReader(info.slotId + 1) // slotId from telephony starts from 0
                val session = reader.openSession()
                val channel = session.openLogicalChannel(APPLET_ID) ?: return null
                return OmapiChannel(info, channel)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open eUICC channel for slot ${info.slotId}, skipping")
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
        }
    }

    override val lpa: LocalProfileAssistant by lazy {
        LocalProfileAssistantImpl(OmapiApduChannel(channel))
    }
    override val valid: Boolean
        get() = channel.isOpen // TODO: This has to be implemented properly

    override fun close() = channel.close()
}
