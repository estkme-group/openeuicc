package im.angry.openeuicc.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.telephony.UiccCardInfo
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class EuiccChannelManager(protected val context: Context) {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channels = mutableListOf<EuiccChannel>()

    private var seService: SEService? = null

    private val lock = Mutex()

    protected val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).telephonyManager
    }

    private val handler = Handler(HandlerThread("BaseEuiccChannelManager").also { it.start() }.looper)

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

    protected open fun tryOpenEuiccChannelPrivileged(uiccInfo: UiccCardInfo, channelInfo: EuiccChannelInfo): EuiccChannel? {
        // No-op when unprivileged
        return null
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

            var euiccChannel: EuiccChannel? = tryOpenEuiccChannelPrivileged(uiccInfo, channelInfo)

            if (euiccChannel == null) {
                try {
                    euiccChannel = OmapiChannel(seService!!, channelInfo)
                } catch (e: IllegalArgumentException) {
                    // Failed
                }
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

    open fun notifyEuiccProfilesChanged(slotId: Int) {
        // No-op for unprivileged
    }
}