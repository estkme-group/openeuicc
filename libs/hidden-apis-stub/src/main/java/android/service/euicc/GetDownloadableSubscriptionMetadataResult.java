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
import android.telephony.euicc.DownloadableSubscription;
/**
 * @hide
 */
public final class GetDownloadableSubscriptionMetadataResult implements Parcelable {
    public static final Creator<GetDownloadableSubscriptionMetadataResult> CREATOR =
            new Creator<GetDownloadableSubscriptionMetadataResult>() {
                @Override
                public GetDownloadableSubscriptionMetadataResult createFromParcel(Parcel in) {
                    return new GetDownloadableSubscriptionMetadataResult(in);
                }
                @Override
                public GetDownloadableSubscriptionMetadataResult[] newArray(int size) {
                    return new GetDownloadableSubscriptionMetadataResult[size];
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
    /**
     * Gets the {@link DownloadableSubscription} with filled-in metadata.
     */
    public DownloadableSubscription getDownloadableSubscription() {
        return null;
    }
    /**
     * Construct a new {@link GetDownloadableSubscriptionMetadataResult}.
     */
    public GetDownloadableSubscriptionMetadataResult(int result,
                                                     DownloadableSubscription subscription) {

    }
    private GetDownloadableSubscriptionMetadataResult(Parcel in) {

    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    @Override
    public int describeContents() {
        return 0;
    }
}