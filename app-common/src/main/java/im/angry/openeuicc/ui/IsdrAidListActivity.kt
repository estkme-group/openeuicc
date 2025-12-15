package im.angry.openeuicc.ui

import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class IsdrAidListActivity : AppCompatActivity() {
    private lateinit var isdrAidListEditor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isdr_aid_list)
        setSupportActionBar(requireViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        isdrAidListEditor = requireViewById(R.id.isdr_aid_list_editor)

        setupRootViewSystemBarInsets(
            window.decorView.rootView, arrayOf(
                this::activityToolbarInsetHandler,
                mainViewPaddingInsetHandler(isdrAidListEditor)
            )
        )

        lifecycleScope.launch {
            preferenceRepository.isdrAidListFlow.onEach {
                isdrAidListEditor.text = Editable.Factory.getInstance().newEditable(it)
            }.collect()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_isdr_aid_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.save -> {
                lifecycleScope.launch {
                    preferenceRepository.isdrAidListFlow.updatePreference(isdrAidListEditor.text.toString())
                    Toast.makeText(
                        this@IsdrAidListActivity,
                        R.string.isdr_aid_list_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }

            R.id.reset -> {
                lifecycleScope.launch {
                    preferenceRepository.isdrAidListFlow.removePreference()
                }
                true
            }

            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
}
