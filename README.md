<img src="https://gitea.angry.im/PeterCxy/OpenEUICC/media/branch/master/art/OpenEUICCBG.svg" width="512" height="300">

A fully free and open-source Local Profile Assistant implementation for Android devices.

There are two variants of this project, OpenEUICC and EasyEUICC:

|                               |            OpenEUICC            |      EasyEUICC      |
| :---------------------------- | :-----------------------------: | :-----------------: |
| Privileged                    | Must be installed as system app |         No          |
| Internal eSIM                 |            Supported            |     Unsupported     |
| External eSIM [^1]            |            Supported            |      Supported      |
| USB Readers                   |            Supported            |      Supported      |
| Requires allowlisting by eSIM |               No                |  Yes -- except USB  |
| System Integration            |          Partial [^2]           |         No          |
| Minimum Android Version       |      Android 11 or higher       | Android 9 or higher |

[^1]: Also known as "Removable eSIM"
[^2]: Carrier Partner API unimplemented yet

Some side notes:

1. When privileged, OpenEUICC supports any eUICC chip that implements the [SGP.22] standard, internal or external.
   However, there is **no guarantee** that external (removable) eSIMs actually follow the standard.
   Please **DO NOT** submit bug reports for non-functioning removable eSIMs.
   They are **NOT** officially supported unless they also support / are supported by EasyEUICC, the unprivileged variant.
2. Both variants support accessing eUICC chips through USB CCID readers,
   regardless of whether the chip contains the correct ARA-M hash to allow for unprivileged access.
   However, only `T=0` readers that use the standard [USB CCID protocol][usb-ccid] are supported.
3. Prebuilt release-mode EasyEUICC apks can be downloaded [here][releases].
   For OpenEUICC, no official release is currently provided and only debug mode APKs and Magisk modules can be found in the [CI page][actions].
4. For removable eSIM chip vendors: to have your chip supported by official builds of EasyEUICC when inserted,
   include the ARA-M hash `2A2FA878BC7C3354C2CF82935A5945A3EDAE4AFA`.

[sgp.22]: https://www.gsma.com/solutions-and-impact/technologies/esim/gsma_resources/sgp-22-v2-2-2/ "SGP.22 v2.2.2"
[usb-ccid]: https://en.wikipedia.org/wiki/CCID_%28protocol%29 "USB CCID Protocol"
[releases]: https://gitea.angry.im/PeterCxy/OpenEUICC/releases "EasyEUICC Releases"
[actions]: https://gitea.angry.im/PeterCxy/OpenEUICC/actions "OpenEUICC Actions"

**This project is Free Software licensed under GNU GPL v3, WITHOUT the "or later" clause.**
Any modification and derivative work **MUST** be released under the SAME license, which means, at the very least, that the source code **MUST** be available upon request.

**If you are releasing a modification of this app, you are kindly asked to make changes to at least the app name and package name.**

# Building (Gradle)

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

# Building (AOSP)

There are two ways to include OpenEUICC in your AOSP-based system image:

1. Include this project and its [dependencies](https://gitea.angry.im/PeterCxy/android_prebuilts_openeuicc-deps) inside the AOSP tree.
   - If inclusion in `manifest.xml` is required, remember to set the `sync-s` option to clone submodules.
   - The module name is `OpenEUICC`. You can include it in `PRODUCT_PACKAGES`, or simply build it standalone using `mm`.
   - Compilation of this project is **only** tested against the latest AOSP release version. The app itself should be compatible with older AOSP versions, but the source may not compile against an older AOSP source tree.
2. If compilation against AOSP source tree is not possible, consider [building with gradle](#building-gradle) and import the apk as a prebuilt.
   - No official `Android.bp` is provided for this case but it should be straightforward to write.
   - You might want to include [`privapp_whitelist_im.angry.openeuicc.xml`] as well.

[`privapp_whitelist_im.angry.openeuicc.xml`]: privapp_whitelist_im.angry.openeuicc.xml "OpenEUICC Privapp Whitelist"

# FAQs

- Q: Do you provide prebuilt binaries for OpenEUICC? \
  A: Debug-mode APKs and Magisk modules are available continuously as an artifact of the [Actions] CI used by this project. However, these debug-mode APKs are **not** intended for inclusion inside system images, nor are they supported by the developer in any sense. If you are a custom ROM developer, either include the entire OpenEUICC repository in your AOSP source tree, or generate an APK using `gradle` and import that as a prebuilt system app. Note that you might want [`privapp_whitelist_im.angry.openeuicc.xml`] as well.

- Q: Can EasyEUICC manage my phone's internal eSIM? \
  A: No. For EasyEUICC to work, the eSIM chip MUST proactively grant access via its ARA-M field.

- Q: Removable eSIMs? Are they a joke? \
  A: No, even though the name "removable embedded SIM" can sound like an oxymoron. In fact, there can be many advantages to these chips compared to fully embedded ones. For example, the ability to transfer eSIM profiles without carrier support or approval, or the ability to use eSIM on devices that do not and may never get the support, such as Wi-Fi hotspots.

# Copyright

Everything except `libs/lpac-jni` and `art/`:

```
Copyright 2022-2024 OpenEUICC contributors

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, version 3.

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

`art/`: Courtesy of [Aikoyori](https://github.com/Aikoyori), CC NC-SA 4.0.
