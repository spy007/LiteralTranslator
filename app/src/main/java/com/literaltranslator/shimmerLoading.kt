package com.literaltranslator

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
    contentColor: Color = Color(0xFFE0E0E0), // Базовый серый цвет подложки
    shimmerColor: Color = Color(0xFFF5F5F5)  // Светлый цвет бегущего блика
): Modifier {
    if (!isLoading) return this

    // 1. Создаем бесконечную анимацию перехода от 0 до 1000 пикселей
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

    // 2. Формируем линейный градиент с наклоном
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

    // 3. Возвращаем модификатор с наложенным градиентным фоном и формой
    return this.background(brush = brush, shape = shape)
}
