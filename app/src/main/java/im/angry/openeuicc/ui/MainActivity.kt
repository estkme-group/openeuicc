package im.angry.openeuicc.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.OpenEUICCApplication
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannelRepository
import im.angry.openeuicc.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var repo: EuiccChannelRepository

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinner: Spinner

    private val fragments = arrayListOf<EuiccManagementFragment>()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as OpenEUICCApplication).euiccChannelRepo

        spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)

        lifecycleScope.launch {
            init()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_slot_spinner, menu)

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

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            repo.load()
            repo.availableChannels.forEach {
                Log.d(TAG, it.name)
                Log.d(TAG, it.lpa.eid)
            }
        }

        withContext(Dispatchers.Main) {
            repo.availableChannels.forEach {
                spinnerAdapter.add(it.name)
                fragments.add(EuiccManagementFragment(it))
            }

            if (fragments.isNotEmpty()) {
                binding.noEuiccPlaceholder.visibility = View.GONE
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments.first()).commit()
            }
        }
    }
}