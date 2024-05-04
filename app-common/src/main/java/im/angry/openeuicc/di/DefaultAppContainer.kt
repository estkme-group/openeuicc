package im.angry.openeuicc.di

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.DefaultEuiccChannelFactory
import im.angry.openeuicc.core.DefaultEuiccChannelManager
import im.angry.openeuicc.core.DefaultEuiccChannelManagerFactory
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.EuiccChannelManagerFactory
import im.angry.openeuicc.util.*

open class DefaultAppContainer(context: Context) : AppContainer {
    override val telephonyManager by lazy {
        context.getSystemService(TelephonyManager::class.java)!!
    }

    override val euiccChannelManager: EuiccChannelManager by lazy {
        DefaultEuiccChannelManager(this, context)
    }

    override val euiccChannelManagerFactory: EuiccChannelManagerFactory by lazy {
        DefaultEuiccChannelManagerFactory(this)
    }

    override val subscriptionManager by lazy {
        context.getSystemService(SubscriptionManager::class.java)!!
    }

    override val preferenceRepository by lazy {
        PreferenceRepository(context)
    }

    override val uiComponentFactory by lazy {
        DefaultUiComponentFactory()
    }

    override val euiccChannelFactory by lazy {
        DefaultEuiccChannelFactory(context)
    }
}