package im.angry.openeuicc

import android.app.Application
import im.angry.openeuicc.core.EuiccChannelRepositoryProxy

class OpenEUICCApplication : Application() {
    val euiccChannelRepo = EuiccChannelRepositoryProxy(this)
}