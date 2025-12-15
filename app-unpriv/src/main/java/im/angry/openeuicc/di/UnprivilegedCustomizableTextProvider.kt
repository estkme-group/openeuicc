package im.angry.openeuicc.di

import android.content.Context
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import im.angry.easyeuicc.R
import im.angry.openeuicc.common.BuildConfig
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class UnprivilegedCustomizableTextProvider(private val context: Context) : DefaultCustomizableTextProvider(context) {
    override val websiteUri: Uri
        get() {
            val parts = arrayOf(
                context.selfAppVersion, // show user current version
                context.selfAppVersionCode.toString(36), // check is upgradable
                BuildConfig.BUILD_TYPE, // update channels
            )
            val message = parts.joinToString("\u0000")
            val signed = Base64.encodeToString(
                // HMAC-SHA256 over the message with app signing certs as key
                with(Mac.getInstance("HmacSHA256")) {
                    // Concatenate all signing certs bytes to form the key
                    val signingInfo = with(context) {
                        packageManager.getPackageInfo(packageName, /* flags = */ GET_SIGNING_CERTIFICATES).signingInfo!!
                    }
                    val key = signingInfo.apkContentsSigners.map { it.toByteArray() }.reduce { a, b -> a + b }
                    init(SecretKeySpec(key, algorithm))
                    doFinal(message.encodeToByteArray())
                },
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            return context.getString(R.string.pref_info_website_url).toUri().buildUpon()
                .appendQueryParameter("v", message)
                .appendQueryParameter("v", signed)
                .build()
        }

    override fun formatNonUsbChannelName(logicalSlotId: Int): String =
        context.getString(R.string.channel_name_format_unpriv, logicalSlotId)

    override fun formatNonUsbChannelNameWithSeId(
        logicalSlotId: Int,
        seId: EuiccChannel.SecureElementId
    ): String =
        context.getString(R.string.channel_name_format_unpriv_se, logicalSlotId, seId.id)
}
