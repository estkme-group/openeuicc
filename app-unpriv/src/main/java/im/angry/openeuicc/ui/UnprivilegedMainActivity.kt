package im.angry.openeuicc.ui

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import im.angry.easyeuicc.R

class UnprivilegedMainActivity: MainActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main_unprivileged, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.compatibility_check -> {
                startActivity(Intent(this, CompatibilityCheckActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}