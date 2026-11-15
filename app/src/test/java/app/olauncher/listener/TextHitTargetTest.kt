package app.olauncher.listener

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextHitTargetTest {
    @Test
    fun `touch inside text plus margin is accepted`() {
        assertTrue(
            TextHitTarget.contains(
                x = 84f,
                y = 33f,
                textLeft = 20f,
                textRight = 80f,
                textTop = 10f,
                textBottom = 30f,
                horizontalMargin = 8,
                verticalMargin = 4,
            ),
        )
    }

    @Test
    fun `touch in view padding but outside text margin is rejected`() {
        assertFalse(
            TextHitTarget.contains(
                x = 100f,
                y = 20f,
                textLeft = 20f,
                textRight = 80f,
                textTop = 10f,
                textBottom = 30f,
                horizontalMargin = 8,
                verticalMargin = 4,
            ),
        )
    }

    @Test
    fun `touch outside vertical text margin is rejected`() {
        assertFalse(
            TextHitTarget.contains(
                x = 50f,
                y = 40f,
                textLeft = 20f,
                textRight = 80f,
                textTop = 10f,
                textBottom = 30f,
                horizontalMargin = 8,
                verticalMargin = 4,
            ),
        )
    }
}
