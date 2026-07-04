# {Open/Easy}EUICC Deep Link

Since: OpenEUICC v1.3.0 or EasyEUICC v1.3.0

## Web Intent

HTML example:

```html
<a href="lpa:1$rsp.example.com$matching-id">Open {Open/Easy}EUICC</a>
```

## Android Intent

Kotlin example:

```kotlin
val lpaUri = Uri.parse("lpa:1\$rsp.example.com\$matching-id")
startActivity(Intent(Intent.ACTION_VIEW, lpaUri))
```

To check whether any installed app can handle this deep link, call [PackageManager#queryIntentActivities] before calling
`startActivity`.

[PackageManager#queryIntentActivities]: https://developer.android.com/reference/android/content/pm/PackageManager#queryIntentActivities(android.content.Intent,%20int)

## References

- [About deep links](https://developer.android.com/training/app-links)
- [Android Intents with Chrome](https://developer.chrome.com/docs/android/intents)
