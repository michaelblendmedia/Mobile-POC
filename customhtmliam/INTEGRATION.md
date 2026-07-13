# Embedding the `customhtmliam` module

This guide gets the Custom-HTML IAM overlay running in **your** app. It covers two starting points:

- **[Path A — a fresh Marketing Cloud SDK app](#path-a--fresh-mc-sdk-app)** (you're wiring up the MC
  Unified Mobile SDK now, or you have it configured but no in-app-message listener yet).
- **[Path B — an existing MC SDK app](#path-b--existing-mc-sdk-app)** (you already call
  `SFMCSdk.configure(...)` and you already register an `InAppMessageManager.EventListener` you must
  not lose).

Both paths end in the **same one call** — `CustomHtmlIam.init(...)`. The only difference is what you
pass as its `delegate`.

For *what the component does*, the display-count fix, the security model, and the full
`CustomHtmlBridge` contract, see [`README.md`](README.md). For *how to write the HTML card itself*,
see [`AUTHORING-GUIDE.md`](AUTHORING-GUIDE.md). This file is only about *wiring the module into a
build*.

---

## Prerequisites

The module is a thin overlay on top of the MC SDK. It does **not** bundle the SDK — your app must
already depend on it. Verified against:

| Thing | Value |
|---|---|
| MC Unified Mobile SDK (`marketingcloudsdk`) | `11.0.+` |
| In-app messaging feature (pulled in transitively / `mobileappmessagingsdk`) | `1.1.+` |
| Kotlin | `2.2.21` |
| `compileSdk` / `targetSdk` | `36` |
| `minSdk` | `26` |
| JVM target | `17` |
| Android Gradle Plugin | 8.10.x (Gradle 8.10.1) |

The MC SDK is served from Salesforce's Maven repo — make sure it's in your `build.gradle`
`repositories` (top level or `settings.gradle` `dependencyResolutionManagement`):

```groovy
maven { url "https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository" }
```

> **Compatibility note.** The display-counting fix casts `getInAppMessageManager()` to the SDK's
> internal `InternalIamManager` (true on SDK v11 / feature module 1.0.1). It's guarded — if a future
> SDK breaks the cast the module logs once at WARN and degrades gracefully (capping reverts to the
> old behavior). See the "Display counting" section of [`README.md`](README.md) before bumping the
> SDK major version.

---

## Step 1 — add the module to your build

You have two ways to consume the module. **Source module** is recommended (you get the assets, can
step into the code, and the versions stay in lock-step with your app). **AAR** is for teams that
can't add another source module.

### Option 1 (recommended): as a source module

1. Copy the whole `customhtmliam/` directory into your project root (a sibling of your `app/`
   module).
2. Register it in your **`settings.gradle`**:

   ```groovy
   include ':app'
   include ':customhtmliam'
   ```
3. Depend on it from your app module's **`app/build.gradle`**:

   ```groovy
   dependencies {
       // Custom HTML in-app message overlay (self-contained library module). See customhtmliam/README.md.
       implementation project(':customhtmliam')
       // ... your existing MC SDK deps stay as they are ...
   }
   ```

That's it for the build. The JS-bridge R8 keep rule (`consumer-rules.pro`) and the `INTERNET`
permission (module manifest) are pulled in automatically — **you do not touch your app's
`proguard-rules.pro` or `AndroidManifest.xml`.**

### Option 2: as a prebuilt AAR

If you'd rather ship a binary, build the AAR once and drop it in:

```bash
./gradlew :customhtmliam:assembleRelease   # or assembleDebug
# output: customhtmliam/build/outputs/aar/customhtmliam-*.aar
```

Then, in the consuming app:

```groovy
dependencies {
    implementation files('libs/customhtmliam-release.aar')

    // An AAR does NOT carry the module's compileOnly SDK dependency or its coroutines runtime dep,
    // so the CONSUMER must provide them (your app already has the MC SDK; add coroutines if absent):
    implementation "com.salesforce.marketingcloud:marketingcloudsdk:11.0.+"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.2.2"
}
```

The AAR still carries its `consumer-rules.pro` and merges its `INTERNET` permission, so R8 and the
manifest are handled for you the same way. The one thing an AAR loses is the automatic `compileOnly`
SDK linkage — hence the explicit deps above.

---

## Path A — fresh MC SDK app

Use this when you don't already register an IAM listener. Add the `CustomHtmlIam.init(...)` call to
your `Application.onCreate()`, **after** `SFMCSdk.configure(...)`:

```kotlin
import com.sfmc.customhtmliam.ContentSource
import com.sfmc.customhtmliam.CustomHtmlIam
import com.sfmc.customhtmliam.CustomHtmlIamConfig
import com.sfmc.customhtmliam.CustomHtmlMatcher

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SFMCSdk.configure(this, myConfigBuilder) { initStatus ->
            // ... your existing init handling ...
        }

        // Custom HTML IAM overlay. Registers the single InAppMessageManager listener.
        // No delegate: this app has no other IAM listener to preserve.
        CustomHtmlIam.init(
            application = this,
            config = CustomHtmlIamConfig(
                matcher = CustomHtmlMatcher.byTitlePrefix("chtml:"),   // title starts with "chtml:"
                contentSource = ContentSource.MESSAGE_BODY_HTML,       // paste HTML into the message body
                debugLogging = BuildConfig.DEBUG,                      // body diagnostics, debug builds only
            ),
        )
    }
}
```

You do **not** need to register your own `InAppMessageManager.EventListener` — the module owns the
SDK's single listener slot. Non-`chtml:` messages still render natively; the module forwards them.

That's the whole integration for a fresh app. Skip to [Verify](#verify-it-works).

---

## Path B — existing MC SDK app

The SDK exposes a **single** `InAppMessageManager` listener slot with **no getter** — calling
`setInAppMessageListener(...)` twice means the last call wins and silently replaces the earlier
listener. The module *must* own that slot (it needs `shouldShowMessage` to suppress the native render
for custom messages). So instead of registering separately, you **hand your existing listener to the
module as its `delegate`**, and the module forwards every non-custom message to it — preserving your
current behavior verbatim.

### B.1 — hoist your existing listener

If today you register an anonymous listener like this:

```kotlin
// BEFORE — your app owns the slot directly
InAppMessagingFeature.requestSdk { feature ->
    feature.getInAppMessageManager().setInAppMessageListener(
        object : InAppMessageManager.EventListener {
            override fun shouldShowMessage(message: InAppMessage): Boolean { /* your gate */ return true }
            override fun didShowMessage(message: InAppMessage) { /* your logging */ }
            override fun didCloseMessage(message: InAppMessage, action: InAppMessageCloseAction) { /* ... */ }
        }
    )
}
```

hoist that anonymous object into a named property (do **not** call `setInAppMessageListener` on it
anymore — the module will):

```kotlin
// AFTER — the same listener, hoisted, no self-registration
private val myIamListener = object : InAppMessageManager.EventListener {
    override fun shouldShowMessage(message: InAppMessage): Boolean { /* your gate */ return true }
    override fun didShowMessage(message: InAppMessage) { /* your logging */ }
    override fun didCloseMessage(message: InAppMessage, action: InAppMessageCloseAction) { /* ... */ }
}
```

### B.2 — pass it as `delegate`

```kotlin
import com.sfmc.customhtmliam.ContentSource
import com.sfmc.customhtmliam.CustomHtmlIam
import com.sfmc.customhtmliam.CustomHtmlIamConfig
import com.sfmc.customhtmliam.CustomHtmlMatcher

// After SFMCSdk.configure(...). The module registers the single listener and forwards
// non-"chtml:" messages to your delegate, so your existing behavior is preserved untouched.
CustomHtmlIam.init(
    application = this,
    config = CustomHtmlIamConfig(
        matcher = CustomHtmlMatcher.byTitlePrefix("chtml:"),
        contentSource = ContentSource.MESSAGE_BODY_HTML,
        debugLogging = BuildConfig.DEBUG,
    ),
    delegate = myIamListener,   // <-- your hoisted listener; the module forwards non-custom messages here
)
```

**What the module forwards to your delegate:**

- **`shouldShowMessage`** — forwarded **only for non-custom messages**, so your gate still decides
  whether a native message displays. A custom-HTML (`chtml:`) message bypasses your gate entirely:
  the module reserves the overlay itself and returns `false` to the SDK. Your suppression logic
  therefore runs unchanged for native messages and is simply not consulted for custom ones.
- **`didShowMessage`** / **`didCloseMessage`** — forwarded for **every** message. For a custom-HTML
  message these fire as part of the display-count bookkeeping the module replays into the SDK (see
  the "Display counting" section of [`README.md`](README.md)), so your existing show/close logging
  still sees a matched pair for custom cards, just as it does for native ones.

> If you keep any other `setInAppMessageListener(...)` call in your codebase, **remove it** — two
> registrations race and one wins. `CustomHtmlIam.init(...)` must be the only caller.

### B.3 — settings you may already have

If your app already sets IAM chrome (status bar color, typeface) inside an
`InAppMessagingFeature.requestSdk { ... }` block, leave that in place — it operates on the same
manager and is independent of the listener slot. Only the `setInAppMessageListener(...)` call moves
into the module.

---

## Choosing where the HTML comes from

One config value, `contentSource`, decides how the module reads the message body (see
[`README.md`](README.md) for the full resolution order and size limits):

| `contentSource` | The message **body** holds | Use when |
|---|---|---|
| `ContentSource.MESSAGE_BODY_HTML` *(default)* | the raw HTML, rendered verbatim, no network | your card fits the body attribute (MCE: large; **MCN: 2,000-char cap**) |
| `ContentSource.MESSAGE_BODY_URL` | a URL (e.g. a CloudPage) fetched over HTTPS | the card is too big for the body, or you want to edit it without an app release |

For `MESSAGE_BODY_URL` (or the optional `contentUrl` / `contentBaseUrl` fallbacks), point at a
**trusted HTTPS origin only** — the loaded HTML can call every bridge method. See the SECURITY
section of [`README.md`](README.md).

---

## Verify it works

1. **Build with R8** to confirm the consumer keep rule propagated:

   ```bash
   ./gradlew :app:assembleRelease          # or your minified variant
   ```

   Then confirm the bridge survived shrinking (the JS bridge is called only from JS, so a missing
   keep rule breaks it silently at runtime, not at build time):

   ```bash
   grep -n "com.sfmc.customhtmliam.CustomHtmlBridge" app/build/outputs/mapping/<variant>/seeds.txt
   ```

   You should see the class and its `_click` / `_dismiss` / `_track` / `_open` / `_display` / `_log`
   members listed.

2. **Send a test message** whose **title** starts with `chtml:` and whose **body** is a small HTML
   card (start from `src/main/assets/customhtmliam/rating.html` or `_skeleton.html`). Use the title
   grammar `chtml:<anchor>[-<size>][:name]` to place it (e.g. `chtml:bottom` for a bottom band,
   `chtml:center` for a centered box, `chtml:full` for full-screen). See
   [`AUTHORING-GUIDE.md`](AUTHORING-GUIDE.md).

3. **Trigger it** on device. The card should render as a transparent overlay, drive analytics via
   the bridge, and dismiss on a terminal call (`_click` / `_dismiss`). A non-`chtml:` message should
   still render as a normal native in-app message and reach your delegate.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Card renders in debug but is blank / bridge calls do nothing in a minified build | R8 stripped the bridge. Confirm you consume the module as a dependency (so `consumer-rules.pro` applies) — if you copied sources instead, add the keep rule from [`README.md`](README.md) manually. |
| Your app's own IAM logic (suppression, logging) stopped firing | You didn't pass `delegate`, or a stray `setInAppMessageListener(...)` elsewhere overwrote the module's listener. See [Path B](#path-b--existing-mc-sdk-app). |
| `chtml:` message shows full-screen instead of the anchor you wanted | The title placement spec didn't parse; it falls back to full-screen by design. Check the grammar in [`AUTHORING-GUIDE.md`](AUTHORING-GUIDE.md). |
| Remote (`MESSAGE_BODY_URL`) card never loads | The URL isn't HTTPS (API 28+ blocks cleartext), the host is unreachable within `fetchTimeoutMs` (default 3s), or `INTERNET` isn't merged — confirm the module's manifest is in the merged output. |
| A capped/`displayLimit` custom message re-appears every app open | The `InternalIamManager` cast failed (SDK version drift). Check Logcat for the one-time WARN from `IamLifecycleReporter`; see the display-counting section of [`README.md`](README.md). |
