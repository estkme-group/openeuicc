package im.angry.openeuicc.util

import im.angry.openeuicc.core.EuiccChannel
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo

val LocalProfileInfo.displayName: String
    get() = nickName.ifEmpty { name }


val LocalProfileInfo.isEnabled: Boolean
    get() = state == LocalProfileInfo.State.Enabled

val List<LocalProfileInfo>.operational: List<LocalProfileInfo>
    get() = filter {
        it.profileClass == LocalProfileInfo.Clazz.Operational
    }

val List<EuiccChannel>.hasMultipleChips: Boolean
    get() = distinctBy { it.slotId }.size > 1

fun LocalProfileAssistant.disableActiveProfileWithUndo(): () -> Unit =
    profiles.find { it.state == LocalProfileInfo.State.Enabled }?.let {
        disableProfile(it.iccid)
        return { enableProfile(it.iccid) }
    } ?: { }