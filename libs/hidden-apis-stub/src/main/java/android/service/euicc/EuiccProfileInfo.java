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
import android.service.carrier.CarrierIdentifier;
import android.telephony.UiccAccessRule;
import java.util.List;

/**
 * Information about an embedded profile (subscription) on an eUICC.
 *
 * @hide
 */
public final class EuiccProfileInfo implements Parcelable {
    /** Profile policy rules (bit mask) */
    /** Once this profile is enabled, it cannot be disabled. */
    public static final int POLICY_RULE_DO_NOT_DISABLE = 1;
    /** This profile cannot be deleted. */
    public static final int POLICY_RULE_DO_NOT_DELETE = 1 << 1;
    /** This profile should be deleted after being disabled. */
    public static final int POLICY_RULE_DELETE_AFTER_DISABLING = 1 << 2;
    /** Class of the profile */
    /** Testing profiles */
    public static final int PROFILE_CLASS_TESTING = 0;
    /** Provisioning profiles which are pre-loaded on eUICC */
    public static final int PROFILE_CLASS_PROVISIONING = 1;
    /** Operational profiles which can be pre-loaded or downloaded */
    public static final int PROFILE_CLASS_OPERATIONAL = 2;
    /**
     * Profile class not set.
     * @hide
     */
    public static final int PROFILE_CLASS_UNSET = -1;
    /** State of the profile */
    /** Disabled profiles */
    public static final int PROFILE_STATE_DISABLED = 0;
    /** Enabled profile */
    public static final int PROFILE_STATE_ENABLED = 1;
    /**
     * Profile state not set.
     * @hide
     */
    public static final int PROFILE_STATE_UNSET = -1;

    public static final Creator<EuiccProfileInfo> CREATOR = new Creator<EuiccProfileInfo>() {
        @Override
        public EuiccProfileInfo createFromParcel(Parcel in) {
            return new EuiccProfileInfo(in);
        }
        @Override
        public EuiccProfileInfo[] newArray(int size) {
            return new EuiccProfileInfo[size];
        }
    };
    // TODO(b/70292228): Remove this method when LPA can be updated.
    /**
     * @hide
     * @deprecated - Do not use.
     */
    @Deprecated
    public EuiccProfileInfo(String iccid, UiccAccessRule[] accessRules,
                            String nickname) {

    }

    private EuiccProfileInfo(Parcel in) {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    @Override
    public int describeContents() {
        return 0;
    }
    /** The builder to build a new {@link EuiccProfileInfo} instance. */
    public static final class Builder {

        public Builder(String value) {

        }
        public Builder(EuiccProfileInfo baseProfile) {

        }
        /** Builds the profile instance. */
        public EuiccProfileInfo build() {
            return null;
        }
        /** Sets the iccId of the subscription. */
        public Builder setIccid(String value) {
            return this;
        }
        /** Sets the nickname of the subscription. */
        public Builder setNickname(String value) {
            return this;
        }
        /** Sets the service provider name of the subscription. */
        public Builder setServiceProviderName(String value) {
            return this;
        }
        /** Sets the profile name of the subscription. */
        public Builder setProfileName(String value) {
            return this;
        }
        /** Sets the profile class of the subscription. */
        public Builder setProfileClass(int value) {
            return this;
        }
        /** Sets the state of the subscription. */
        public Builder setState(int value) {
            return this;
        }
        /** Sets the carrier identifier of the subscription. */
        public Builder setCarrierIdentifier(CarrierIdentifier value) {
            return this;
        }
        /** Sets the policy rules of the subscription. */
        public Builder setPolicyRules(int value) {
            return this;
        }
        /** Sets the access rules of the subscription. */
        public Builder setUiccAccessRule(List<UiccAccessRule> value) {
            return this;
        }
    }
    private EuiccProfileInfo(
            String iccid,
            String nickname,
            String serviceProviderName,
            String profileName,
            int profileClass,
            int state,
            CarrierIdentifier carrierIdentifier,
            int policyRules,
            List<UiccAccessRule> accessRules) {

    }
    /** Gets the ICCID string. */
    public String getIccid() {
        return null;
    }
    /** Gets the access rules. */
    public List<UiccAccessRule> getUiccAccessRules() {
        return null;
    }
    /** Gets the nickname. */
    public String getNickname() {
        return null;
    }
    /** Gets the service provider name. */
    public String getServiceProviderName() {
        return null;
    }
    /** Gets the profile name. */
    public String getProfileName() {
        return null;
    }
    /** Gets the profile class. */
    public int getProfileClass() {
        return 0;
    }
    /** Gets the state of the subscription. */
    public int getState() {
        return 0;
    }
    /** Gets the carrier identifier. */
    public CarrierIdentifier getCarrierIdentifier() {
        return null;
    }
    /** Gets the policy rules. */
    public int getPolicyRules() {
        return 0;
    }
    /** Returns whether any policy rule exists. */
    public boolean hasPolicyRules() {
        return false;
    }
    /** Checks whether a certain policy rule exists. */
    public boolean hasPolicyRule(int policy) {
        return false;
    }
    @Override
    public boolean equals(Object obj) {
        return false;
    }
    @Override
    public int hashCode() {
        int result = 1;
        return result;
    }
    @Override
    public String toString() {
        return null;
    }
}