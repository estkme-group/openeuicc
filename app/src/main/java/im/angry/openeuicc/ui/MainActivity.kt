package im.angry.openeuicc.ui

import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var manager: EuiccChannelManager

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinner: Spinner

    private val fragments = arrayListOf<EuiccManagementFragment>()

    private lateinit var noEuiccPlaceholder: View

    private lateinit var tm: TelephonyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        noEuiccPlaceholder = findViewById(R.id.no_euicc_placeholder)

        tm = openEuiccApplication.telephonyManager

        manager = openEuiccApplication.euiccChannelManager

        spinnerAdapter = ArrayAdapter<String>(this, R.layout.spinner_item)

        lifecycleScope.launch {
            init()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)

        spinner = menu.findItem(R.id.spinner).actionView as Spinner
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments[position]).commit()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        if (tm.supportsDSDS) {
            val dsds = menu.findItem(R.id.dsds)
            dsds.isVisible = true
            dsds.isChecked = tm.dsdsEnabled
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.dsds -> {
            tm.dsdsEnabled = !item.isChecked
            Toast.makeText(this, R.string.toast_dsds_switched, Toast.LENGTH_LONG).show()
            finish()
            true
        }
        else -> false
    }

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            manager.enumerateEuiccChannels()
            manager.knownChannels.forEach {
                Log.d(TAG, it.name)
                Log.d(TAG, it.lpa.eid)
                openEuiccApplication.subscriptionManager.tryRefreshCachedEuiccInfo(it.cardId)
            }
        }

        withContext(Dispatchers.Main) {
            manager.knownChannels.forEach { channel ->
                spinnerAdapter.add(channel.name)
                fragments.add(EuiccManagementFragment.newInstance(channel.slotId))
            }

            if (fragments.isNotEmpty()) {
                noEuiccPlaceholder.visibility = View.GONE
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments.first()).commit()
            }
        }
    }
}