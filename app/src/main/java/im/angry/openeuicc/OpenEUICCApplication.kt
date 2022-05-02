package im.angry.openeuicc

import android.app.Application
import im.angry.openeuicc.core.EuiccChannelManager

class OpenEUICCApplication : Application() {
    val euiccChannelManager = EuiccChannelManager(this)
}