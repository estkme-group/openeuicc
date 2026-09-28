package im.angry.openeuicc.util

import android.content.Intent
import android.os.Bundle
import android.telephony.UiccAccessRule
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import java.lang.reflect.Method

// System APIs of android.telephony.euicc via reflection to enable building without AOSP source
// tree (see TelephonyManagerHiddenApi.kt). When building against AOSP, the real member
// functions take precedence and this file is not compiled at all.
private val continueOperation: Method by lazy {
    EuiccManager::class.java.getMethod(
        "continueOperation",
        Intent::class.java, Bundle::class.java
    )
}
private val builderSetCarrierName: Method by lazy {
    DownloadableSubscription.Builder::class.java.getMethod(
        "setCarrierName",
        String::class.java
    )
}
private val builderSetAccessRules: Method by lazy {
    DownloadableSubscription.Builder::class.java.getMethod(
        "setAccessRules",
        List::class.java
    )
}

fun EuiccManager.continueOperation(resolutionIntent: Intent, resolutionExtras: Bundle) {
    continueOperation.invoke(this, resolutionIntent, resolutionExtras)
}

fun DownloadableSubscription.Builder.setCarrierName(value: String): DownloadableSubscription.Builder =
    builderSetCarrierName.invoke(this, value) as DownloadableSubscription.Builder

fun DownloadableSubscription.Builder.setAccessRules(value: List<UiccAccessRule>): DownloadableSubscription.Builder =
    builderSetAccessRules.invoke(this, value) as DownloadableSubscription.Builder
