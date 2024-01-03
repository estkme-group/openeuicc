package im.angry.openeuicc

import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager

class PrivilegedOpenEuiccApplication: OpenEuiccApplication() {
    override val euiccChannelManager: EuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        (euiccChannelManager as PrivilegedEuiccChannelManager).closeAllStaleChannels()
    }
}