{Open,Easy}EUICC
---

A fully free and open-source Local Profile Assistant implementation for Android devices.

There are two variants of this project:

- OpenEUICC: The full-fledged privileged variant. Intended to be run as a privileged system app (inside `/system/priv-app`) and serve as the system LPA. This can be used to manage all kinds of eSIM chips, embedded or removable.
  - The privileged variant can be imported to build along with AOSP by simply placing this repository and its [dependencies](https://gitea.angry.im/PeterCxy/android_prebuilts_openeuicc-deps) inside the AOSP tree.
  - Notes:
    - This repository contains submodules. If inclusion in `manifest.xml` is required, remember to set the `sync-s` option.
    - **Only the latest AOSP release** is supported for building. Older versions of AOSP are still compatible with the app itself, but it may not compile within the old AOSP trees. For older versions, consider building the app with `gradle` or a newer AOSP source tree and simply import as a prebuilt apk.
- EasyEUICC: Unprivileged version that can run as a user app. An eSIM chip must include the certificate of EasyEUICC in its ARA-M field in order to grant access without system privileges. This is intended for removable eSIM chips such as those provided by eSTK.
  - Prebuilt EasyEUICC apks can be downloaded [here](https://gitea.angry.im/PeterCxy/OpenEUICC/releases)
  - For removable eSIM chip vendors: to have your chip supported by official builds of EasyEUICC, include the ARA-M hash `2A2FA878BC7C3354C2CF82935A5945A3EDAE4AFA`

Building
===

Make sure you have all submodules cloned and updated by running

```shell
git submodule update --init
```

A file `keystore.properties` is required in the root directory. Template:

```ini
storePassword=my-store-password
keyPassword=my-password
keyAlias=my-key
unprivKeyPassword=my-unpriv-password
unprivKeyAlias=my-unpriv-key
storeFile=/path/to/android/keystore
```

Note that you must have a Java-compatible keystore generated first.

To build the privileged OpenEUICC:

```shell
./gradlew :app:assembleRelease
```

For EasyEUICC:

```shell
./gradlew :app-unpriv:assembleRelease
```

FAQs
===

- Q: Do you provide prebuilt binaries for OpenEUICC?
- A: No. If you are a custom ROM developer, either include the entire OpenEUICC repository in your AOSP source tree, or generate an APK using `gradle` and import that as a prebuilt system app. Note that you might want `privapp_whitelist_im.angry.openeuicc.xml` as well.

- Q: AOSP's Settings app seems to be confused by OpenEUICC (for example, disabling / enabling profiles from the Networks page do not work properly)
- A: When your device has internal eSIM chip(s) __and__ you have inserted a removable eSIM chip, the Settings app can misbehave since it was never designed for this scenario. __Please prefer using OpenEUICC's own management interface whenever possible.__ In the future, there might be an option to exclude removable SIMs from being reported to the Android system.

- Q: Can EasyEUICC manage my phone's internal eSIM?
- A: No. For EasyEUICC to work, the eSIM chip MUST proactively grant access via its ARA-M field.

- Q: Removable eSIMs? Are they a joke?
- A: No, even though the name "removable embedded SIM" can sound like an oxymoron. In fact, there can be many advantages to these chips compared to fully embedded ones. For example, the ability to transfer eSIM profiles without carrier support or approval, or the ability to use eSIM on devices that do not and may never get the support, such as Wi-Fi hotspots.

Copyright
===

Everything except `libs/lpac-jni`:

```
Copyright 2022-2024 OpenEUICC contributors

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, version 2.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
```

`libs/lpac-jni`:

```
Copyright (C) 2022-2024 OpenEUICC contributiors

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, version 2.1.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
```