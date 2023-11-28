package im.angry.openeuicc

import im.angry.openeuicc.core.BaseEuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager

class PrivilegedOpenEuiccApplication: BaseOpenEuiccApplication() {
    override val euiccChannelManager: BaseEuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        (euiccChannelManager as PrivilegedEuiccChannelManager).closeAllStaleChannels()
    }
}