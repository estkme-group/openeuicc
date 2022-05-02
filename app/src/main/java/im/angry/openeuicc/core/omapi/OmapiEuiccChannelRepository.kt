package im.angry.openeuicc.core.omapi

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.util.Log
import com.truphone.lpa.impl.LocalProfileAssistantImpl
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelRepository
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OmapiEuiccChannelRepository(private val context: Context) : EuiccChannelRepository {
    companion object {
        const val TAG = "OmapiEuiccChannelRepository"
        val APPLET_ID = byteArrayOf(-96, 0, 0, 5, 89, 16, 16, -1, -1, -1, -1, -119, 0, 0, 1, 0)
    }

    private val handler = Handler(HandlerThread("OMAPI").also { it.start() }.looper)

    private val channels = mutableListOf<EuiccChannel>()

    private suspend fun connectSEService(): SEService = suspendCoroutine { cont ->
        var service: SEService? = null
        service = SEService(context, { handler.post(it) }) {
            cont.resume(service!!)
        }
    }

    private fun tryConnectSlot(service: SEService, slotId: Int): EuiccChannel? {
        try {
            val reader = service.getUiccReader(slotId)
            val session = reader.openSession()
            val channel = session.openLogicalChannel(APPLET_ID) ?: return null
            val apduChannel = OmapiApduChannel(channel)
            val lpa = LocalProfileAssistantImpl(apduChannel)

            return EuiccChannel(slotId, reader.name, lpa)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open eUICC channel for slot ${slotId}, skipping")
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    override suspend fun load() {
        channels.clear()
        val service = connectSEService()

        for (slotId in 1..3) {
            tryConnectSlot(service, slotId)?.let {
                Log.d(TAG, "New eUICC eSE channel: ${it.name}")
                channels.add(it)
            }
        }
    }

    override val availableChannels: List<EuiccChannel>
        get() = channels
}