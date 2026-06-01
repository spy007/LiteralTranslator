package com.literaltranslator.ui.theme

import androidx.compose.foundation.shape.GenericShape

// Refined shapes for CustomTranslatorDesign matching the 150f corner radius
val TopWaveDesignShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    // Use a fixed radius matching the design
    val r = 150f

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h - r)
    
    // Right arc (concave) - matches circular transition
    // kappa = 4/3 * (sqrt(2) - 1) / 1 ≈ 0.55228
    val k = 0.55228f
    cubicTo(
        w, h - r * (1 - k),
        w - r * (1 - k), h,
        w - r, h
    )
    
    // Bottom line
    lineTo(r, h)
    
    // Left arc (convex) - matching the other side
    cubicTo(
        r * (1 - k), h,
        0f, h + r * (1 - k),
        0f, h + r
    )
    close()
}
