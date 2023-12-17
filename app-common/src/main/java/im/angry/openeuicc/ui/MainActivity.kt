package im.angry.openeuicc.ui

import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    protected lateinit var manager: EuiccChannelManager

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinner: Spinner

    private val fragments = arrayListOf<EuiccManagementFragment>()

    private lateinit var noEuiccPlaceholder: View

    protected lateinit var tm: TelephonyManager

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

        return true
    }

    protected open fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment =
        EuiccManagementFragment.newInstance(channel.slotId, channel.portId)

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            manager.enumerateEuiccChannels()
            manager.knownChannels.forEach {
                Log.d(TAG, it.name)
                Log.d(TAG, it.lpa.eID)
                // Request the system to refresh the list of profiles every time we start
                // Note that this is currently supposed to be no-op when unprivileged,
                // but it could change in the future
                manager.notifyEuiccProfilesChanged(it.logicalSlotId)
            }
        }

        withContext(Dispatchers.Main) {
            manager.knownChannels.forEach { channel ->
                spinnerAdapter.add(channel.name)
                fragments.add(createEuiccManagementFragment(channel))
            }

            if (fragments.isNotEmpty()) {
                noEuiccPlaceholder.visibility = View.GONE
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments.first()).commit()
            }
        }
    }
}