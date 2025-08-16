TMP_FILE="$TMPDIR/{APK_NAME}"

chmod u+x "$MODPATH/uninstall.sh"
cp "$MODPATH/system/system_ext/{APK_NAME}/{APK_NAME}.apk" "$TMP_FILE"

pm install -r "$TMP_FILE"
rm -f "$TMP_FILE"

pm grant "{PKG_NAME}" android.permission.READ_PHONE_STATE