package im.angry.openeuicc.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*

class SettingsActivity: AppCompatActivity() {
    private val appContainer
        get() = (application as OpenEuiccApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(requireViewById(R.id.toolbar))
        setupToolbarInsets()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val settingsFragment = appContainer.uiComponentFactory.createSettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, settingsFragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}