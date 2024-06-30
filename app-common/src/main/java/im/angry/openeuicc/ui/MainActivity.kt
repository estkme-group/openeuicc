package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MainActivity : BaseEuiccAccessActivity(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "MainActivity"
        const val ACTION_USB_PERMISSION = "im.angry.openeuicc.USB_PERMISSION"
    }

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinnerItem: MenuItem
    private lateinit var spinner: Spinner

    private val fragments = arrayListOf<EuiccManagementFragment>()

    protected lateinit var tm: TelephonyManager

    private val usbManager: UsbManager by lazy {
        getSystemService(USB_SERVICE) as UsbManager
    }

    private var usbDevice: UsbDevice? = null
    private var usbChannel: EuiccChannel? = null

    private lateinit var usbPendingIntent: PendingIntent

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        switchToUsbFragmentIfPossible()
                    }
                }
            }
        }
    }

    @SuppressLint("WrongConstant", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(requireViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_root,
            appContainer.uiComponentFactory.createNoEuiccPlaceholderFragment()
        ).commit()

        tm = telephonyManager

        spinnerAdapter = ArrayAdapter<String>(this, R.layout.spinner_item)

        usbPendingIntent = PendingIntent.getBroadcast(this, 0,
            Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbPermissionReceiver, filter)
        }
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
                    } else if (position == fragments.size) {
                        // If we are at the last position, this is the USB device
                        lifecycleScope.launch(Dispatchers.Main) {
                            switchToUsbFragmentIfPossible()
                        }
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

        withContext(Dispatchers.IO) {
            val res = euiccChannelManager.enumerateUsbEuiccChannel()
            usbDevice = res.first
            usbChannel = res.second
        }

        withContext(Dispatchers.Main) {
            knownChannels.sortedBy { it.logicalSlotId }.forEach { channel ->
                spinnerAdapter.add(getString(R.string.channel_name_format, channel.logicalSlotId))
                fragments.add(appContainer.uiComponentFactory.createEuiccManagementFragment(channel))
            }

            // If USB readers exist, add them at the very last
            // The adapter logic depends on this assumption
            usbDevice?.let { spinnerAdapter.add(it.productName) }

            if (fragments.isNotEmpty()) {
                if (this@MainActivity::spinner.isInitialized) {
                    spinnerItem.isVisible = true
                }
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments.first()).commit()
            }
        }
    }

    private suspend fun switchToUsbFragmentIfPossible() {
        if (usbDevice != null && usbChannel == null) {
            if (!usbManager.hasPermission(usbDevice)) {
                usbManager.requestPermission(usbDevice, usbPendingIntent)
                return
            }  else {
               val (device, channel) = withContext(Dispatchers.IO) {
                    euiccChannelManager.enumerateUsbEuiccChannel()
                }

                if (device != null && channel != null) {
                    usbDevice = device
                    usbChannel = channel
                }
            }
        }

        if (usbChannel != null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_root,
                appContainer.uiComponentFactory.createEuiccManagementFragment(usbChannel!!)).commit()
        }
    }
}