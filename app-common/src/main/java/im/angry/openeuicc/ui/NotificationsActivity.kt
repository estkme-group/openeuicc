package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnMenuItemClickListener
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.typeblog.lpac_jni.LocalProfileNotification

class NotificationsActivity : BaseEuiccAccessActivity(), OpenEuiccContextMarker {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var notificationList: RecyclerView
    private val notificationAdapter = NotificationAdapter()

    private var logicalSlotId = -1
    private var seId = EuiccChannel.SecureElementId.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setSupportActionBar(requireViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        swipeRefresh = requireViewById(R.id.swipe_refresh)
        notificationList = requireViewById(R.id.recycler_view)

        setupRootViewSystemBarInsets(
            window.decorView.rootView, arrayOf(
                this::activityToolbarInsetHandler,
                mainViewPaddingInsetHandler(notificationList)
            )
        )
    }

    override fun onInit() {
        notificationList.apply {
            val context = this@NotificationsActivity
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            registerForContextMenu(this)
        }

        logicalSlotId = intent.getIntExtra("logicalSlotId", 0)
        seId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("seId", EuiccChannel.SecureElementId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("seId")
        } ?: EuiccChannel.SecureElementId.DEFAULT

        // This is slightly different from the MainActivity logic
        // due to the length (we don't want to display the full USB product name)
        val channelTitle = if (logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            getString(R.string.channel_type_usb)
        } else {
            appContainer.customizableTextProvider.formatNonUsbChannelName(logicalSlotId)
        }

        title = getString(R.string.profile_notifications_detailed_format, channelTitle)

        swipeRefresh.setOnRefreshListener {
            refresh()
        }

        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.help -> {
                AlertDialog.Builder(this, R.style.AlertDialogTheme).apply {
                    setMessage(R.string.profile_notifications_help)
                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun launchTask(task: suspend () -> Unit) {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                euiccChannelManagerLoaded.await()
            }

            task()

            swipeRefresh.isRefreshing = false
        }
    }

    private fun refresh() {
        launchTask {
            notificationAdapter.notifications = withEuiccChannel { channel ->
                if (channel.hasMultipleSE && logicalSlotId != EuiccChannelManager.USB_CHANNEL_ID) {
                    withContext(Dispatchers.Main) {
                        title =
                            appContainer.customizableTextProvider.formatNonUsbChannelNameWithSeId(logicalSlotId, seId)
                    }
                }

                val nameMap = channel.lpa.profiles
                    .associate { Pair(it.iccid, it.displayName) }

                channel.lpa.notifications.map {
                    LocalProfileNotificationWrapper(it, nameMap[it.iccid] ?: "???")
                }
            }
        }
    }

    private suspend fun <R> withEuiccChannel(fn: suspend (EuiccChannel) -> R) =
        euiccChannelManager.withEuiccChannel(logicalSlotId, seId, fn)

    data class LocalProfileNotificationWrapper(
        val inner: LocalProfileNotification,
        val profileName: String
    )

    @SuppressLint("ClickableViewAccessibility")
    inner class NotificationViewHolder(private val root: View) :
        RecyclerView.ViewHolder(root), View.OnCreateContextMenuListener, OnMenuItemClickListener {
        private val address: TextView = root.requireViewById(R.id.notification_address)
        private val sequenceNumber: TextView =
            root.requireViewById(R.id.notification_sequence_number)
        private val profileName: TextView = root.requireViewById(R.id.notification_profile_name)

        private lateinit var notification: LocalProfileNotificationWrapper

        private var lastTouchX = 0f
        private var lastTouchY = 0f

        init {
            root.isClickable = true
            root.setOnCreateContextMenuListener(this)
            root.setOnTouchListener { _, event ->
                lastTouchX = event.x
                lastTouchY = event.y
                false
            }
            root.setOnLongClickListener {
                root.showContextMenu(lastTouchX, lastTouchY)
                true
            }
        }


        private fun operationToLocalizedText(operation: LocalProfileNotification.Operation) =
            root.context.getText(
                when (operation) {
                    LocalProfileNotification.Operation.Install -> R.string.profile_notification_operation_download
                    LocalProfileNotification.Operation.Delete -> R.string.profile_notification_operation_delete
                    LocalProfileNotification.Operation.Enable -> R.string.profile_notification_operation_enable
                    LocalProfileNotification.Operation.Disable -> R.string.profile_notification_operation_disable
                }
            )

        fun updateNotification(value: LocalProfileNotificationWrapper) {
            notification = value

            address.text = value.inner.notificationAddress
            sequenceNumber.text = root.context.getString(
                R.string.profile_notification_sequence_number_format,
                value.inner.seqNumber
            )
            profileName.text = Html.fromHtml(
                root.context.getString(
                    R.string.profile_notification_name_format,
                    operationToLocalizedText(value.inner.profileManagementOperation),
                    value.profileName, value.inner.iccid
                ),
                Html.FROM_HTML_MODE_COMPACT
            )
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menuInflater.inflate(R.menu.notification_options, menu)

            menu!!.forEach {
                it.setOnMenuItemClickListener(this)
            }
        }

        override fun onMenuItemClick(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.notification_process -> {
                    launchTask {
                        withContext(Dispatchers.IO) {
                            withEuiccChannel { channel ->
                                channel.lpa.handleNotification(notification.inner.seqNumber)
                            }
                        }

                        refresh()
                    }
                    true
                }

                R.id.notification_delete -> {
                    launchTask {
                        withContext(Dispatchers.IO) {
                            withEuiccChannel { channel ->
                                channel.lpa.deleteNotification(notification.inner.seqNumber)
                            }
                        }

                        refresh()
                    }
                    true
                }

                else -> false
            }
    }

    inner class NotificationAdapter : RecyclerView.Adapter<NotificationViewHolder>() {
        var notifications: List<LocalProfileNotificationWrapper> = listOf()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.notification_item, parent, false)
            return NotificationViewHolder(root)
        }

        override fun getItemCount(): Int = notifications.size

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) =
            holder.updateNotification(notifications[position])

    }
}
