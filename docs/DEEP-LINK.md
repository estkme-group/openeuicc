# {Open/Easy}EUICC Deep Link

Since: OpenEUICC v1.3.0 or EasyEUICC v1.3.0

## Web intent

HTML example:

```html
<a href="lpa:1$rsp.example.com$matching-id">Open {Open/Easy}EUICC</a>
```

## Android intent

Kotlin example:

```kotlin
val lpaUri = Uri.parse("lpa:1\$rsp.example.com\$matching-id")
startActivity(Intent(Intent.ACTION_VIEW, lpaUri))
```

Before calling `startActivity`, check whether any installed app can handle this deep link by calling [PackageManager#queryIntentActivities].

[PackageManager#queryIntentActivities]: https://developer.android.com/reference/android/content/pm/PackageManager#queryIntentActivities(android.content.Intent,%20int)

## References

- [About deep links](https://developer.android.com/training/app-links)
- [Android Intents with Chrome](https://developer.chrome.com/docs/android/intents)
