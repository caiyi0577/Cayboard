package com.cayboard.ui.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareToolbarGeometryTest {
    @Test fun physicalToolsAlwaysSnapToNearestSideEvenAtBottomCentre() {
        val geometry = hardwareToolbarGeometry(411, 800, 56, 272, 8)
        for (xf in listOf(0f, .1f, .49f, .5f, .8f, 1f)) {
            for (yf in listOf(0f, .5f, 1f)) {
                val p = HardwareToolbarPosition(xf, yf)
                val o = geometry.offset(p)
                val released = geometry.sidePositionAt(o.x, o.y, p)
                assertEquals(if (xf < .5f) 0f else 1f, released.xFraction, 0f)
                assertEquals(yf, released.yFraction, .0001f)
            }
        }
    }
    @Test fun defaultIsBottomCenteredWithinRealHost() {
        val geometry = hardwareToolbarGeometry(900, 600, 232, 56, 8)
        assertEquals(HardwareToolbarOffset(334f, 536f), geometry.offset(HardwareToolbarPosition()))
    }

    @Test fun releaseOnlySnapsNearEdgesOrCenterAndKeepsFreeTravel() {
        val geometry = hardwareToolbarGeometry(900, 600, 232, 56, 8)
        for ((fraction, dock) in listOf(0.03f to 0f, 0.24f to 0.24f, 0.49f to 0.5f, 0.7f to 0.7f, 0.76f to 0.76f, 0.98f to 1f)) {
            val offset = geometry.offset(HardwareToolbarPosition(fraction, 0.35f))
            val result = geometry.positionAt(offset.x, offset.y, HardwareToolbarPosition())
            assertEquals(dock, result.xFraction, 0.0001f)
            assertEquals(0.35f, result.yFraction, 0.0001f)
        }
    }

    @Test fun movingBeforeReleaseDoesNotSnapOrLeaveHost() {
        val geometry = hardwareToolbarGeometry(900, 600, 232, 56, 8)
        val offset = geometry.offset(HardwareToolbarPosition(0.37f, 0.62f))
        val result = geometry.positionAt(offset.x, offset.y, HardwareToolbarPosition(), snapX = false)
        assertEquals(0.37f, result.xFraction, 0.0001f)
        assertEquals(0.62f, result.yFraction, 0.0001f)
        assertEquals(HardwareToolbarOffset(8f, 536f), geometry.constrain(-300f, 3000f))
    }

    @Test fun savedPositionAdaptsToRotationSplitAndToolbarExpansion() {
        val position = HardwareToolbarPosition(1f, 0.4f)
        for ((width, height) in listOf(900 to 600, 600 to 900, 260 to 480)) {
            for (toolbarHeight in listOf(56, 104)) {
                val geometry = hardwareToolbarGeometry(width, height, 232, toolbarHeight, 8)
                val offset = geometry.offset(position)
                assertEquals((width - 232 - 8).toFloat(), offset.x, 0.0001f)
                assertTrue(offset.y >= 8f && offset.y + toolbarHeight <= height - 8f)
                val restored = geometry.positionAt(offset.x, offset.y, position)
                assertEquals(position.xFraction, restored.xFraction, 0.0001f)
                assertEquals(position.yFraction, restored.yFraction, 0.0001f)
            }
        }
    }

    @Test fun noTravelPreservesExistingDockAndTinyHostsStayValid() {
        val geometry = hardwareToolbarGeometry(48, 48, 48, 48, 8)
        val position = HardwareToolbarPosition(1f, 0.7f)
        assertEquals(HardwareToolbarOffset(0f, 0f), geometry.offset(position))
        assertEquals(position, geometry.positionAt(50f, 50f, position))
        assertEquals(HardwareToolbarOffset(0f, 0f), hardwareToolbarGeometry(0, 0, 232, 56, 8).offset(position))
        assertEquals(HardwareToolbarOffset(1f, 2f), hardwareToolbarGeometry(50, 52, 48, 48, 8).offset(position))
    }

    @Test fun restoredInvalidFractionsReturnToSafeDefaults() {
        val geometry = hardwareToolbarGeometry(900, 600, 232, 56, 8)
        assertEquals(geometry.offset(HardwareToolbarPosition()), geometry.offset(HardwareToolbarPosition(Float.NaN, Float.POSITIVE_INFINITY)))
        assertEquals(HardwareToolbarOffset(8f, 536f), geometry.offset(HardwareToolbarPosition(-3f, 7f)))
    }

    @Test fun sidePositionsNormalizeOldHorizontalAndInvalidLocations() {
        assertEquals(HardwareToolbarPosition(1f, 1f), HardwareToolbarPosition().sideDocked())
        assertEquals(HardwareToolbarPosition(1f, .5f), HardwareToolbarPosition(Float.NaN, Float.NaN).sideDocked())
        assertEquals(HardwareToolbarPosition(0f, 0f), HardwareToolbarPosition(-2f, -2f).sideDocked())
    }
    @org.junit.Test fun editorAvoidanceMovesOnlyWhenNeededAndStaysInsideHost() {
        val g = hardwareToolbarGeometry(800, 600, 232, 56, 8)
        val start = g.offset(HardwareToolbarPosition())
        val far = HardwareCandidateExclusion(10, 10, 100, 50)
        org.junit.Assert.assertEquals(start, avoidHardwareEditor(g, start, 232, 56, far, 12))
        val editor = HardwareCandidateExclusion(250, 500, 550, 590)
        val moved = avoidHardwareEditor(g, start, 232, 56, editor, 12)
        org.junit.Assert.assertTrue(moved.y + 56 <= editor.top - 12)
        org.junit.Assert.assertTrue(moved.x in g.minX..g.maxX && moved.y in g.minY..g.maxY)
        org.junit.Assert.assertEquals(start, g.offset(HardwareToolbarPosition()))
        val covered = avoidHardwareEditor(g, start, 232, 56, HardwareCandidateExclusion(0, 0, 800, 600), 12)
        org.junit.Assert.assertTrue(covered.x in g.minX..g.maxX && covered.y in g.minY..g.maxY)
    }

    @Test fun bottomDockOnlyNearTheBottomCentreAndCornersStayOnTheSides() {
        val geometry = hardwareToolbarGeometry(900, 600, 232, 56, 8)
        val middle = (geometry.minX + geometry.maxX) / 2
        for (x in listOf(middle - 30f, middle, middle + 30f)) {
            assertEquals(HardwareToolbarPosition(), geometry.positionAt(x, geometry.maxY - 20f, HardwareToolbarPosition()))
            assertEquals(HardwareToolbarPosition(), geometry.positionAt(x, geometry.maxY, HardwareToolbarPosition()))
        }
        // Lower corners dock to the side, the rest of the bottom edge stays where it was dropped.
        for ((x, dock) in listOf(geometry.minX to 0f, geometry.maxX to 1f, 150f to null, middle - 60f to null)) {
            for (y in listOf(geometry.maxY - 20f, geometry.maxY)) {
                val result = geometry.positionAt(x, y, HardwareToolbarPosition())
                assertTrue(!result.isBottomDocked(true))
                if (dock != null) assertEquals(dock, result.xFraction, 0f)
                else assertTrue(result.xFraction > 0f && result.xFraction < 1f && result.xFraction != 0.5f)
                assertTrue(result.yFraction > 0.9f)
            }
        }
        val free = geometry.positionAt(200f, geometry.maxY - 40f, HardwareToolbarPosition())
        assertTrue(!free.isBottomDocked(true))
        assertTrue(free.xFraction > 0f && free.xFraction < 1f)
        val unsnapped = geometry.positionAt(200f, geometry.maxY - 1f, HardwareToolbarPosition(), snapX = false)
        assertTrue(unsnapped.yFraction < 1f)
        assertTrue(!HardwareToolbarPosition().isBottomDocked(false))
    }

    @Test fun bottomStripFitsPortraitLandscapeAndSplitHosts() {
        for (width in listOf(336, 600, 1400)) {
            val stripWidth = minOf(720, width - 16)
            val geometry = hardwareToolbarGeometry(width, 500, stripWidth, 56, 8)
            val offset = geometry.offset(HardwareToolbarPosition())
            assertEquals(width / 2f, offset.x + stripWidth / 2f, 0.01f)
            assertEquals(492f, offset.y + 56f, 0.01f)
            assertTrue(offset.x >= 8 && offset.x + stripWidth <= width - 8)
            val released = geometry.positionAt(offset.x, offset.y - 100f, HardwareToolbarPosition())
            assertTrue(!released.isBottomDocked(true))
        }
    }

    @Test fun sideEditorAvoidanceNeverLeavesEitherEdge() {
        val g = hardwareToolbarGeometry(411, 800, 56, 272, 8)
        val preferred = g.offset(HardwareToolbarPosition(1f, .5f))
        val excluded = HardwareCandidateExclusion(300, 180, 411, 580)
        val result = avoidHardwareSideEditor(g, preferred, 56, 272, excluded, 12)
        assertTrue(result.x == g.minX || result.x == g.maxX)
        assertTrue(result.y in g.minY..g.maxY)
        assertTrue(result.x + 56 <= excluded.left - 12 || result.y + 272 <= excluded.top - 12 || result.y >= excluded.bottom + 12)
    }
}
