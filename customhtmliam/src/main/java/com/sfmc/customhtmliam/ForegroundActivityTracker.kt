package com.sfmc.customhtmliam

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/** Tracks the current foreground Activity and notifies on the next resume. */
class ForegroundActivityTracker : Application.ActivityLifecycleCallbacks {
    private var currentRef: WeakReference<Activity>? = null
    private var onResume: ((Activity) -> Unit)? = null

    fun current(): Activity? = currentRef?.get()

    fun setOnResume(cb: ((Activity) -> Unit)?) { onResume = cb }

    override fun onActivityResumed(activity: Activity) {
        currentRef = WeakReference(activity)
        onResume?.invoke(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentRef?.get() === activity) currentRef = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
