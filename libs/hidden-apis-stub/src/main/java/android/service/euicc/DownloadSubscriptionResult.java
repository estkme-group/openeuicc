/*
 * Copyright (C) 2018 The Android Open Source Project
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
/**
 * @hide
 */
public final class DownloadSubscriptionResult implements Parcelable {
    public static final Creator<DownloadSubscriptionResult> CREATOR =
            new Creator<DownloadSubscriptionResult>() {
                @Override
                public DownloadSubscriptionResult createFromParcel(Parcel in) {
                    return new DownloadSubscriptionResult(in);
                }
                @Override
                public DownloadSubscriptionResult[] newArray(int size) {
                    return new DownloadSubscriptionResult[size];
                }
            };

    public DownloadSubscriptionResult(int result, int resolvableErrors,
                                      int cardId) {

    }
    /** Gets the result of the operation. */
    public int getResult() {
        return 0;
    }
    /**
     * Gets the bit map of resolvable errors.
     */
    public int getResolvableErrors() {
        return 0;
    }
    /**
     * Gets the card Id. This is used when resolving resolvable errors. The value is passed from
     * EuiccService.
     */
    public int getCardId() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    @Override
    public int describeContents() {
        return 0;
    }
    private DownloadSubscriptionResult(Parcel in) {
    }
}