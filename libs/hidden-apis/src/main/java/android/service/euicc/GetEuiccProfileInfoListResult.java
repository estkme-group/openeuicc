/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.service.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
/**
 * @hide
 */
public final class GetEuiccProfileInfoListResult implements Parcelable {
    public static final Creator<GetEuiccProfileInfoListResult> CREATOR =
            new Creator<GetEuiccProfileInfoListResult>() {
                @Override
                public GetEuiccProfileInfoListResult createFromParcel(Parcel in) {
                    return new GetEuiccProfileInfoListResult(in);
                }
                @Override
                public GetEuiccProfileInfoListResult[] newArray(int size) {
                    return new GetEuiccProfileInfoListResult[size];
                }
            };
    /**
     * @hide
     * @deprecated - Do no use. Use getResult() instead.
     */
    @Deprecated
    public final int result = 0;
    /**
     * Gets the result of the operation.
     */
    public int getResult() {
        return 0;
    }
    /** Gets the profile list (only upon success). */
    public List<EuiccProfileInfo> getProfiles() {
        return null;
    }
    /** Gets whether the eUICC is removable. */
    public boolean getIsRemovable() {
        return false;
    }
    /**
     * Construct a new {@link GetEuiccProfileInfoListResult}.
     * @param isRemovable whether the eUICC in this slot is removable. If true, the profiles
     *     returned here will only be considered accessible as long as this eUICC is present.
     *     Otherwise, they will remain accessible until the next time a response with isRemovable
     *     set to false is returned.
     */
    public GetEuiccProfileInfoListResult(
            int result, EuiccProfileInfo[] profiles, boolean isRemovable) {
    }
    private GetEuiccProfileInfoListResult(Parcel in) {
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
    @Override
    public int describeContents() {
        return 0;
    }
}