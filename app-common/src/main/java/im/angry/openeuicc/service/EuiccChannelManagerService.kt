package im.angry.openeuicc.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*

/**
 * An Android Service wrapper for EuiccChannelManager.
 * The purpose of this wrapper is mainly lifecycle-wise: having a Service allows the manager
 * instance to have its own independent lifecycle. This way it can be created as requested and
 * destroyed when no other components are bound to this service anymore.
 * This behavior allows us to avoid keeping the APDU channels open at all times. For example,
 * the EuiccService implementation should *only* bind to this service when it requires an
 * instance of EuiccChannelManager. UI components can keep being bound to this service for
 * their entire lifecycles, since the whole purpose of them is to expose the current state
 * to the user.
 */
class EuiccChannelManagerService : Service(), OpenEuiccContextMarker {
    inner class LocalBinder : Binder() {
        val service = this@EuiccChannelManagerService
    }

    private val euiccChannelManagerDelegate = lazy {
        appContainer.euiccChannelManagerFactory.createEuiccChannelManager(this)
    }
    val euiccChannelManager: EuiccChannelManager by euiccChannelManagerDelegate

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        super.onDestroy()
        // This is the whole reason of the existence of this service:
        // we can clean up opened channels when no one is using them
        if (euiccChannelManagerDelegate.isInitialized()) {
            euiccChannelManager.invalidate()
        }
    }
}