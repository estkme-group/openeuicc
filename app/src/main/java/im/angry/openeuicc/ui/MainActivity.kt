package im.angry.openeuicc.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannelRepositoryProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private val repo = EuiccChannelRepositoryProxy(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            init()
        }
    }

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            repo.load()
            repo.availableChannels?.forEach {
                Log.d(TAG, it.name)
                Log.d(TAG, it.lpa.eid)
            }
        }
    }
}