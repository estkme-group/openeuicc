package im.angry.openeuicc.util

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import net.typeblog.lpac_jni.LocalProfileInfo
import java.lang.Exception

val TelephonyManager.supportsDSDS: Boolean
    get() = supportedModemCount == 2

var TelephonyManager.dsdsEnabled: Boolean
    get() = activeModemCount >= 2
    set(value) {
        switchMultiSimConfig(if (value) { 2 } else {1})
    }

fun SubscriptionManager.tryRefreshCachedEuiccInfo(cardId: Int) {
    if (cardId != 0) {
        try {
            requestEmbeddedSubscriptionInfoListRefresh(cardId)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

val LocalProfileInfo.displayName: String
    get() = nickName.ifEmpty { name }

val List<LocalProfileInfo>.operational: List<LocalProfileInfo>
    get() = filter {
        // TODO: Profiles get marked as PROVISIONING with lpac even if it is OPERATIONAL. Why?
        it.profileClass != LocalProfileInfo.Clazz.Testing
    }