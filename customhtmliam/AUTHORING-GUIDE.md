# Authoring a Custom-HTML In-App Card (for humans and LLMs)

This guide tells you how to write the **HTML** for a Custom HTML in-app message so it renders in
the transparent full-screen overlay and talks to Marketing Cloud through the JavaScript bridge. **No
Kotlin changes are ever needed** — a new card (rating, survey, promo, NPS, consent, …) is authored
entirely in HTML against the six generic bridge functions, and its **placement (bottom/top/left/right band, a corner, centered, or full-page) is chosen in the message *title*** — see 'Title-driven placement' below — while the card's CSS positions content *within* that region.

> **New here?** The system architecture, the display-count/re-show fix, and the security model live
> in `README.md`. This file is only about *writing the card*.

---

## What you are producing

**One self-contained HTML document, as a single string, that goes in the in-app message `body`.**

- **Hard size limit: under 2,000 characters** (the Marketing Cloud "Advanced Edition / MCN"
  Message-attribute cap). Stay under it and the same card works on every MC edition. Cards this
  small are easy — the tested rating card is ~1,570 chars.
- Everything is inline: one `<style>` block, one `<script>` block, no external CSS/JS/font files,
  no frameworks. The page loads with an **opaque origin** (`about:blank`), so there is no
  `file://` / app access and remote `<script src>` should be avoided.
- If your card genuinely cannot fit in 2,000 chars, host it on a trusted HTTPS page (e.g. a
  CloudPage) and switch the component to `ContentSource.MESSAGE_BODY_URL` — then the body holds
  only the URL. See `README.md`. **Default is inline HTML; design for < 2,000.**

---

## The bridge — the entire contract

The overlay injects a global JS object named **`CustomHtmlBridge`**. Your HTML calls its methods.
That is the whole API:

| Call | Terminal? | What it does |
|---|---|---|
| `_display()` | no | Optional "I'm visible" echo. Display is auto-recorded on attach, so you rarely need this. |
| `_click(label?)` | **yes** | Records a **button-click** completion, then **closes** the card. `label` is an optional string. |
| `_dismiss()` | **yes** | Records a **user-dismiss** completion, then **closes** the card. |
| `_open(url)` | no | Opens `url` via the SDK's own URL handler (deep link / CloudPage / web). Does **not** close — pair with `_click()`/`_dismiss()`. |
| `_track(name, attrsJson?)` | no | Sends an MC **custom event** named `name`. `attrsJson` is a **flat JSON object of string values**, e.g. `'{"rating":"4"}'`. `messageId` is injected natively (you cannot set or spoof it). Nested objects/arrays and malformed JSON are dropped; the event still sends. |
| `_log(msg)` | no | Writes `msg` to Logcat (tag `CustomHtmlIam`). Never raises an event. Use for debugging only. |

**Terminal = closes the card.** Exactly one terminal call fires per interaction. Non-terminal calls
(`_track`, `_open`, `_display`, `_log`) leave the card up so you can compose them — the standard
pattern is *track, then close*: `b._track('iam_rating','{"rating":"4"}'); b._click('rating')`.

### Always start the script with the browser-safe shim

`CustomHtmlBridge` only exists inside the app. The shim gives every method a no-op fallback so the
same HTML also previews harmlessly in a desktop browser and never throws if the bridge is absent.
**Call the local `b`, never `CustomHtmlBridge` directly:**

```js
var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};
```

---

## Hard rules (an LLM must follow all of these)

1. **Output one HTML document, nothing else.** No markdown fences, no prose, no `\n` escaping — the
   raw string is pasted straight into the MC message body.
2. **Under 2,000 characters, total.** Minify: no comments, single-quote or unquoted HTML attrs,
   short class names, no whitespace between tags.
3. **`<meta viewport>` is required** so it renders at device width:
   `<meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no">`.
4. **Transparent page.** Always keep `html,body{margin:0;background:transparent}`. The native
   overlay view is sized and positioned by the message **title** (see "Title-driven placement");
   your CSS lays the card out *inside* that region. Only the pixels your card paints are opaque.
   A `full` placement covers the whole screen (modal); a band/box/corner leaves the rest of the
   app live and tappable.
5. **Include the shim** (above) as the first statement in `<script>`, and call `b._...`.
6. **Every path must end in exactly one terminal call** (`_click` or `_dismiss`). A card with no
   terminal call can never close. Two terminal calls in one handler is a bug.
7. **`_track` attributes are flat string→string only.** Send `{"rating":"4"}`, not `{"rating":4}`
   or nested objects. Do not put `messageId` in attrs — it is added natively.
8. **No external resources, no network** (unless you deliberately use `_open` for navigation). No
   analytics scripts, no CDN fonts — use the system stack
   `font-family:-apple-system,Roboto,sans-serif`.
9. **Escape quotes inside inline `onclick` JSON.** Prefer building JSON with `JSON.stringify(...)`
   in the script over hand-writing quotes in an attribute. If you must inline it, use `&quot;`.

---

## Title-driven placement (where the card sits)

**Placement is set in the message *title*, not in Kotlin and not only in CSS.** The title grammar is:

```
chtml:<anchor>[-<size>][:<name>]
```

- **`chtml:`** — the classifier prefix (unchanged). Everything after it is the optional placement.
- **`<anchor>`** — one of: `top` `bottom` `left` `right` `center` `full`
  `top-left` `top-right` `bottom-left` `bottom-right`. Omit it (e.g. `chtml:rating`) → **full-screen**.
- **`<size>`** — how big the native view is (and thus how much of the screen the card can eat taps in):
  - **Edges** (`top`/`bottom`/`left`/`right`) take a **single value** = band thickness
    (height for top/bottom, width for left/right). Omit it → a default 220dp band.
  - **Corners and `center`** take a **`WxH` box** (e.g. `320x180`). A single value `N` → an `N×N`
    square. Omit it → falls back to full-screen.
  - each number is `<n>` | `<n>dp` | `<n>px` | `<n>%`. Bare/`dp`/`px` are all **dp** (≈ CSS px here);
    `%` is a fraction of the screen.
- **`<name>`** — optional trailing label (e.g. `:rating`) to tell duplicate messages apart in MC.
  The SDK ignores it.

**Non-blocking rule:** Android dispatches touches by view bounds, *not* pixel transparency — so a
view larger than the card eats taps in its transparent area. Sizing the view to the card (a band or
box) is what keeps the rest of the app usable. `full` is deliberately modal.

**Anything unparseable falls back to full-screen** — never native, never a crash. A wrong number or
anchor just means a clipped card or a modal, so double-check the title.

### Title examples

| Title | Result |
|---|---|
| `chtml:rating` | full-screen (legacy/default); card CSS decides where it paints |
| `chtml:bottom-180px:rating` | 180dp bottom band, rest of app live |
| `chtml:top-64px:banner` | 64dp top banner |
| `chtml:center-320x180:consent` | 320×180dp centered box |
| `chtml:bottom-right-320x140:toast` | 320×140dp box pinned bottom-right |
| `chtml:left-40%` | left band 40% of screen width |
| `chtml:full` | full-screen takeover (modal) |

### CSS still positions *within* the region

Inside the sized native view, lay the card out with CSS. For a **band**, fill the view and pad:

```css
html,body{margin:0;background:transparent}
.wrap{position:fixed;inset:0;display:flex;align-items:flex-end;padding:14px}
```

For a **centered box** or **corner**, center the card inside the view:

```css
html,body{margin:0;background:transparent}
.wrap{position:fixed;inset:0;display:flex;align-items:center;justify-content:center}
```

For a **full-screen** card that should still feel non-modal, add a translucent full-bleed scrim with
`onclick="b._dismiss()"` so a tap outside the card closes it (taps are otherwise swallowed):

```css
.scrim{position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:20px}
.card{width:100%;max-width:340px}
```
```html
<div class=scrim onclick="b._dismiss()"><div class=card onclick="event.stopPropagation()">…</div></div>
```

---

## Minimal skeleton (620 chars — copy, then fill the card)

```html
<!DOCTYPE html><html><head><meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no"><style>html,body{margin:0;background:transparent}.band{position:fixed;left:0;right:0;bottom:0;padding:24px 14px 14px}.card{position:relative;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:16px;font-family:-apple-system,Roboto,sans-serif}</style></head><body><div class=band><div class=card><!-- your content + onclick="b._..." --></div></div><script>var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};</script></body></html>
```

---

## Example 1 — Rating card (~1,570 chars, tested end-to-end)

Tap a star → sends the score as the `iam_rating` custom event → records a click completion and
closes. The ✕ records a dismiss and closes.

```html
<!DOCTYPE html><html><head><meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no"><style>html,body{margin:0;background:transparent}.band{position:fixed;left:0;right:0;bottom:0;padding:24px 14px 14px}.card{position:relative;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:16px;font-family:-apple-system,Roboto,sans-serif}.t{font-size:18px;font-weight:700;color:#14351f}.s{font-size:14px;color:#666;margin:2px 0 12px}.stars{display:flex;gap:8px}.star{font-size:30px;color:#ccc;cursor:pointer}.star.on{color:#f5a623}.x{position:absolute;top:-16px;right:8px;width:36px;height:36px;border-radius:50%;background:#fff;box-shadow:0 2px 8px rgba(0,0,0,.2);border:0;font-size:18px;color:#555}</style></head><body><div class=band><div class=card><button class=x onclick="b._dismiss()">&times;</button><div class=t>Rate the driver</div><div class=s>Cisauk Train Station</div><div class=stars id=stars><span class=star data-v=1>&#9733;</span><span class=star data-v=2>&#9733;</span><span class=star data-v=3>&#9733;</span><span class=star data-v=4>&#9733;</span><span class=star data-v=5>&#9733;</span></div></div></div><script>var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};document.querySelectorAll('.star').forEach(function(e){e.onclick=function(){var v=+e.dataset.v;document.querySelectorAll('.star').forEach(function(o){o.classList.toggle('on',+o.dataset.v<=v)});b._track('iam_rating',JSON.stringify({rating:''+v}));b._click('rating')}})</script></body></html>
```

## Example 2 — Two-button promo (~1,140 chars)

"See offer" opens a URL and records a click; "Not now" tracks the decline reason and dismisses.

```html
<!DOCTYPE html><html><head><meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no"><style>html,body{margin:0;background:transparent}.band{position:fixed;left:0;right:0;bottom:0;padding:24px 14px 14px}.card{position:relative;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:16px;font-family:-apple-system,Roboto,sans-serif}.t{font-size:18px;font-weight:700}.s{font-size:14px;color:#666;margin:2px 0 12px}.row{display:flex;gap:10px}button{flex:1;padding:12px;border-radius:10px;border:0;font-size:15px}.p{background:#1a73e8;color:#fff}.g{background:#eee;color:#333}</style></head><body><div class=band><div class=card><div class=t>Weekend offer</div><div class=s>20% off your next ride</div><div class=row><button class=g onclick="b._track('promo_declined','{&quot;reason&quot;:&quot;not_now&quot;}');b._dismiss()">Not now</button><button class=p onclick="b._open('https://example.com/promo');b._click('open_promo')">See offer</button></div></div></div><script>var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};</script></body></html>
```

## Example 3 — Centered consent dialog with scrim (~1,310 chars)

A **centered** modal (not bottom-anchored) demonstrating flexible placement: a dim full-screen scrim
centers the card; tapping the scrim dismisses, tapping the card does not (`event.stopPropagation()`).
Each button tracks the choice and then closes.

```html
<!DOCTYPE html><html><head><meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no"><style>html,body{margin:0;background:transparent}.scrim{position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:20px;font-family:-apple-system,Roboto,sans-serif}.card{width:100%;max-width:340px;background:#fff;border-radius:16px;padding:20px;box-shadow:0 12px 32px rgba(0,0,0,.3)}.t{font-size:19px;font-weight:700}.s{font-size:14px;color:#666;margin:6px 0 16px}.row{display:flex;gap:10px}button{flex:1;padding:12px;border-radius:10px;border:0;font-size:15px}.p{background:#1a73e8;color:#fff}.g{background:#eee;color:#333}</style></head><body><div class=scrim onclick="b._dismiss()"><div class=card onclick="event.stopPropagation()"><div class=t>Enable notifications?</div><div class=s>Get ride updates and offers. You can turn this off anytime.</div><div class=row><button class=g onclick="b._track('consent','{&quot;choice&quot;:&quot;no&quot;}');b._dismiss()">Not now</button><button class=p onclick="b._track('consent','{&quot;choice&quot;:&quot;yes&quot;}');b._click('allow')">Allow</button></div></div></div><script>var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};</script></body></html>
```

**To make a different card** (NPS, feedback…), keep the skeleton + shim, set the placement in the
message title and use a CSS wrapper from "Title-driven placement", and change only the content and
the `_track`/`_open`/`_click`/`_dismiss` calls. E.g. NPS: `b._track('nps_score','{"score":"9"}');b._click('nps')`.

---

## Ready-to-paste LLM prompt

Give an LLM the block below plus a one-line description of the card you want. It will emit a
paste-ready body.

```
You generate ONE self-contained HTML document for a Salesforce Marketing Cloud custom-HTML
in-app message. Output ONLY the raw HTML — no markdown fences, no commentary.

HARD RULES:
- Under 2000 characters total. Minify: no comments, unquoted/short attrs, no inter-tag whitespace.
- Include: <meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no">
- Always keep html,body{margin:0;background:transparent}. The native view is sized+positioned by
  the message TITLE (grammar: chtml:<anchor>[-<size>][:name]); your CSS lays the card out INSIDE it.
  Fill the view and align the card, e.g.:
    band/corner/center: .wrap{position:fixed;inset:0;display:flex;align-items:center;justify-content:center;padding:14px}
    full-screen scrim:  .scrim{position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:20px} + inner .card{max-width:340px};
                        put onclick="b._dismiss()" on .scrim and onclick="event.stopPropagation()" on .card
  ALSO OUTPUT, on a line before the HTML, the recommended message TITLE, e.g.:
    TITLE: chtml:bottom-180px:rating   (bottom band)   |   chtml:center-320x180:consent (centered box)
- Inline <style> and <script> only. No external CSS/JS/fonts/frameworks/network.
  Use font-family:-apple-system,Roboto,sans-serif.
- First line of <script> MUST be this shim, and you MUST call b._... (never CustomHtmlBridge):
  var N=function(){},b=window.CustomHtmlBridge||{_display:N,_click:N,_dismiss:N,_open:N,_track:N,_log:N};

BRIDGE API (the only way to talk to the app):
- b._click(label?)   TERMINAL: records a click completion, then closes the card.
- b._dismiss()       TERMINAL: records a user-dismiss completion, then closes the card.
- b._open(url)       opens a URL via the SDK; does NOT close (pair with a terminal call).
- b._track(name, attrsJson?)  sends a custom MC event. attrsJson is FLAT string->string JSON,
  e.g. '{"rating":"4"}'. Do NOT include messageId (added natively). Use JSON.stringify(...).
- b._log(msg)        Logcat only; debugging.

RULES OF USE:
- Every interaction path ends in EXACTLY ONE terminal call (_click or _dismiss).
- Standard pattern is track-then-close: b._track(...); b._click('...').
- _track values are strings only ("4", not 4). No nested objects/arrays.

CARD TO BUILD: <describe the card: title, subtitle, buttons/inputs, which event name +
attributes each control should send, and what closes it>
```

---

## Pre-flight checklist (before pasting into MC)

- [ ] **< 2,000 characters.** Check: `wc -c card.html` (aim well under; leave room for copy edits).
- [ ] `<meta viewport>` present; `html,body{background:transparent}`; the card's CSS fills the
      native region and positions the card inside it (see "Title-driven placement").
- [ ] Script starts with the shim; all calls use `b._...`.
- [ ] Every button/handler ends in exactly one terminal call (`_click` or `_dismiss`).
- [ ] `_track` attrs are flat string→string; no `messageId` key; built with `JSON.stringify`.
- [ ] No external resources / network (unless intentionally using `_open`).
- [ ] Message **title** = `chtml:<anchor>[-<size>][:name]` — the prefix is required so the component
      picks the message up, and the anchor/size choose placement (e.g. `chtml:bottom-180px:rating`,
      `chtml:center-320x180:consent`). Omit the anchor for a full-screen card. (Prefix set at `init`.)

## Verify on device (debug build)

After sending a test, confirm in Logcat (tag filter `CustomHtmlIam` / `TriggerManager` /
`IamCompleted`):

- `_track` fired: `TriggerManager: (iam_rating) event logged with attributes {messageId=…, rating=4}`
  — note `messageId` is auto-injected and your attrs are merged.
- terminal fired: `Publish IamCompletedEvent …` and `closed with action InAppMessageCloseAction`.
- the overlay is torn down (card disappears) and, thanks to display counting, the message does
  **not** re-show on the next app open once its `displayLimit` is reached.
