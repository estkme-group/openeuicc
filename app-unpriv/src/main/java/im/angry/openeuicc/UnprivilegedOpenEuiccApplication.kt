package im.angry.openeuicc

import android.content.Intent
import im.angry.openeuicc.ui.LogsActivity
import im.angry.openeuicc.util.*
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

class UnprivilegedOpenEuiccApplication : OpenEuiccApplication() {
    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            Intent(this, LogsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("log", runBlocking { readSelfLog() })
                startActivity(this)
                exitProcess(-1)
            }
        }
    }
}