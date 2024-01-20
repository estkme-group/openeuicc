package im.angry.openeuicc.util

import android.content.Context
import android.content.pm.PackageManager
import net.typeblog.lpac_jni.LocalProfileInfo
import java.lang.RuntimeException

val Context.selfAppVersion: String
    get() =
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }

val LocalProfileInfo.isEnabled: Boolean
    get() = state == LocalProfileInfo.State.Enabled