package im.angry.openeuicc.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import im.angry.openeuicc.common.R
import java.io.FileOutputStream

// Source: <https://stackoverflow.com/questions/12478520/how-to-set-dialogfragments-width-and-height>
/**
 * Call this method (in onActivityCreated or later) to set
 * the width of the dialog to a percentage of the current
 * screen width.
 */
fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

/**
 * A handler function for `setupRootViewSystemBarInsets`, which is intended to set up
 * insets for the top toolbar, in the case where the activity contains a toolbar with the default
 * ID `R.id.toolbar`, and a spacer `R.id.toolbar_spacer` for status bar background.
 */
fun AppCompatActivity.activityToolbarInsetHandler(insets: Insets) {
    val toolbar = requireViewById<View>(R.id.toolbar)
    toolbar.apply {
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = insets.top
        }
        updatePadding(insets.left, paddingTop, insets.right, paddingBottom)
    }

    requireViewById<View>(R.id.toolbar_spacer).updateLayoutParams {
        height = toolbar.top
    }
}

/**
 * A handler function for `setupRootViewSystemBarInsets`, which is intended to set up
 * left, right, and bottom padding for a "main view", usually a RecyclerView or a ScrollView.
 *
 * It ignores top paddings because that should be handled by the toolbar handler for the activity.
 * See above.
 */
fun mainViewPaddingInsetHandler(v: View): (Insets) -> Unit = { insets ->
    // Disable clipToPadding to make sure content actually display
    if (v is ViewGroup) {
        v.clipToPadding = false
    }
    v.updatePadding(insets.left, v.paddingTop, insets.right, insets.bottom)
}

/**
 * A wrapper for `ViewCompat.setOnApplyWindowInsetsListener`, which should only be called
 * on a root view of a certain component. For activities, this should usually be `window.decorView.rootView`,
 * and for Fragments this should be the outermost layer of view it inflated during creation.
 *
 * This function takes in an array of handler functions, and is expected to only ever be called
 * on views belonging to the same hierarchy. All sibling views should be handled from the array of
 * handler functions, rather than a separate call to this function OR `ViewCompat.setOnApplyWindowInsetsListener`.
 *
 * The reason this function exists is that on some versions of Android, the dispatch of window inset
 * events is completely broken. If an inset event is handled by a view, it will never be seen by any of
 * its siblings. By wrapping this function and restricting its use to only the "main" view hierarchy and
 * handling all sibling views using our own handler functions, we work around that issue.
 *
 * Note that this function by default returns `WindowInsetCompat.CONSUME`, which will prevent the event from
 * being dispatched further to child views. This may be a problem for activities that act as fragment hosts.
 * In that case, please set `consume = false` in order for the event to propagate.
 */
fun setupRootViewSystemBarInsets(rootView: View, handlers: Array<(Insets) -> Unit>, consume: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.displayCutout()
        )

        handlers.forEach { it(bars) }

        if (consume) {
            WindowInsetsCompat.CONSUMED
        } else {
            insets
        }
    }
}

fun <T : ActivityResultCaller> T.setupLogSaving(
    getLogFileName: () -> String,
    getLogText: () -> String
): () -> Unit {
    var lastFileName = "untitled"

    val launchSaveIntent =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult

            val context = when (this@setupLogSaving) {
                is Context -> this@setupLogSaving
                is Fragment -> requireContext()
                else -> throw IllegalArgumentException("Must be either Context or Fragment!")
            }

            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { os ->
                    os.write(getLogText().encodeToByteArray())
                }
            }

            AlertDialog.Builder(context).apply {
                setMessage(R.string.logs_saved_message)
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        clipData = ClipData.newUri(context.contentResolver, lastFileName, uri)
                        putExtra(Intent.EXTRA_TITLE, lastFileName)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.startActivity(Intent.createChooser(intent, null))
                }
            }.show()
        }

    return {
        lastFileName = getLogFileName()
        launchSaveIntent.launch(lastFileName)
    }
}
