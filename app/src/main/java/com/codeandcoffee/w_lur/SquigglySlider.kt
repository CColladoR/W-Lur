package com.codeandcoffee.w_lur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White, // Renombrado a trackColor
    activeTrackColor: Color = Color.White, // Color para la parte activa
    thumbColor: Color = Color.White,
    thumbRadius: Float = 20.dp.value, // Valor predeterminado corregido
    trackWidth: Float = 10.dp.value, // Valor predeterminado, y ahora es el grosor de la línea inactiva
    activeTrackWidth: Float = 10.dp.value, // Grosor de la línea activa
    horizontalPadding: Dp = 50.dp, // Valor predeterminado
) {

    var sliderPosition by remember { mutableStateOf(0f) } // Comienza en 0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = horizontalPadding)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    val availableWidth =
                        size.width - (2 * horizontalPadding.toPx()) // Ancho disponible
                    val delta = (dragAmount / availableWidth)
                    val newValue = (sliderPosition + delta).coerceIn(0f, 1f)
                    onValueChange(newValue)
                    sliderPosition = newValue
                    change.consume()
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2
        val amplitude = canvasHeight / 4
        val frequency = 2f * Math.PI / canvasWidth * 5
        val thumbX =
            sliderPosition * canvasWidth // Posición X del thumb, ya tiene en cuenta el padding

        // --- Dibujo de la línea inactiva (completa) ---
        for (x in 0 until canvasWidth.toInt()) {
            val y = amplitude * kotlin.math.sin(frequency * x).toFloat() + centerY
            val nextX = (x + 1).toFloat()
            val nextY = amplitude * kotlin.math.sin(frequency * (x + 1)).toFloat() + centerY

            drawLine(
                color = trackColor.copy(alpha = 0.5f), // Color más claro, usa trackColor
                start = Offset(x.toFloat(), y),
                end = Offset(nextX, nextY),
                strokeWidth = trackWidth // Grosor normal
            )
        }

        // --- Dibujo de la línea activa (hasta el thumb) ---
        for (x in 0 until thumbX.toInt()) {
            val y = amplitude * kotlin.math.sin(frequency * x).toFloat() + centerY
            val nextX = (x + 1).toFloat()
            val nextY = amplitude * kotlin.math.sin(frequency * (x + 1)).toFloat() + centerY

            drawLine(
                color = activeTrackColor, // Usa activeTrackColor
                start = Offset(x.toFloat(), y),
                end = Offset(nextX, nextY),
                strokeWidth = activeTrackWidth // Grosor mayor
            )
        }

        // Dibuja el círculo (thumb)
        val thumbY = amplitude * kotlin.math.sin(frequency * thumbX).toFloat() + centerY
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(thumbX, thumbY)
        )
    }
}