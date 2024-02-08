package im.angry.openeuicc.ui

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import im.angry.openeuicc.common.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsActivity : AppCompatActivity() {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var scrollView: ScrollView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        swipeRefresh = findViewById(R.id.swipe_refresh)
        scrollView = findViewById(R.id.scroll_view)
        logText = findViewById(R.id.log_text)

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

    private suspend fun reload() = withContext(Dispatchers.Main) {
        swipeRefresh.isRefreshing = true

        val logStr = withContext(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("logcat -t 1024").inputStream.readBytes()
                    .decodeToString()
            } catch (_: Exception) {
                ""
            }
        }

        logText.text = logStr

        swipeRefresh.isRefreshing = false

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}