package im.angry.openeuicc.util

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.lang.reflect.Method

// Hidden APIs via reflection to enable building without AOSP source tree
// When building against AOSP, this file can be simply excluded to resolve
// calls to AOSP hidden APIs
private val iccOpenLogicalChannelBySlot: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccOpenLogicalChannelBySlot",
        Int::class.java, String::class.java, Int::class.java
    )
}
private val iccCloseLogicalChannelBySlot: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccCloseLogicalChannelBySlot",
        Int::class.java, Int::class.java
    )
}
private val iccTransmitApduLogicalChannelBySlot: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccTransmitApduLogicalChannelBySlot",
        Int::class.java, Int::class.java, Int::class.java, Int::class.java,
        Int::class.java, Int::class.java, Int::class.java, String::class.java
    )
}

fun TelephonyManager.iccOpenLogicalChannelBySlot(
    slotId: Int, appletId: String, p2: Int
): IccOpenLogicalChannelResponse =
    iccOpenLogicalChannelBySlot.invoke(this, slotId, appletId, p2) as IccOpenLogicalChannelResponse

fun TelephonyManager.iccCloseLogicalChannelBySlot(slotId: Int, channel: Int): Boolean =
    iccCloseLogicalChannelBySlot.invoke(this, slotId, channel) as Boolean

fun TelephonyManager.iccTransmitApduLogicalChannelBySlot(
    slotId: Int, channel: Int, cla: Int, instruction: Int,
    p1: Int, p2: Int, p3: Int, data: String?
): String? =
    iccTransmitApduLogicalChannelBySlot.invoke(
        this, slotId, channel, cla, instruction, p1, p2, p3, data
    ) as String?

private val requestEmbeddedSubscriptionInfoListRefresh: Method by lazy {
    SubscriptionManager::class.java.getMethod("requestEmbeddedSubscriptionInfoListRefresh", Int::class.java)
}

fun SubscriptionManager.requestEmbeddedSubscriptionInfoListRefresh(cardId: Int): Unit {
    requestEmbeddedSubscriptionInfoListRefresh.invoke(this, cardId)
}