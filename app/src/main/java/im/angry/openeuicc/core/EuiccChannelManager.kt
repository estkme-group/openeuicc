package im.angry.openeuicc.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.telephony.UiccCardInfo
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EuiccChannelManager(private val context: Context) {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channels = mutableListOf<EuiccChannel>()

    private var seService: SEService? = null

    private val lock = Mutex()

    private val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).telephonyManager
    }

    private val handler = Handler(HandlerThread("EuiccChannelManager").also { it.start() }.looper)

    private suspend fun connectSEService(): SEService = suspendCoroutine { cont ->
        handler.post {
            var service: SEService? = null
            service = SEService(context, { handler.post(it) }) {
                cont.resume(service!!)
            }
        }
    }

    private suspend fun ensureSEService() {
         if (seService == null) {
             seService = connectSEService()
         }
    }

    private suspend fun tryOpenEuiccChannel(uiccInfo: UiccCardInfo): EuiccChannel? {
        lock.withLock {
            ensureSEService()
            val existing = channels.find { it.slotId == uiccInfo.slotIndex }
            if (existing != null) {
                if (existing.valid) {
                    return existing
                } else {
                    existing.close()
                    channels.remove(existing)
                }
            }

            val channelInfo = EuiccChannelInfo(
                uiccInfo.slotIndex,
                uiccInfo.cardId,
                "SIM ${uiccInfo.slotIndex}",
                tm.getImei(uiccInfo.slotIndex) ?: return null,
                uiccInfo.isRemovable
            )

            var euiccChannel: EuiccChannel? = null

            if (uiccInfo.isEuicc && !uiccInfo.isRemovable) {
                Log.d(TAG, "Using TelephonyManager for slot ${uiccInfo.slotIndex}")
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
    }

    private suspend fun findEuiccChannelBySlot(slotId: Int): EuiccChannel? {
        return tm.uiccCardsInfo.find { it.slotIndex == slotId }?.let {
            tryOpenEuiccChannel(it)
        }
    }

    fun findEuiccChannelBySlotBlocking(slotId: Int): EuiccChannel? = runBlocking {
        withContext(Dispatchers.IO) {
            findEuiccChannelBySlot(slotId)
        }
    }

    suspend fun enumerateEuiccChannels() {
        withContext(Dispatchers.IO) {
            ensureSEService()

            for (uiccInfo in tm.uiccCardsInfo) {
                if (tryOpenEuiccChannel(uiccInfo) != null) {
                    Log.d(TAG, "Found eUICC on slot ${uiccInfo.slotIndex}")
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

    // Clean up channels left open in TelephonyManager
    // due to a (potentially) forced restart
    // This should be called every time the application is restarted
    fun closeAllStaleChannels() {
        for (card in tm.uiccCardsInfo) {
            for (channel in 0 until 10) {
                try {
                    tm.iccCloseLogicalChannelBySlot(card.slotIndex, channel)
                } catch (_: Exception) {
                    // We do not care
                }
            }
        }
    }
}