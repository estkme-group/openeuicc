package im.angry.openeuicc.util

import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo

val LocalProfileInfo.displayName: String
    get() = nickName.ifEmpty { name }

val List<LocalProfileInfo>.operational: List<LocalProfileInfo>
    get() = filter {
        it.profileClass == LocalProfileInfo.Clazz.Operational
    }

fun LocalProfileAssistant.disableActiveProfileWithUndo(): () -> Unit =
    profiles.find { it.state == LocalProfileInfo.State.Enabled }?.let {
        disableProfile(it.iccid)
        return { enableProfile(it.iccid) }
    } ?: { }