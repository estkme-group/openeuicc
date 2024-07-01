package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MainActivity : BaseEuiccAccessActivity(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinnerItem: MenuItem
    private lateinit var spinner: Spinner
    private lateinit var loadingProgress: ProgressBar

    var loading: Boolean
        get() = loadingProgress.visibility == View.VISIBLE
        set(value) {
            loadingProgress.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

    private val fragments = arrayListOf<Fragment>()

    protected lateinit var tm: TelephonyManager

    @SuppressLint("WrongConstant", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(requireViewById(R.id.toolbar))
        loadingProgress = requireViewById(R.id.loading)

        tm = telephonyManager

        spinnerAdapter = ArrayAdapter<String>(this, R.layout.spinner_item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)

        if (!this::spinner.isInitialized) {
            spinnerItem = menu.findItem(R.id.spinner)
            spinner = spinnerItem.actionView as Spinner
            if (spinnerAdapter.isEmpty) {
                spinnerItem.isVisible = false
            }
            spinner.adapter = spinnerAdapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position < fragments.size) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_root, fragments[position]).commit()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

            }
        } else {
            // Fragments may cause this menu to be inflated multiple times.
            // Simply reuse the action view in that case
            menu.findItem(R.id.spinner).actionView = spinner
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java));
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onInit() {
        lifecycleScope.launch {
            init()
        }
    }

    private suspend fun init() {
        loading = true

        val knownChannels = withContext(Dispatchers.IO) {
            euiccChannelManager.enumerateEuiccChannels().onEach {
                Log.d(TAG, "slot ${it.slotId} port ${it.portId}")
                Log.d(TAG, it.lpa.eID)
                // Request the system to refresh the list of profiles every time we start
                // Note that this is currently supposed to be no-op when unprivileged,
                // but it could change in the future
                euiccChannelManager.notifyEuiccProfilesChanged(it.logicalSlotId)
            }
        }

        val (usbDevice, _) = withContext(Dispatchers.IO) {
            euiccChannelManager.enumerateUsbEuiccChannel()
        }

        withContext(Dispatchers.Main) {
            loading = false

            knownChannels.sortedBy { it.logicalSlotId }.forEach { channel ->
                spinnerAdapter.add(getString(R.string.channel_name_format, channel.logicalSlotId))
                fragments.add(appContainer.uiComponentFactory.createEuiccManagementFragment(channel))
            }

            // If USB readers exist, add them at the very last
            // We use a wrapper fragment to handle logic specific to USB readers
            usbDevice?.let {
                spinnerAdapter.add(it.productName)
                fragments.add(UsbCcidReaderFragment())
            }

            if (fragments.isNotEmpty()) {
                if (this@MainActivity::spinner.isInitialized) {
                    spinnerItem.isVisible = true
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_root, fragments.first()).commit()
            } else {
                supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_root,
                    appContainer.uiComponentFactory.createNoEuiccPlaceholderFragment()
                ).commit()
            }
        }
    }
}