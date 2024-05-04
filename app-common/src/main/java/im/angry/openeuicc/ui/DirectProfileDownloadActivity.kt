package im.angry.openeuicc.ui

import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectProfileDownloadActivity : BaseEuiccAccessActivity(), SlotSelectFragment.SlotSelectedListener, OpenEuiccContextMarker {
    override fun onInit() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                euiccChannelManager.enumerateEuiccChannels()
            }

            when {
                euiccChannelManager.knownChannels.isEmpty() -> {
                    finish()
                }
                euiccChannelManager.knownChannels.hasMultipleChips -> {
                    SlotSelectFragment.newInstance()
                        .show(supportFragmentManager, SlotSelectFragment.TAG)
                }
                else -> {
                    // If the device has only one eSIM "chip" (but may be mapped to multiple slots),
                    // we can skip the slot selection dialog since there is only one chip to save to.
                    onSlotSelected(euiccChannelManager.knownChannels[0].slotId,
                        euiccChannelManager.knownChannels[0].portId)
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