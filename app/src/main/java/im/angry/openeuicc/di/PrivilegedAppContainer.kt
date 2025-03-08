package im.angry.openeuicc.di

import android.content.Context
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.EuiccChannelManagerFactory
import im.angry.openeuicc.core.PrivilegedEuiccChannelFactory
import im.angry.openeuicc.core.PrivilegedEuiccChannelManager
import im.angry.openeuicc.core.PrivilegedEuiccChannelManagerFactory
import im.angry.openeuicc.util.*

class PrivilegedAppContainer(context: Context) : DefaultAppContainer(context) {
    override val euiccChannelManager: EuiccChannelManager by lazy {
        PrivilegedEuiccChannelManager(this, context)
    }

    override val euiccChannelManagerFactory: EuiccChannelManagerFactory by lazy {
        PrivilegedEuiccChannelManagerFactory(this)
    }

    override val uiComponentFactory by lazy {
        PrivilegedUiComponentFactory()
    }

    override val euiccChannelFactory by lazy {
        PrivilegedEuiccChannelFactory(context)
    }

    override val customizableTextProvider by lazy {
        PrivilegedCustomizableTextProvider(context)
    }

    override val preferenceRepository by lazy {
        PrivilegedPreferenceRepository(context)
    }
}