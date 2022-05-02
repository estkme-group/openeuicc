package im.angry.openeuicc.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class LuiActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}