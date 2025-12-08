package im.angry.openeuicc.ui

import android.content.Intent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import im.angry.openeuicc.R
import im.angry.openeuicc.ui.wizard.DownloadWizardActivity

class LuiActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        enableEdgeToEdge()
        setContentView(R.layout.activity_lui)

        ViewCompat.setOnApplyWindowInsetsListener(requireViewById(R.id.lui_container)) { v, insets ->
            val bars = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        requireViewById<View>(R.id.lui_skip).setOnClickListener { finish() }
        // TODO: Deactivate DownloadWizardActivity if there is no eSIM found.
        // TODO: Support pre-filled download info (from carrier apps); UX
        requireViewById<View>(R.id.lui_download).setOnClickListener {
            startActivity(Intent(this, DownloadWizardActivity::class.java))
            finish()
        }
    }
}
