package im.angry.openeuicc

import im.angry.openeuicc.core.IEuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager

class PrivilegedOpenEuiccApplication: OpenEuiccApplication() {
    override val euiccChannelManager: IEuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        (euiccChannelManager as PrivilegedEuiccChannelManager).closeAllStaleChannels()
    }
}