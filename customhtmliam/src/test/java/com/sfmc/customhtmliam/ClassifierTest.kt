package com.sfmc.customhtmliam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {
    @Test fun `title prefix match is ours`() {
        val c = Classifier(CustomHtmlMatcher.byTitlePrefix("chtml:"))
        assertTrue(c.isCustomHtml("id1", "chtml:rating"))
    }

    @Test fun `non-matching title is not ours`() {
        val c = Classifier(CustomHtmlMatcher.byTitlePrefix("chtml:"))
        assertFalse(c.isCustomHtml("id1", "Weekly promo"))
    }

    @Test fun `null title is not ours`() {
        val c = Classifier(CustomHtmlMatcher.byTitlePrefix("chtml:"))
        assertFalse(c.isCustomHtml("id1", null))
    }

    @Test fun `null id is not ours`() {
        val c = Classifier(CustomHtmlMatcher.byTitlePrefix("chtml:"))
        assertFalse(c.isCustomHtml(null, "chtml:rating"))
    }

    @Test fun `id allow-list match is ours`() {
        val c = Classifier(CustomHtmlMatcher.byIds("abc", "def"))
        assertTrue(c.isCustomHtml("def", null))
        assertFalse(c.isCustomHtml("zzz", null))
    }

    @Test fun `matcher throwing is treated as not ours`() {
        val throwing = CustomHtmlMatcher { _, _ -> throw RuntimeException("boom") }
        assertFalse(Classifier(throwing).isCustomHtml("id1", "chtml:x"))
    }
}
