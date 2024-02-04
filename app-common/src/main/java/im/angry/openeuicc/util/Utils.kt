package im.angry.openeuicc.util

import android.content.Context
import android.content.pm.PackageManager
import android.se.omapi.SEService
import im.angry.openeuicc.core.EuiccChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.typeblog.lpac_jni.LocalProfileInfo
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

val List<EuiccChannel>.hasMultipleChips: Boolean
    get() = distinctBy { it.slotId }.size > 1

// Create an instance of OMAPI SEService in a manner that "makes sense" without unpredictable callbacks
suspend fun connectSEService(context: Context): SEService = suspendCoroutine { cont ->
    // Use a Mutex to make sure the continuation is run *after* the "service" variable is assigned
    val lock = Mutex()
    var service: SEService? = null
    val callback = {
        runBlocking {
            lock.withLock {
                cont.resume(service!!)
            }
        }
    }

    runBlocking {
        // If this were not protected by a Mutex, callback might be run before service is even assigned
        // Yes, we are on Android, we could have used something like a Handler, but we cannot really
        // assume the coroutine is run on a thread that has a Handler. We either use our own HandlerThread
        // (and then cleanup becomes an issue), or we use a lock
        lock.withLock {
            try {
                service = SEService(context, { it.run() }, callback)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }
}