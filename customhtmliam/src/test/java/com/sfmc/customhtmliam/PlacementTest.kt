package com.sfmc.customhtmliam

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTest {
    private fun p(title: String?, band: Int = 220) = Placement.parse(title, "chtml:", band)

    private val full = Placement(Anchor.FULL, Dimension.Match, Dimension.Match)

    @Test fun `explicit full`() {
        assertEquals(full, p("chtml:full"))
    }

    @Test fun `size on full is ignored`() {
        assertEquals(full, p("chtml:full-300x300"))
    }

    @Test fun `legacy title with no anchor falls back to full`() {
        assertEquals(full, p("chtml:rating"))
    }

    @Test fun `prefix only falls back to full`() {
        assertEquals(full, p("chtml:"))
    }

    @Test fun `null title falls back to full`() {
        assertEquals(full, p(null))
    }

    @Test fun `title not matching prefix falls back to full`() {
        assertEquals(full, Placement.parse("other:top", "chtml:", 220))
    }

    @Test fun `bare edge uses default band`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Dp(220)), p("chtml:top"))
        assertEquals(Placement(Anchor.BOTTOM, Dimension.Match, Dimension.Dp(220)), p("chtml:bottom"))
    }

    @Test fun `default band is configurable`() {
        assertEquals(Placement(Anchor.BOTTOM, Dimension.Match, Dimension.Dp(140)), p("chtml:bottom", band = 140))
    }

    @Test fun `edge with px size`() {
        assertEquals(Placement(Anchor.BOTTOM, Dimension.Match, Dimension.Dp(120)), p("chtml:bottom-120px"))
    }

    @Test fun `edge with bare number is dp`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Dp(200)), p("chtml:top-200"))
    }

    @Test fun `edge with dp suffix`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Dp(200)), p("chtml:top-200dp"))
    }

    @Test fun `edge with percent`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Percent(25)), p("chtml:top-25%"))
    }

    @Test fun `vertical edges size the width`() {
        assertEquals(Placement(Anchor.LEFT, Dimension.Dp(220), Dimension.Match), p("chtml:left"))
        assertEquals(Placement(Anchor.RIGHT, Dimension.Dp(80), Dimension.Match), p("chtml:right-80dp"))
        assertEquals(Placement(Anchor.LEFT, Dimension.Percent(40), Dimension.Match), p("chtml:left-40%"))
    }

    @Test fun `center with box`() {
        assertEquals(Placement(Anchor.CENTER, Dimension.Dp(320), Dimension.Dp(180)), p("chtml:center-320x180"))
    }

    @Test fun `center with no size falls back to full`() {
        assertEquals(full, p("chtml:center"))
    }

    @Test fun `center with single value is a square`() {
        assertEquals(Placement(Anchor.CENTER, Dimension.Dp(200), Dimension.Dp(200)), p("chtml:center-200"))
    }

    @Test fun `two-word anchor wins over the size hyphen`() {
        assertEquals(Placement(Anchor.TOP_LEFT, Dimension.Dp(320), Dimension.Dp(140)), p("chtml:top-left-320x140"))
        assertEquals(Placement(Anchor.BOTTOM_LEFT, Dimension.Dp(320), Dimension.Dp(140)), p("chtml:bottom-left-320x140"))
        assertEquals(Placement(Anchor.BOTTOM_RIGHT, Dimension.Dp(320), Dimension.Dp(140)), p("chtml:bottom-right-320x140"))
    }

    @Test fun `corner with percent box`() {
        assertEquals(Placement(Anchor.TOP_RIGHT, Dimension.Percent(40), Dimension.Percent(30)), p("chtml:top-right-40%x30%"))
    }

    @Test fun `corner with no size falls back to full`() {
        assertEquals(full, p("chtml:top-left"))
    }

    @Test fun `trailing cosmetic name is ignored`() {
        assertEquals(Placement(Anchor.BOTTOM, Dimension.Match, Dimension.Dp(180)), p("chtml:bottom-180px:rating"))
        assertEquals(Placement(Anchor.CENTER, Dimension.Dp(320), Dimension.Dp(180)), p("chtml:center-320x180:consent"))
    }

    @Test fun `custom prefix`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Dp(100)), Placement.parse("promo:top-100px", "promo:", 220))
    }

    @Test fun `unknown anchor falls back to full`() {
        assertEquals(full, p("chtml:diagonal-100"))
    }

    @Test fun `word that merely starts like an anchor falls back to full`() {
        assertEquals(full, p("chtml:topbanner"))
    }

    @Test fun `edge given a box is malformed and falls back to full`() {
        assertEquals(full, p("chtml:top-320x140"))
    }

    @Test fun `non-numeric size is malformed and falls back to full`() {
        assertEquals(full, p("chtml:top-abc"))
    }

    @Test fun `malformed axis in a box falls back to full`() {
        assertEquals(full, p("chtml:center-320xfoo"))
    }

    @Test fun `zero size falls back to full`() {
        assertEquals(full, p("chtml:top-0"))
    }

    @Test fun `negative size falls back to full`() {
        assertEquals(full, p("chtml:top--50"))
    }

    @Test fun `percent over 100 falls back to full`() {
        assertEquals(full, p("chtml:left-150%"))
    }

    @Test fun `box with zero width falls back to full`() {
        assertEquals(full, p("chtml:center-0x180"))
    }

    @Test fun `box with zero height falls back to full`() {
        assertEquals(full, p("chtml:center-320x0"))
    }

    @Test fun `100 percent is valid`() {
        assertEquals(Placement(Anchor.TOP, Dimension.Match, Dimension.Percent(100)), p("chtml:top-100%"))
    }
}
