package im.angry.openeuicc

import com.google.android.material.color.DynamicColors
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager

class PrivilegedOpenEuiccApplication: OpenEuiccApplication() {
    override val euiccChannelManager: EuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)

        (euiccChannelManager as PrivilegedEuiccChannelManager).closeAllStaleChannels()
    }
}