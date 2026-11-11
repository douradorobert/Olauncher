package app.olauncher.listener

object TextHitTarget {
    fun contains(
        x: Float,
        y: Float,
        textLeft: Float,
        textRight: Float,
        textTop: Float,
        textBottom: Float,
        horizontalMargin: Int,
        verticalMargin: Int,
    ): Boolean =
        x in (textLeft - horizontalMargin)..(textRight + horizontalMargin) &&
            y in (textTop - verticalMargin)..(textBottom + verticalMargin)
}
