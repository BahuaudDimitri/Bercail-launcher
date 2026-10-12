package io.github.bahuauddimitri.bercail.core.domain

/** A picture as plain pixels (ARGB, row by row), so that the domain stays free of Android: an app icon, a cover. */
class Picture(val width: Int, val height: Int, val pixels: IntArray)
