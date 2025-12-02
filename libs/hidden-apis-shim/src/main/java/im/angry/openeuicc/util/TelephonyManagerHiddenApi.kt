package im.angry.openeuicc.util

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
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
private val iccOpenLogicalChannelByPort: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccOpenLogicalChannelByPort",
        Int::class.java, Int::class.java, String::class.java, Int::class.java
    )
}
private val iccCloseLogicalChannelBySlot: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccCloseLogicalChannelBySlot",
        Int::class.java, Int::class.java
    )
}
private val iccCloseLogicalChannelByPort: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccCloseLogicalChannelByPort",
        Int::class.java, Int::class.java, Int::class.java
    )
}
private val iccTransmitApduLogicalChannelBySlot: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccTransmitApduLogicalChannelBySlot",
        Int::class.java, Int::class.java, Int::class.java, Int::class.java,
        Int::class.java, Int::class.java, Int::class.java, String::class.java
    )
}
private val iccTransmitApduLogicalChannelByPort: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "iccTransmitApduLogicalChannelByPort",
        Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java,
        Int::class.java, Int::class.java, Int::class.java, String::class.java
    )
}
private val getSimSlotMapping: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "getSimSlotMapping"
    )
}
private val setSimSlotMapping: Method by lazy {
    TelephonyManager::class.java.getMethod(
        "setSimSlotMapping",
        Collection::class.java
    )
}

fun TelephonyManager.iccOpenLogicalChannelBySlot(
    slotId: Int, appletId: String?, p2: Int
): IccOpenLogicalChannelResponse =
    iccOpenLogicalChannelBySlot.invoke(this, slotId, appletId, p2) as IccOpenLogicalChannelResponse

fun TelephonyManager.iccOpenLogicalChannelByPort(
    slotId: Int, portId: Int, appletId: String?, p2: Int
): IccOpenLogicalChannelResponse =
    iccOpenLogicalChannelByPort.invoke(this, slotId, portId, appletId, p2) as IccOpenLogicalChannelResponse

fun TelephonyManager.iccCloseLogicalChannelBySlot(slotId: Int, channel: Int) {
    iccCloseLogicalChannelBySlot.invoke(this, slotId, channel)
}

fun TelephonyManager.iccCloseLogicalChannelByPort(slotId: Int, portId: Int, channel: Int) {
    iccCloseLogicalChannelByPort.invoke(this, slotId, portId, channel)
}

fun TelephonyManager.iccTransmitApduLogicalChannelBySlot(
    slotId: Int, channel: Int, cla: Int, instruction: Int,
    p1: Int, p2: Int, p3: Int, data: String?
): String? =
    iccTransmitApduLogicalChannelBySlot.invoke(
        this, slotId, channel, cla, instruction, p1, p2, p3, data
    ) as String?

fun TelephonyManager.iccTransmitApduLogicalChannelByPort(
    slotId: Int, portId: Int, channel: Int, cla: Int, instruction: Int,
    p1: Int, p2: Int, p3: Int, data: String?
): String? =
    iccTransmitApduLogicalChannelByPort.invoke(
        this, slotId, portId, channel, cla, instruction, p1, p2, p3, data
    ) as String?

var TelephonyManager.simSlotMapping: Collection<UiccSlotMapping>
    get() = getSimSlotMapping.invoke(this) as Collection<UiccSlotMapping>
    set(new) {
        setSimSlotMapping.invoke(this, new)
    }

private val requestEmbeddedSubscriptionInfoListRefresh: Method by lazy {
    SubscriptionManager::class.java.getMethod("requestEmbeddedSubscriptionInfoListRefresh", Int::class.java)
}

fun SubscriptionManager.requestEmbeddedSubscriptionInfoListRefresh(cardId: Int) {
    requestEmbeddedSubscriptionInfoListRefresh.invoke(this, cardId)
}
