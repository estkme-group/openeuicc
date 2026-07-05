package im.angry.openeuicc.ui.wizard


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*

class DeepLinkConfirmationActivity : AppCompatActivity() {
    private val dialog by lazy {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(R.string.deep_link_confirmation_dialog_title)
            .setCancelable(false)
            .setMessage(R.string.deep_link_confirmation_dialog_description)
            .setNegativeButton(R.string.deep_link_confirmation_cancel_button) { _, _ ->
                finish()
            }
            .setPositiveButton(R.string.deep_link_confirmation_setup_button) { _, _ ->
                val intent = Intent(this, DownloadWizardActivity::class.java).apply {
                    data = "lpa:$lpaString".toUri()
                }
                startActivity(intent)
                finish()
            }
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        if (lpaString != null) {
            dialog.show()
        } else {
            val resId = R.string.deep_link_confirmation_unable_to_read_url
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val lpaString by lazy {
        val lpaUri = intent?.data?.normalizeScheme()?.takeIf { it.scheme == "lpa" } ?: return@lazy null
        runCatching { LPAString.parse(lpaUri.schemeSpecificPart) }.getOrNull()
    }
}
