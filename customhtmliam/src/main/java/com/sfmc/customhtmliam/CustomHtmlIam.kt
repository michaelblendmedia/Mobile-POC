package com.sfmc.customhtmliam

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.salesforce.marketingcloud.inappmessagingfeature.InAppMessageManager
import com.salesforce.marketingcloud.inappmessagingfeature.InAppMessagingFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Single entry point. Call once, after SFMCSdk.configure(...).
 * @param delegate the app's pre-existing InAppMessageManager.EventListener, if any. Because the
 *   SDK has a single listener slot, our listener REPLACES it and forwards non-custom messages to
 *   this delegate. Pass the app's current listener here to preserve its behavior.
 */
object CustomHtmlIam {
    fun init(
        application: Application,
        config: CustomHtmlIamConfig,
        delegate: InAppMessageManager.EventListener? = null,
    ) {
        val tracker = ForegroundActivityTracker()
        application.registerActivityLifecycleCallbacks(tracker)

        val mainHandler = Handler(Looper.getMainLooper())
        val overlayState = OverlayState(config.foregroundTtlMs) { System.currentTimeMillis() }
        val overlay = OverlayController(tracker, overlayState, mainHandler)

        val classifier = Classifier(config.matcher)
        val contentProvider = ContentProvider(
            baseUrl = config.contentBaseUrl,
            timeoutMs = config.fetchTimeoutMs,
            http = UrlHtmlHttpClient(config.fetchTimeoutMs.toInt()), // align socket timeouts to the fetch budget
            assets = AndroidAssetLoader(application),
            exactUrl = config.contentUrl,
        )
        // The single event path: the JS `_track(name, attrs)` bridge. A use case (rating, survey,
        // promo, …) emits its own custom events by name from HTML; nothing here is use-case specific.
        val customEventSink = MarketingCloudCustomEventSink(SfmcEventTracker())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        // The reporter needs the concrete InAppMessageManager (an IamComponent) to reach the
        // internal display/completion bookkeeping. We obtain it inside requestSdk, then register the
        // listener with the same manager instance.
        InAppMessagingFeature.requestSdk { feature ->
            val manager = feature.getInAppMessageManager()
            val reporter = IamLifecycleReporter(manager, tracker)
            val listener =
                IamEventListener(
                    classifier = classifier,
                    contentProvider = contentProvider,
                    overlay = overlay,
                    customEventSink = customEventSink,
                    reporter = reporter,
                    scope = scope,
                    delegate = delegate,
                    contentSource = config.contentSource,
                    debugLogging = config.debugLogging,
                    placementPrefix = config.placementPrefix,
                    defaultBandDp = config.defaultBandDp,
                )
            manager.setInAppMessageListener(listener)
        }
    }
}
