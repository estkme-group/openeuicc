package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public final class UiccSlotMapping implements Parcelable {
    public static final Creator<UiccSlotMapping> CREATOR = null;

    /**
     *
     * @param portIndex         The port index is an enumeration of the ports available on the UICC.
     * @param physicalSlotIndex is unique index referring to a physical SIM slot.
     * @param logicalSlotIndex  is unique index referring to a logical SIM slot.
     *
     */
    public UiccSlotMapping(int portIndex, int physicalSlotIndex, int logicalSlotIndex) {
        throw new RuntimeException("stub");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("stub");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Port index is the unique index referring to a port belonging to the physical SIM slot.
     * If the SIM does not support multiple enabled profiles, the port index is default index 0.
     *
     * @return port index.
     */
    public int getPortIndex() {
        throw new RuntimeException("stub");
    }

    /**
     * Gets the physical slot index for the slot that the UICC is currently inserted in.
     *
     * @return physical slot index which is the index of actual physical UICC slot.
     */
    public int getPhysicalSlotIndex() {
        throw new RuntimeException("stub");
    }

    /**
     * Gets logical slot index for the slot that the UICC is currently attached.
     * Logical slot index is the unique index referring to a logical slot(logical modem stack).
     *
     * @return logical slot index;
     */
    public int getLogicalSlotIndex() {
        throw new RuntimeException("stub");
    }

    @Override
    public boolean equals(Object obj) {
        throw new RuntimeException("stub");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("stub");
    }

    @Override
    public String toString() {
        throw new RuntimeException("stub");
    }
}
