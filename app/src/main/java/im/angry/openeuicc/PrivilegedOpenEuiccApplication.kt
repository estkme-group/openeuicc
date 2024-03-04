package im.angry.openeuicc

import im.angry.openeuicc.core.PrivilegedEuiccChannelManager
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.di.PrivilegedAppContainer

class PrivilegedOpenEuiccApplication: OpenEuiccApplication() {
    override val appContainer: AppContainer by lazy {
        PrivilegedAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()

        (appContainer.euiccChannelManager as PrivilegedEuiccChannelManager).closeAllStaleChannels()
    }
}