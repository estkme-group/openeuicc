package im.angry.openeuicc.util

import android.app.Activity
import android.content.pm.PackageManager

fun Activity.requestPermissions(requestCode: Int, vararg permissions: String) {
    val deniedPermissions = permissions.filter { name ->
        checkSelfPermission(name) == PackageManager.PERMISSION_DENIED
    }
    if (deniedPermissions.isNotEmpty()) {
        requestPermissions(deniedPermissions.toTypedArray(), requestCode)
    }
}