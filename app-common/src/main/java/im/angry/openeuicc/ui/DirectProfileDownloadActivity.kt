package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.util.openEuiccApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectProfileDownloadActivity : AppCompatActivity(), SlotSelectFragment.SlotSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                openEuiccApplication.euiccChannelManager.enumerateEuiccChannels()
            }

            SlotSelectFragment.newInstance()
                .show(supportFragmentManager, SlotSelectFragment.TAG)
        }
    }

    override fun onSlotSelected(slotId: Int, portId: Int) {
        ProfileDownloadFragment.newInstance(slotId, portId, finishWhenDone = true)
            .show(supportFragmentManager, ProfileDownloadFragment.TAG)
    }

    override fun onSlotSelectCancelled() = finish()
}