package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.UnprivilegedEuiccContextMarker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UnprivilegedMainActivity : MainActivity(), UnprivilegedEuiccContextMarker {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (runBlocking { !preferenceRepository.skipQuickAvailabilityFlow.first() }) {
            startActivity(Intent(this, QuickCompatibilityActivity::class.java))
        }
    }

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