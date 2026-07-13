package com.sfmc.customhtmliam

import com.salesforce.marketingcloud.inappmessaging.models.InAppMessage

/** Null- and exception-safe wrapper around the configured matcher. Never throws. */
class Classifier(private val matcher: CustomHtmlMatcher) {

    /** Testable core: decide from primitive fields already extracted from the message. */
    fun isCustomHtml(id: String?, title: String?): Boolean {
        if (id == null) return false
        return try {
            matcher.matches(id, title)
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Production shim: pull the two primitive fields off the SDK message, then delegate.
     * `title` is an InAppMessage.TextField; its display string is `.text`. Field reads are
     * guarded so a malformed message is treated as "not ours".
     */
    fun isCustomHtml(message: InAppMessage?): Boolean {
        if (message == null) return false
        val id = try { message.id } catch (t: Throwable) { null }
        val title = try { message.title?.text } catch (t: Throwable) { null }
        return isCustomHtml(id, title)
    }

    companion object {
        /** Safely reads the message body's display string, or null. Never throws. */
        fun bodyText(message: InAppMessage?): String? =
            try { message?.body?.text } catch (t: Throwable) { null }
    }
}
