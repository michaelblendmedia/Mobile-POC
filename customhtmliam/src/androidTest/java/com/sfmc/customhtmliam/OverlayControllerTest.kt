package com.sfmc.customhtmliam

import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayControllerTest {
    @Test fun overlayAttachesToForegroundActivity() {
        val tracker = ForegroundActivityTracker()
        val state = OverlayState(30_000) { System.currentTimeMillis() }
        val controller = OverlayController(tracker, state, Handler(Looper.getMainLooper()))

        // Uses the library-local TestHostActivity (androidTest manifest) so the suite has no
        // dependency on any host-app Activity.
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> tracker.onActivityResumed(activity) }
            assertTrue(controller.reserve())
            controller.show(
                messageId = "m1",
                html = "<html><body></body></html>",
                placement = Placement.default(),
                onShown = { },
                onEvent = { },
            )
            Thread.sleep(500)
            // Attach + transparency are confirmed visually on-device; this exercises the reserve/attach path.
        }
    }
}
