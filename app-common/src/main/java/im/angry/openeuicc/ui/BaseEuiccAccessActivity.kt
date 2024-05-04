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

abstract class BaseEuiccAccessActivity : AppCompatActivity() {
    lateinit var euiccChannelManager: EuiccChannelManager

    private val euiccChannelManagerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            euiccChannelManager =
                (service!! as EuiccChannelManagerService.LocalBinder).service.euiccChannelManager
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