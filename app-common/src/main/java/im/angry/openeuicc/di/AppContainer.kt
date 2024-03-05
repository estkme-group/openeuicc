package im.angry.openeuicc.di

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.EuiccChannelFactory
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*

interface AppContainer {
    val telephonyManager: TelephonyManager
    val euiccChannelManager: EuiccChannelManager
    val subscriptionManager: SubscriptionManager
    val preferenceRepository: PreferenceRepository
    val uiComponentFactory: UiComponentFactory
    val euiccChannelFactory: EuiccChannelFactory
}