package im.angry.openeuicc.ui.wizard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.angry.openeuicc.common.R

class DownloadWizardLowPowerFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    companion object {
        /** Battery level threshold (in percentage) below which the battery is considered low. */
        private const val BATTERY_LEVEL_THRESHOLD = 20

        fun isBatteryLow(context: Context): Boolean {
            val pm = context.getSystemService(PowerManager::class.java)
            if (pm.isPowerSaveMode) return true
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter) ?: return false
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            return level <= BATTERY_LEVEL_THRESHOLD
        }
    }

    override val hasNext: Boolean
        get() = true

    override val hasPrev: Boolean
        get() = true

    override fun createNextFragment() = DownloadWizardSlotSelectFragment()

    override fun createPrevFragment() = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_download_low_power, container, /* attachToRoot = */ false)
}
