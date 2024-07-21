package im.angry.openeuicc.ui

import android.annotation.SuppressLint
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

class NotificationsActivity: BaseEuiccAccessActivity(), OpenEuiccContextMarker {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var notificationList: RecyclerView
    private val notificationAdapter = NotificationAdapter()

    private lateinit var euiccChannel: EuiccChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setSupportActionBar(requireViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onInit() {
        euiccChannel = euiccChannelManager
            .findEuiccChannelBySlotBlocking(intent.getIntExtra("logicalSlotId", 0))!!

        swipeRefresh = requireViewById(R.id.swipe_refresh)
        notificationList = requireViewById(R.id.recycler_view)

        notificationList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        notificationList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        notificationList.adapter = notificationAdapter
        registerForContextMenu(notificationList)

        // This is slightly different from the MainActivity logic
        // due to the length (we don't want to display the full USB product name)
        val channelTitle = if (euiccChannel.logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            getString(R.string.usb)
        } else {
            getString(R.string.channel_name_format, euiccChannel.logicalSlotId)
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
            task()

            swipeRefresh.isRefreshing = false
        }
    }

    private fun refresh() {
       launchTask {
           val profiles = withContext(Dispatchers.IO) {
               euiccChannel.lpa.profiles
           }

           notificationAdapter.notifications =
               withContext(Dispatchers.IO) {
                   euiccChannel.lpa.notifications.map {
                       val profile = profiles.find { p -> p.iccid == it.iccid }
                       LocalProfileNotificationWrapper(it, profile?.displayName ?: "???")
                   }
               }
       }
    }

    data class LocalProfileNotificationWrapper(
        val inner: LocalProfileNotification,
        val profileName: String
    )

    @SuppressLint("ClickableViewAccessibility")
    inner class NotificationViewHolder(private val root: View):
        RecyclerView.ViewHolder(root), View.OnCreateContextMenuListener, OnMenuItemClickListener {
        private val address: TextView = root.requireViewById(R.id.notification_address)
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
                })

        fun updateNotification(value: LocalProfileNotificationWrapper) {
            notification = value

            address.text = value.inner.notificationAddress
            profileName.text = Html.fromHtml(
                root.context.getString(R.string.profile_notification_name_format,
                    operationToLocalizedText(value.inner.profileManagementOperation),
                    value.profileName, value.inner.iccid),
                Html.FROM_HTML_MODE_COMPACT)
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
                            euiccChannel.lpa.handleNotification(notification.inner.seqNumber)
                        }

                        refresh()
                    }
                    true
                }
                R.id.notification_delete -> {
                    launchTask {
                        withContext(Dispatchers.IO) {
                            euiccChannel.lpa.deleteNotification(notification.inner.seqNumber)
                        }

                        refresh()
                    }
                    true
                }
                else -> false
            }
    }

    inner class NotificationAdapter: RecyclerView.Adapter<NotificationViewHolder>() {
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