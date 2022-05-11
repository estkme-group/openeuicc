package im.angry.openeuicc.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
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

    private val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).telephonyManager
    }

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
            if (existing.valid) {
                return existing
            } else {
                existing.close()
                channels.remove(existing)
            }
        }

        val cardInfo = tm.uiccCardsInfo.find { it.slotIndex == slotId } ?: return null

        val channelInfo = EuiccChannelInfo(
            slotId, cardInfo.cardId, "SIM $slotId", cardInfo.isRemovable
        )

        val (shouldTryTelephonyManager, cardId) =
            cardInfo.let {
                Pair(it.isEuicc && !it.isRemovable, it.cardId)
            }

        var euiccChannel: EuiccChannel? = null

        if (shouldTryTelephonyManager) {
            Log.d(TAG, "Using TelephonyManager for slot $slotId")
            // TODO: On Tiramisu, we should also connect all available "ports" for MEP support
            euiccChannel = TelephonyManagerChannel.tryConnect(tm, channelInfo)
        }

        if (euiccChannel == null) {
            euiccChannel = OmapiChannel.tryConnect(seService!!, channelInfo)
        }

        if (euiccChannel != null) {
            channels.add(euiccChannel)
        }

        return euiccChannel
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
            channel.close()
        }

        channels.clear()
        seService?.shutdown()
        seService = null
    }
}