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
package android.telephony;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

/**
 * Describes a single UICC access rule according to the GlobalPlatform Secure Element Access Control
 * specification.
 *
 * @hide
 */
public final class UiccAccessRule implements Parcelable {

    public static final Creator<UiccAccessRule> CREATOR = new Creator<UiccAccessRule>() {
        @Override
        public UiccAccessRule createFromParcel(Parcel in) {
            return new UiccAccessRule(in);
        }
        @Override
        public UiccAccessRule[] newArray(int size) {
            return new UiccAccessRule[size];
        }
    };
    /**
     * Encode these access rules as a byte array which can be parsed with {@link #decodeRules}.
     * @hide
     */
    public static byte[] encodeRules(UiccAccessRule[] accessRules) {
        return null;
    }
    /**
     * Decodes {@link CarrierConfigManager#KEY_CARRIER_CERTIFICATE_STRING_ARRAY} values.
     * @hide
     */
    public static UiccAccessRule[] decodeRulesFromCarrierConfig(String[] certs) {
        return null;
    }
    /**
     * Decodes a byte array generated with {@link #encodeRules}.
     * @hide
     */
    public static UiccAccessRule[] decodeRules(byte[] encodedRules) {
        return null;
    }

    public UiccAccessRule(byte[] certificateHash, String packageName, long accessType) {

    }
    UiccAccessRule(Parcel in) {

    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    /**
     * Return the package name this rule applies to.
     *
     * @return the package name, or null if this rule applies to any package signed with the given
     *     certificate.
     */
    public String getPackageName() {
        return null;
    }
    /**
     * Returns the hex string of the certificate hash.
     */
    public String getCertificateHexString() {
        return null;
    }
    /**
     * Returns the carrier privilege status associated with the given package.
     *
     * @param packageInfo package info fetched from
     *     {@link android.content.pm.PackageManager#getPackageInfo}.
     *     {@link android.content.pm.PackageManager#GET_SIGNING_CERTIFICATES} must have been
     *         passed in.
     */
    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        return 0;
    }
    /**
     * Returns the carrier privilege status for the given certificate and package name.
     *
     * @param signature The signature of the certificate.
     * @param packageName name of the package.
     */
    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        return 0;
    }
    /**
     * Returns true if the given certificate and package name match this rule's values.
     * @hide
     */
    public boolean matches(String certHash, String packageName) {
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
    @Override
    public int describeContents() {
        return 0;
    }
    /**
     * Gets all of the Signatures from the given PackageInfo.
     * @hide
     */
    public static List<Signature> getSignatures(PackageInfo packageInfo) {
        return null;
    }
    /**
     * Converts a Signature into a Certificate hash usable for comparison.
     * @hide
     */
    public static byte[] getCertHash(Signature signature, String algo) {
        return null;
    }
}