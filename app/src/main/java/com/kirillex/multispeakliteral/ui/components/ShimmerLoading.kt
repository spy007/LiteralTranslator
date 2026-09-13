package com.kirillex.multispeakliteral.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun Modifier.shimmerLoading(
    isLoading: Boolean,
    shape: Shape = RectangleShape,
    contentColor: Color = Color(0xFFE0E0E0), // Base gray color for the background
    shimmerColor: Color = Color(0xFFF5F5F5)  // Light color for the shimmer highlight
): Modifier {
    if (!isLoading) return this

    // 1. Create an infinite transition animation from 0 to 1000 pixels
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerAnimation"
    )

    // 2. Form a linear gradient with a slope
    val shimmerColors = listOf(
        contentColor,
        shimmerColor,
        contentColor
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    // 3. Return the modifier with the applied gradient background and shape
    return this.background(brush = brush, shape = shape)
}
