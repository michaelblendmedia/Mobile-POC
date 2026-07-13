# Custom HTML In-App Message (IAM) for Marketing Cloud — Android

A self-contained, drop-in package that renders **custom HTML** Marketing Cloud in-app messages
as a **transparent, non-blocking overlay** and lets the HTML drive the native IAM lifecycle and
emit Marketing Cloud analytics through a small JavaScript bridge.

Marketing Cloud's Unified Mobile SDK (v11) has **no native custom-HTML IAM support**. This package
fills that gap: it registers a single `InAppMessageManager.EventListener`, classifies which
messages are "custom HTML", suppresses the SDK's native render for those, self-renders your HTML in
a WebView overlay, and forwards non-custom messages to your existing listener untouched.

**The component is use-case agnostic and detached from any marketing activity.** It ships only
generic bridge functions (`_display`, `_click`, `_dismiss`, `_open`, `_track`, `_log`). A concrete
card — a rating prompt, a survey, a promo, an NPS ask — is authored **entirely in HTML** against
those functions; **no Kotlin change is needed to add a new use case**. The bundled sticky
"rate the driver" card (`rating.html`) is just one **example** of that pattern: it calls
`_track('iam_rating', '{"rating":"5"}')` then `_click('rating')` — nothing in this package knows
what a "rating" is.

## How it's packaged

This is a **self-contained Android library module** (`:customhtmliam`, `com.android.library`).
Every type lives under `com.sfmc.customhtmliam` with **zero compile-time references to any host app
code**. The only seam back to the app is a `delegate: InAppMessageManager.EventListener?` parameter,
which is an SDK type. Everything an integrator used to copy by hand now ships inside the module:

- the R8/ProGuard keep rule for the JS bridge ships via `consumer-rules.pro` (see
  `consumerProguardFiles` in `build.gradle`) and is applied to the host app automatically — no rule
  to copy;
- the `<uses-permission android:name="android.permission.INTERNET" />` needed for remote HTML fetch
  is declared in the module manifest and **merges into the host automatically**;
- the example card templates ship as module assets under `src/main/assets/customhtmliam/`.

Because it's a module, the MC SDK is a `compileOnly` dependency here: the library compiles against
the SDK your app already provides, without bundling a second copy or pinning the version for you.

**To embed it in a fresh or existing MC-SDK app, follow [`INTEGRATION.md`](INTEGRATION.md)** — the
short version is: add the module to your build (`include ':customhtmliam'` +
`implementation project(':customhtmliam')`), then call `CustomHtmlIam.init(...)` once after
`SFMCSdk.configure(...)`, passing your existing IAM listener as `delegate`.

## Wiring (one call)

Call this once at startup, **after** the SDK is configured. Because the SDK has a single listener
slot with no getter, pass your app's existing `InAppMessageManager.EventListener` as `delegate` so
its behavior (e.g. suppression logic) is preserved for non-custom messages:

```kotlin
CustomHtmlIam.init(
    application = this,
    config = CustomHtmlIamConfig(
        matcher = CustomHtmlMatcher.byTitlePrefix("chtml:"),      // or .byIds("id1", "id2")
        contentSource = ContentSource.MESSAGE_BODY_HTML,          // OPTION 1 (see below)
        contentUrl = "https://your-trusted-host/card.html",       // optional fallback; see SECURITY
        debugLogging = BuildConfig.DEBUG,                         // body-content diagnostics; debug only
    ),
    delegate = yourExistingInAppMessageListener,                  // null if you have none
)
```

- **Matcher** — `byTitlePrefix(prefix)` matches on the message title's display text; `byIds(vararg)`
  matches on message id. On any classification doubt the message is treated as **not** ours and
  renders normally.
- **Content source (where the HTML comes from)** — both options read the in-app message's own
  **`body` attribute**; switch modes with the single `contentSource` value at init time:

  | `contentSource` | The message `body` holds | Behavior |
  |---|---|---|
  | `ContentSource.MESSAGE_BODY_HTML` (default) | the raw HTML | rendered **verbatim**, no network |
  | `ContentSource.MESSAGE_BODY_URL` | a URL (e.g. a CloudPage) | the URL is **fetched over HTTPS**, then rendered |

- **Content resolution order** — message-body content (per `contentSource` above) → fixed
  `contentUrl` (fetched verbatim, if set) → `"$contentBaseUrl$messageId.html"` (if set,
  timeout-bounded) → bundled asset `customhtmliam/$messageId.html` → bundled asset
  `customhtmliam/rating.html` → nothing. `contentUrl`/`contentBaseUrl` are optional fallbacks used
  only when the message body yields nothing usable.
- **Custom events** — the page emits Marketing Cloud custom events by name via the JS
  `_track(name, attrsJson)` bridge; `messageId` is always injected natively (and cannot be spoofed).
  This is the **only** event path in the package — there is no built-in "rating" event. A rating
  card simply calls `_track('iam_rating', '{"rating":"5"}')`; a survey card calls
  `_track('survey_answer', …)`; and so on. See "JavaScript bridge" below.
- **Display counting** — because the overlay suppresses the SDK's native render, the package
  explicitly reports the display and its completion back to the SDK so `displayLimit` / frequency
  caps work and Journey analytics fire. See "Display counting & why a message must be 'completed'
  back to the SDK" below — read it before changing lifecycle code.

## Display counting & why a message must be "completed" back to the SDK

**This section documents SDK-internal behavior we reverse-engineered — it is not in Salesforce's
public docs. Read it before touching the lifecycle code.**

### The bug it explains

Symptom: a custom-HTML message with a **display limit** (e.g. `displayLimit: 1`) or a
**frequency cap** (`timeBetweenDisplaySec`) **re-appeared on every app open** (`$appOpen`), no matter
how many times it was already shown or dismissed.

### Root cause — how the native renderer records a display

The SDK's own in-app renderer (`RealIamComponent`, decompiled from
`inappmessagingfeaturemodule-1.0.1`) records a display/completion through **two internal calls that
its own IAM Activity makes** — not through the public listener callbacks. The real lifecycle is:

| Native step | Who calls it | What it does |
|---|---|---|
| `shouldShowMessage(msg)` | trigger engine → your listener | Gate. If it returns `true`, the SDK does `startActivity(<IAM Activity>)`. **Returning `false` stops here — nothing below runs.** |
| `canDisplay(msg)` | the IAM Activity, on show | **Records the display**: `storage.incrementDisplayCount(msg)`, publishes the `IamDisplayed` analytics event, updates throttles, notifies `didShowMessage`, and marks `msg` as the in-flight message. Idempotent for the same message; returns `false` only when a *different* message is already live. |
| `handleMessageFinished(msg, MessageCompletedEvent)` | `MessageHandler.messageFinished`, on dismiss/click/auto | **Records completion**: publishes the `IamCompleted` analytics event (with display duration), builds the `InAppMessageCloseAction`, notifies `didCloseMessage`, and clears the in-flight slot. |

The count that `displayLimit` / frequency capping is evaluated against is the **persisted
`displayCount`**, and the only thing that increments it is `canDisplay(...)`. Because this package
returns **`false`** from `shouldShowMessage` (to suppress the native UI and render our own overlay),
the SDK never launches its Activity, so **`canDisplay` is never called, `displayCount` stays `0`, and
the cap never trips** → the message re-fires on every trigger.

### The fix — replay the two internal calls ourselves

`getInAppMessageManager()` returns an `IamComponent`, which implements the **public**
`InternalIamManager` interface exposing both `canDisplay(InAppMessage)` and
`handleMessageFinished(InAppMessage, MessageCompletedEvent)`. `IamLifecycleReporter` casts to it
(guarded — if a future SDK breaks the cast we log once and degrade gracefully) and:

1. calls `recordDisplayed(msg)` **the moment our overlay actually attaches** (native parity with
   `canDisplay`), and
2. calls `recordCompleted(msg, reason, …)` **on the terminal bridge event** (native parity with
   `handleMessageFinished`), mapping the reason to the SDK's own
   `MessageCompletedEvent.userDismissed / buttonClicked / autoDismissed / unknown` factories.

**These two are a balanced pair** — `canDisplay` sets the SDK's in-flight `currentMessage`,
`handleMessageFinished` clears it. We only record a completion when the matching display was actually
recorded, so we never release a slot we don't own (which would clobber another live message). Net
effect: a self-rendered overlay is counted, capped, and reported to Journey analytics **exactly like
a native in-app message.**

> Caveat: this depends on `getInAppMessageManager()` returning an `InternalIamManager`
> (true in SDK v11 / feature module 1.0.1). It is an SDK-internal contract, not a public guarantee —
> if a future SDK changes it, capping silently reverts to the old "re-shows" behavior (logged at
> WARN), and you should re-verify against the new `RealIamComponent`.

## JavaScript bridge (`CustomHtmlBridge`)

The overlay injects a JS object named **`CustomHtmlBridge`** into the page. Your HTML calls its
methods to drive the native lifecycle above and to talk to Marketing Cloud. **This is the entire
contract** — the whole surface is these six generic functions, none of them named for a particular
use case. Any card (rating, survey, promo, NPS, …) is authored purely in HTML against them, with
zero Kotlin changes:

| JS call | Terminal? | Effect |
|---|---|---|
| `CustomHtmlBridge._display()` | no | Explicit "I'm on screen" echo. Display is **auto-recorded** when the overlay attaches, so this is optional/idempotent — use it only if your card decides visibility itself. |
| `CustomHtmlBridge._click(label?)` | **yes** | Records a **button-click** completion (analytics parity), then dismisses the overlay. Optional `label` string is used as the click label. |
| `CustomHtmlBridge._dismiss()` | **yes** | Records a **user-dismiss** completion, then dismisses the overlay. |
| `CustomHtmlBridge._open(url)` | no | Opens `url` through the SDK's own `UrlHandler` (same path a native IAM button uses for deep link / CloudPage / web URL). Does **not** dismiss — pair with `_click()`/`_dismiss()` if you want the card to close. |
| `CustomHtmlBridge._track(name, attrsJson?)` | no | Sends an arbitrary MC **custom event** named `name`. `attrsJson` is a **flat JSON object of string values**, e.g. `'{"choice":"later"}'`; `messageId` is always injected natively (and cannot be overwritten by the page). Nested objects/arrays and malformed JSON are dropped; the event still sends. |
| `CustomHtmlBridge._log(msg)` | no | Writes `msg` to Logcat (`CustomHtmlIam` tag). Never raises an event. |

**Terminal vs non-terminal:** a *terminal* call records completion and tears the card down; a
*non-terminal* call (`_track`, `_open`, `_display`, `_log`) leaves the card up so you can compose
them (e.g. `_track('cta_shown')` on render, then `_open(url); _click('cta')` on tap).

**Browser-safe shim** (so the same HTML previews in a desktop browser where `CustomHtmlBridge` is
absent) — put this at the top of your card's script:

```js
var b = window.CustomHtmlBridge || {
  _display:function(){}, _click:function(){}, _dismiss:function(){},
  _open:function(){}, _track:function(){}, _log:function(){}
};
// then call b._click('cta'), b._track('cta_shown','{"variant":"a"}'), etc.
```

**Example — a two-button card:**

```html
<button onclick="b._open('https://example.com/promo'); b._click('open_promo')">See offer</button>
<button onclick="b._track('promo_declined','{\"reason\":\"not_now\"}'); b._dismiss()">Not now</button>
```

### Example use case: the rating card (pure HTML, no Kotlin)

The bundled `rating.html` demonstrates how a real use case is built with only the generic bridge —
it is **not** special-cased anywhere in Kotlin. On a star tap it sends the score as a custom MC event
and then records a click-through completion (which also tears the card down):

```js
b._log('rating tapped: ' + v);
b._track('iam_rating', JSON.stringify({ rating: String(v) }));  // custom MC event, messageId auto-injected
b._click('rating');                                             // terminal: completes + dismisses
```

To add a different use case (e.g. an NPS ask), author a new HTML card that calls
`b._track('nps_score', '{"score":"9"}'); b._click('nps')` — no change to this package.

### Message `body` attribute size limit

The two options above put content into the in-app message **`body` attribute**. Limits differ by
Marketing Cloud edition:

- **Marketing Cloud Engagement (MCE):** the effective limit is high — verified live to accept and
  deliver a message body of **at least 64,000 characters intact** (no truncation, no HTML escaping)
  on SDK v11 / Android. This is comfortably large for a full inline HTML card (Option 1).
- **Marketing Cloud on Salesforce Platform (MCN / "Marketing Cloud — Advanced Edition"):** the
  Message attribute is capped at **2,000 characters**. A full inline HTML card easily exceeds this,
  so on MCN prefer **Option 2 (`MESSAGE_BODY_URL`)** — the body holds only a short URL and the HTML
  is hosted externally (e.g. a CloudPage), keeping you well under the 2,000-char cap.

> These are platform-side limits, not guarantees in the SDK contract. Design for the **2,000-char
> MCN cap** if you need cross-edition portability, and never author a body right at whatever ceiling
> you measure.

## Required project config

Consuming the module as a Gradle dependency, **you do not have to add any of the following by hand**
— they ship with the library. This section documents what the module already provides so you can
verify (or reproduce it if you vendor the sources instead of the module).

**R8 / ProGuard** — the JS bridge is only ever called from JavaScript, so R8 would strip it. This
rule ships in the module's `consumer-rules.pro` (via `consumerProguardFiles`) and is applied to the
host app's R8 pass automatically:

```proguard
-keep class com.sfmc.customhtmliam.CustomHtmlBridge { *; }
-keepclassmembers class com.sfmc.customhtmliam.CustomHtmlBridge {
    @android.webkit.JavascriptInterface <methods>;
}
```

**INTERNET permission** — remote HTML fetch (Option 2 / fallback URLs) needs
`android.permission.INTERNET`. The module's `src/main/AndroidManifest.xml` declares it, and Android's
manifest merger folds it into the host app automatically — so it works even in a host whose own
manifest omits it. (Option 1, inline HTML, needs no network at all.)

**Unit tests** — the module's own `build.gradle` sets `testOptions { unitTests.returnDefaultValues =
true }` (so `android.util.Log` / stubbed `org.json` don't blow up the pure-JVM unit tests) and puts a
real `org.json` on the test classpath. This is internal to the module; a consuming app needs it only
if the app's *own* unit tests exercise this code.

## SECURITY — read before choosing a content source

The overlay WebView has `javaScriptEnabled = true` and a native bridge (`CustomHtmlBridge`)
attached, so **any HTML you load can call every bridge method** (see the JavaScript-bridge table
above — `_open`, `_track`, `_click`, `_dismiss`, etc.). Hardening in place: the bridge surface is
small and each method's authority is bounded (`_open` routes through the SDK's own `UrlHandler`,
which already gates schemes and is `null`-safe; `_track` attributes are parsed as a **flat
string→string** map only, dropping nested/malformed JSON; **`messageId` is captured natively and
cannot be set or overwritten from JS**, so a page can neither spoof which message it acts on nor
forge that key inside a tracked event; `_log` only writes to Logcat). Content loads with a **null
base URL** (opaque origin — no `file://` / same-origin app access), and file/content access are
explicitly disabled.

Because the bridge is fully generic, `_open` can launch a URL and `_track` can emit a custom event
of **any name**. Both are bounded by the trust boundary below (only HTML you author runs here), but
if you host content externally, treat that origin as able to open URLs and send analytics events on
the user's behalf.

The remaining trust boundary is **whatever produces the HTML**, so treat every source as trusted:

- `MESSAGE_BODY_HTML` — the HTML comes from the in-app message body, i.e. from anyone who can author
  messages in your MC org. Enable custom HTML only for messages you control.
- `MESSAGE_BODY_URL` / `contentUrl` / `contentBaseUrl` — use a **trusted HTTPS origin only**. On API
  28+ cleartext (`http://`) is blocked by default, so an `http://` URL fails the fetch and falls
  back to the next source in the resolution order.

## The overlay

Permission-free: attached to the Activity's `android.R.id.content` (no `SYSTEM_ALERT_WINDOW`). The
WebView is a **transparent canvas sized to the card by the message title** — the grammar
`chtml:<anchor>[-<size>][:name]` (see `AUTHORING-GUIDE.md`) picks a bottom/top/left/right band, one
of four corners, a centered box, or a full-screen takeover, and `OverlayController` resolves it to an
Android `Gravity` + `LayoutParams` before attach. No anchor → full-screen (preserving the original
behavior). The page is transparent except where the card paints, so uncovered areas show the app
behind it. Sizing (band/box) is configurable via `defaultBandDp`; the placement prefix is
`placementPrefix` (defaults to the `chtml:` matcher prefix).

**Interaction model:** Android dispatches touches by view bounds, not pixel transparency, so a view
larger than the card eats taps in its transparent area. That is why placement sizes the native view
to the card: a band/box/corner leaves the rest of the screen live and tappable, while a `full`
placement is deliberately modal until a terminal bridge call (`_click`/`_dismiss`) tears it down. If you want a card that lets the
user keep tapping the app around it, keep the painted card small and give the surrounding
transparent area an explicit dismiss affordance (e.g. a translucent full-bleed scrim element with
`onclick="b._dismiss()"`), so a tap outside the card closes the overlay rather than being silently
swallowed. One overlay shows at a time; a message that arrives while backgrounded is queued and
flushed on the next resume within a TTL (default 30s).

## Files

| File | Responsibility |
|------|----------------|
| `CustomHtmlIam.kt` | Entry-point facade — `init(application, config, delegate?)` wires everything. |
| `CustomHtmlIamConfig.kt` | Config + `ContentSource` (body-HTML vs body-URL) + `CustomHtmlMatcher` (`byTitlePrefix` / `byIds`). |
| `Classifier.kt` | Null/exception-safe "is this ours?" decision + `bodyText(message)` reader. |
| `ContentProvider.kt` | Resolves HTML: inline body → override/fixed URL → base-url-by-id → asset. Timeout-bounded, cancellation-safe. |
| `OverlayController.kt` | Transparent WebView overlay lifecycle; resolves a `Placement` (title-driven) to `Gravity` + `LayoutParams`; fires `onShown` on attach; tears down on terminal bridge events. |
| `Placement.kt` | Pure-Kotlin title grammar `chtml:<anchor>[-<size>][:name]` → `Placement` (anchor + width/height); total fallback to full-screen. |
| `OverlayState.kt` | One-at-a-time + TTL pending-queue state machine. |
| `ForegroundActivityTracker.kt` | Tracks the current Activity for attach + resume flush. |
| `CustomHtmlBridge.kt` | Generic `@JavascriptInterface` bridge exposed to page JS as `CustomHtmlBridge`; defines `BridgeEvent` + `terminal`. Use-case agnostic. |
| `CustomEventSink.kt` | `MarketingCloudCustomEventSink` backs the JS `_track` (flat string→string attrs, `messageId` auto-injected); `EventTracker`/`SfmcEventTracker` are the SDK seam. |
| `IamLifecycleReporter.kt` | Replays the SDK's native display/completion bookkeeping (`canDisplay` / `handleMessageFinished`) so custom overlays are counted & capped; also opens URLs via the SDK `UrlHandler`. |
| `IamEventListener.kt` | The single SDK listener; composes your `delegate`; records display on show and completion on terminal events. |
| `src/main/assets/customhtmliam/rating.html` | Example sticky rating card — a use case built purely in HTML on the generic bridge. Other example cards (`consent-centered`, `promo-two-button`, `rating-card-compact`, `_skeleton`) live alongside it. |
