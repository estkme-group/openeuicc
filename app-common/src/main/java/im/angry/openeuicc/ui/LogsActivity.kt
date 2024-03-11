package im.angry.openeuicc.ui

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Date

class LogsActivity : AppCompatActivity() {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var scrollView: ScrollView
    private lateinit var logText: TextView
    private lateinit var logStr: String

    private val saveLogs =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult
            if (!this::logStr.isInitialized) return@registerForActivityResult
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { os ->
                    os.write(logStr.encodeToByteArray())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        setSupportActionBar(requireViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        swipeRefresh = requireViewById(R.id.swipe_refresh)
        scrollView = requireViewById(R.id.scroll_view)
        logText = requireViewById(R.id.log_text)

        swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                reload()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            reload()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save -> {
            saveLogs.launch(getString(R.string.logs_filename_template,
                SimpleDateFormat.getDateTimeInstance().format(Date())
            ))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private suspend fun reload() = withContext(Dispatchers.Main) {
        swipeRefresh.isRefreshing = true

        logStr = intent.extras?.getString("log") ?: readSelfLog()

        logText.text = withContext(Dispatchers.IO) {
            // Limit the UI to display only 256 lines
            logStr.lines().takeLast(256).joinToString("\n")
        }

        swipeRefresh.isRefreshing = false

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}