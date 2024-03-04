package im.angry.openeuicc.di

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.IEuiccChannelManager
import im.angry.openeuicc.util.*

interface AppContainer {
    val telephonyManager: TelephonyManager
    val euiccChannelManager: IEuiccChannelManager
    val subscriptionManager: SubscriptionManager
    val preferenceRepository: PreferenceRepository
    val uiComponentFactory: UiComponentFactory
}