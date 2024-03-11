package im.angry.openeuicc.ui

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import im.angry.openeuicc.R

class LuiActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        setContentView(R.layout.activity_lui)

        requireViewById<View>(R.id.lui_skip).setOnClickListener { finish() }
        // TODO: Deactivate LuiActivity if there is no eSIM found.
        // TODO: Support pre-filled download info (from carrier apps); UX
        requireViewById<View>(R.id.lui_download).setOnClickListener {
            startActivity(Intent(this, DirectProfileDownloadActivity::class.java))
        }
    }
}