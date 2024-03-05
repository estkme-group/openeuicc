package im.angry.openeuicc.di

import android.content.Context
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager

class PrivilegedAppContainer(context: Context) : DefaultAppContainer(context) {
    override val euiccChannelManager: EuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(context)
    }

    override val uiComponentFactory by lazy {
        PrivilegedUiComponentFactory()
    }
}