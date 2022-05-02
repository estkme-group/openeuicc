package im.angry.openeuicc.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.util.Log
import com.truphone.lpa.ApduChannel
import com.truphone.lpa.impl.LocalProfileAssistantImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EuiccChannelManager(private val context: Context) {
    companion object {
        const val TAG = "EuiccChannelManager"
        const val MAX_SIMS = 3
    }

    private val channels = mutableListOf<EuiccChannel>()

    private var seService: SEService? = null

    private val handler = Handler(HandlerThread("EuiccChannelManager").also { it.start() }.looper)

    private suspend fun connectSEService(): SEService = suspendCoroutine { cont ->
        var service: SEService? = null
        service = SEService(context, { handler.post(it) }) {
            cont.resume(service!!)
        }
    }

    private suspend fun ensureSEService() {
         if (seService == null) {
             seService = connectSEService()
         }
    }

    private suspend fun findEuiccChannelBySlot(slotId: Int): EuiccChannel? {
        ensureSEService()
        val existing = channels.find { it.slotId == slotId }
        if (existing != null) {
            if (existing.stateManager.valid) {
                return existing
            } else {
                existing.stateManager.destroy()
                channels.remove(existing)
            }
        }

        var apduChannel: ApduChannel? = null
        var stateManager: EuiccChannelStateManager? = null
        OmapiApduChannel.tryConnectUiccSlot(seService!!, slotId)?.let { (_apduChannel, _stateManager) ->
            apduChannel = _apduChannel
            stateManager = _stateManager
        } ?: return null

        val channel = EuiccChannel(slotId, "SIM $slotId", LocalProfileAssistantImpl(apduChannel), stateManager!!)
        channels.add(channel)
        return channel
    }

    fun findEuiccChannelBySlotBlocking(slotId: Int): EuiccChannel? = runBlocking {
        withContext(Dispatchers.IO) {
            findEuiccChannelBySlot(slotId)
        }
    }

    suspend fun enumerateEuiccChannels() {
        withContext(Dispatchers.IO) {
            ensureSEService()

            for (slotId in 0 until MAX_SIMS) {
                if (findEuiccChannelBySlot(slotId) != null) {
                    Log.d(TAG, "Found eUICC on slot $slotId")
                }
            }
        }
    }

    val knownChannels: List<EuiccChannel>
        get() = channels.toList()

    fun invalidate() {
        for (channel in channels) {
            channel.stateManager.destroy()
        }

        channels.clear()
        seService?.shutdown()
        seService = null
    }
}