package com.example.personaweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.personaweather.ui.theme.LocalHighContrast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val highContrast = LocalHighContrast.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highContrast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = if (highContrast) 0.8f else 0.18f),
        ),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            if (highContrast) Color.Transparent else accent.copy(alpha = 0.12f),
                            if (highContrast) Color.Transparent else Color.White.copy(alpha = 0.02f),
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (title != null) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.semantics { heading() },
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun Gauge(
    value: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.White.copy(alpha = 0.15f),
    label: String = "",
) {
    val clamped = value.coerceIn(0f, 1f)
    val animatedValue = animateFloatAsState(clamped, tween(700), label = "gauge-fill").value
    val start = 225f
    val sweep = 270f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.semantics {
            contentDescription = if (label.isBlank()) "${(clamped * 100).toInt()} percent" else "$label, ${(clamped * 100).toInt()} percent"
        },
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            drawArc(
                color = trackColor,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep * animatedValue,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = (clamped * 100).toInt().toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
) {
    if (values.isEmpty()) return

    val maxValue = values.maxOrNull() ?: 1f
    val minValue = values.minOrNull() ?: 0f
    val range = max(0.001f, maxValue - minValue)

    Canvas(modifier = modifier.height(52.dp).fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val points = values.mapIndexed { index, value ->
            val x = width * index / max(1, values.size - 1)
            val normalized = (value - minValue) / range
            val y = height - (normalized * height)
            Offset(x, y)
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun WeatherGlyph(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    val kind = icon.lowercase()
    val strings = com.example.personaweather.ui.localization.LocalAppStrings.current
    Surface(
        modifier = modifier.size(44.dp).semantics { contentDescription = strings.weather(weatherDescription(kind)) },
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        contentColor = tint,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(30.dp)) {
                val stroke = 2.5.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val cloud = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.65f)
                    cubicTo(size.width * 0.02f, size.height * 0.62f, size.width * 0.08f, size.height * 0.38f, size.width * 0.28f, size.height * 0.39f)
                    cubicTo(size.width * 0.34f, size.height * 0.13f, size.width * 0.69f, size.height * 0.16f, size.width * 0.73f, size.height * 0.4f)
                    cubicTo(size.width * 0.98f, size.height * 0.36f, size.width, size.height * 0.67f, size.width * 0.79f, size.height * 0.68f)
                    lineTo(size.width * 0.2f, size.height * 0.68f)
                    close()
                }
                fun drawCloud() = drawPath(cloud, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
                when (kind) {
                    "sun", "clear" -> {
                        drawCircle(tint, radius = size.minDimension * 0.19f, center = Offset(cx, cy))
                        repeat(8) { index ->
                            val angle = index * (Math.PI / 4.0)
                            val inner = size.minDimension * 0.31f
                            val outer = size.minDimension * 0.43f
                            drawLine(
                                tint,
                                Offset(cx + kotlin.math.cos(angle).toFloat() * inner, cy + kotlin.math.sin(angle).toFloat() * inner),
                                Offset(cx + kotlin.math.cos(angle).toFloat() * outer, cy + kotlin.math.sin(angle).toFloat() * outer),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    "cloud" -> drawCloud()
                    "rain" -> {
                        drawCloud()
                        listOf(0.3f, 0.5f, 0.7f).forEach { x ->
                            drawLine(tint, Offset(size.width * x, size.height * 0.77f), Offset(size.width * (x - 0.08f), size.height * 0.94f), stroke, StrokeCap.Round)
                        }
                    }
                    "storm" -> {
                        drawCloud()
                        val bolt = Path().apply {
                            moveTo(size.width * 0.55f, size.height * 0.66f)
                            lineTo(size.width * 0.39f, size.height * 0.88f)
                            lineTo(size.width * 0.53f, size.height * 0.87f)
                            lineTo(size.width * 0.45f, size.height)
                            lineTo(size.width * 0.69f, size.height * 0.75f)
                            lineTo(size.width * 0.55f, size.height * 0.76f)
                            close()
                        }
                        drawPath(bolt, tint)
                    }
                    "fog" -> repeat(3) { index ->
                        val y = size.height * (0.34f + index * 0.2f)
                        drawLine(tint, Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y), stroke, StrokeCap.Round)
                    }
                    "wind" -> repeat(3) { index ->
                        val y = size.height * (0.28f + index * 0.23f)
                        val gust = Path().apply {
                            moveTo(size.width * 0.12f, y)
                            cubicTo(size.width * 0.46f, y - size.height * 0.12f, size.width * 0.57f, y + size.height * 0.12f, size.width * 0.85f, y)
                        }
                        drawPath(gust, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    }
                    "wave" -> repeat(2) { index ->
                        val y = size.height * (0.43f + index * 0.25f)
                        val wave = Path().apply {
                            moveTo(size.width * 0.08f, y)
                            cubicTo(size.width * 0.28f, y - size.height * 0.2f, size.width * 0.38f, y + size.height * 0.2f, size.width * 0.56f, y)
                            cubicTo(size.width * 0.72f, y - size.height * 0.16f, size.width * 0.81f, y + size.height * 0.14f, size.width * 0.94f, y)
                        }
                        drawPath(wave, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    }
                    "heat" -> {
                        val flame = Path().apply {
                            moveTo(cx, size.height * 0.08f)
                            cubicTo(size.width * 0.78f, size.height * 0.38f, size.width * 0.94f, size.height * 0.54f, cx, size.height * 0.94f)
                            cubicTo(size.width * 0.08f, size.height * 0.77f, size.width * 0.3f, size.height * 0.54f, size.width * 0.38f, size.height * 0.36f)
                            cubicTo(size.width * 0.44f, size.height * 0.54f, size.width * 0.59f, size.height * 0.53f, cx, size.height * 0.08f)
                            close()
                        }
                        drawPath(flame, tint)
                    }
                    else -> drawCircle(tint, radius = size.minDimension * 0.12f, center = Offset(cx, cy))
                }
            }
        }
    }
}

private fun weatherDescription(kind: String): String = when (kind) {
    "sun", "clear" -> "Clear sky"
    "cloud" -> "Cloudy"
    "rain" -> "Rain"
    "storm" -> "Thunderstorm"
    "fog" -> "Fog"
    "wind" -> "Wind"
    "heat" -> "Heat"
    "wave" -> "Sea conditions"
    else -> "Weather"
}

enum class SeverityLevel { LOW, MODERATE, HIGH, CRITICAL }

@Composable
fun SeverityBanner(
    text: String,
    level: SeverityLevel = SeverityLevel.MODERATE,
    modifier: Modifier = Modifier,
    accessibilityDescription: String? = null,
) {
    val strings = com.example.personaweather.ui.localization.LocalAppStrings.current
    val severityLabel = when (level) {
        SeverityLevel.LOW -> strings.text("severity_good", "Good")
        SeverityLevel.MODERATE -> strings.text("severity_caution", "Caution")
        SeverityLevel.HIGH, SeverityLevel.CRITICAL -> strings.text("severity_alert", "Alert")
    }
    val severityIcon = if (level == SeverityLevel.MODERATE || level == SeverityLevel.LOW) Icons.Default.Info else Icons.Default.Warning
    val color = when (level) {
        SeverityLevel.LOW -> Color(0xFF49D59B)
        SeverityLevel.MODERATE -> Color(0xFFFFC857)
        SeverityLevel.HIGH -> Color(0xFFFF8A65)
        SeverityLevel.CRITICAL -> Color(0xFFEE6C83)
    }

    Surface(
        modifier = modifier.semantics { contentDescription = accessibilityDescription ?: "$severityLabel, $text" },
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF102B43).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            androidx.compose.material3.Icon(
                imageVector = severityIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Text(severityLabel, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
