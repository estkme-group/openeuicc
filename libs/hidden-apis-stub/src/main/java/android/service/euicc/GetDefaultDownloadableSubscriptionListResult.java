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
import java.util.List;

/**
 * @hide
 */
public final class GetDefaultDownloadableSubscriptionListResult implements Parcelable {
    public static Creator<GetDefaultDownloadableSubscriptionListResult> CREATOR =
            new Creator<GetDefaultDownloadableSubscriptionListResult>() {
                @Override
                public GetDefaultDownloadableSubscriptionListResult createFromParcel(Parcel in) {
                    return new GetDefaultDownloadableSubscriptionListResult(in);
                }
                @Override
                public GetDefaultDownloadableSubscriptionListResult[] newArray(int size) {
                    return new GetDefaultDownloadableSubscriptionListResult[size];
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
     * Gets the available {@link DownloadableSubscription}s (with filled-in metadata).
     */
    public List<DownloadableSubscription> getDownloadableSubscriptions() {
        return null;
    }
    /**
     * Construct a new {@link GetDefaultDownloadableSubscriptionListResult}.
     */
    public GetDefaultDownloadableSubscriptionListResult(int result,
                                                        DownloadableSubscription[] subscriptions) {

    }
    private GetDefaultDownloadableSubscriptionListResult(Parcel in) {
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    @Override
    public int describeContents() {
        return 0;
    }
}