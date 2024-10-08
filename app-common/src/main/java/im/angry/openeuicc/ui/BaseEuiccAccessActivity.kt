package im.angry.openeuicc.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.service.EuiccChannelManagerService
import kotlinx.coroutines.CompletableDeferred

abstract class BaseEuiccAccessActivity : AppCompatActivity() {
    val euiccChannelManagerLoaded = CompletableDeferred<Unit>()
    lateinit var euiccChannelManager: EuiccChannelManager
    lateinit var euiccChannelManagerService: EuiccChannelManagerService

    private val euiccChannelManagerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            euiccChannelManagerService = (service!! as EuiccChannelManagerService.LocalBinder).service
            euiccChannelManager = euiccChannelManagerService.euiccChannelManager
            euiccChannelManagerLoaded.complete(Unit)
            onInit()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // These activities should never lose the EuiccChannelManagerService connection
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindService(
            Intent(this, EuiccChannelManagerService::class.java),
            euiccChannelManagerServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(euiccChannelManagerServiceConnection)
    }

    /**
     * When called, euiccChannelManager is guaranteed to have been initialized
     */
    abstract fun onInit()
}