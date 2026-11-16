package im.angry.openeuicc.di

import android.content.Context
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import im.angry.easyeuicc.BuildConfig
import im.angry.easyeuicc.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class UnprivilegedCustomizableTextProvider(private val context: Context) : DefaultCustomizableTextProvider(context) {
    override val websiteUri: Uri
        get() {
            val language = ConfigurationCompat.getLocales(context.resources.configuration).get(0)?.toLanguageTag() ?: ""
            return context.getString(R.string.pref_info_website_url).toUri().buildUpon()
                .appendQueryParameter("hl", language) // host language for localized website
                .appendQueryParameter("v", BuildConfig.TOKENIZED_VERSION)
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
