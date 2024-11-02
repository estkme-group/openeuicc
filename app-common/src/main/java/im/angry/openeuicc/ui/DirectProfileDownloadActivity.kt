package im.angry.openeuicc.ui

import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectProfileDownloadActivity : BaseEuiccAccessActivity(), SlotSelectFragment.SlotSelectedListener, OpenEuiccContextMarker {
    override fun onInit() {
        lifecycleScope.launch {
            val knownChannels = withContext(Dispatchers.IO) {
                euiccChannelManager.flowEuiccPorts().map { (slotId, portId) ->
                    euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                        Triple(slotId, channel.logicalSlotId, portId)
                    }
                }.toList().sortedBy { it.second }
            }

            when {
                knownChannels.isEmpty() -> {
                    finish()
                }
                // Detect multiple eUICC chips
                knownChannels.distinctBy { it.first }.size > 1 -> {
                    SlotSelectFragment.newInstance(
                        knownChannels.map { it.first },
                        knownChannels.map { it.second },
                        knownChannels.map { it.third })
                        .show(supportFragmentManager, SlotSelectFragment.TAG)
                }
                else -> {
                    // If the device has only one eSIM "chip" (but may be mapped to multiple slots),
                    // we can skip the slot selection dialog since there is only one chip to save to.
                    onSlotSelected(
                        knownChannels[0].first,
                        knownChannels[0].third
                    )
                }
            }
        }
    }

    override fun onSlotSelected(slotId: Int, portId: Int) {
        ProfileDownloadFragment.newInstance(slotId, portId, finishWhenDone = true)
            .show(supportFragmentManager, ProfileDownloadFragment.TAG)
    }

    override fun onSlotSelectCancelled() = finish()
}