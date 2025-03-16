package im.angry.openeuicc.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.Fragment
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface PrivilegedEuiccContextMarker : OpenEuiccContextMarker {
    override val preferenceRepository: PrivilegedPreferenceRepository
        get() = appContainer.preferenceRepository as PrivilegedPreferenceRepository
}

suspend fun Context.bindServiceSuspended(intent: Intent, flags: Int): Pair<IBinder?, () -> Unit> =
    suspendCoroutine { cont ->
        var binder: IBinder?
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder = service
                cont.resume(Pair(binder) { unbindService(this) })
            }

            override fun onServiceDisconnected(name: ComponentName?) {

            }
        }

        bindService(intent, flags, Executors.newSingleThreadExecutor(), conn)
    }