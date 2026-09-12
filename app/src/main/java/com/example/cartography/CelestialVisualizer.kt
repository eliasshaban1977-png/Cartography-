package com.example.cartography

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.BorderGray
import com.example.ui.theme.Copper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DawnGold
import com.example.ui.theme.DuskPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SiriusBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CelestialVisualizer(
    sunAngle: Double,
    moonAngle: Double,
    siriusAngle: Double,
    declinationResult: SolarDeclinationResult,
    barrierState: DawnDuskBarrierState,
    zoomScale: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(ObsidianBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (zoomScale * zoom).coerceIn(0.5f, 3.0f)
                    onZoomChange(newZoom)
                    panOffsetX = (panOffsetX + pan.x).coerceIn(-400f, 400f)
                    panOffsetY = (panOffsetY + pan.y).coerceIn(-400f, 400f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f + panOffsetX
            val cy = size.height / 2f + panOffsetY
            val baseDimension = min(size.width, size.height)
            val scaleFactor = (baseDimension / 640f) * zoomScale

            val rMoonOrbit = 95f * scaleFactor
            val rRearSpout = 125f * scaleFactor
            val rFrontSpout = 165f * scaleFactor
            val rSun = 205f * scaleFactor
            val rOuterRail = 250f * scaleFactor
            val rDegreeRing = 275f * scaleFactor

            // Background Deep Radial Gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1518), Color(0xFF060809), ObsidianBg),
                    center = Offset(cx, cy),
                    radius = rDegreeRing * 1.5f
                )
            )

            // Celestial Grid lines
            drawCartographicGrid(cx, cy, scaleFactor, rOuterRail)

            // Shaded night hemisphere with terminator angle
            val terminatorAngle = (sunAngle + PI / 2.0).toFloat()
            drawNightHemisphere(cx, cy, rOuterRail, terminatorAngle)

            // Concentric orbit rings
            drawConcentricOrbits(cx, cy, rMoonOrbit, rRearSpout, rFrontSpout, rSun, rOuterRail, rDegreeRing)

            // Bucket Mouth Navigation Arcs
            drawBucketMouthNav(cx, cy, rFrontSpout, rRearSpout, siriusAngle.toFloat())

            // Central Anchor Intersection Lines (B1-B3 Anchor × B2-B4 Equator)
            drawAnchorIntersections(cx, cy, rOuterRail)

            // Dawn / Dusk Barrier (Terminator line with dynamic gradient)
            drawDawnDuskBarrier(cx, cy, rOuterRail * 1.15f, terminatorAngle)

            // Base Anchor Nodes (B1, B2, B3, B4 & Center Ka'aba Sanctuary)
            drawBaseAnchorNodes(cx, cy, rOuterRail)

            // Celestial Bodies
            // 1. Sirius Beacon (Rabbu ash-Shi'ra)
            val siriusX = cx + rOuterRail * cos(siriusAngle).toFloat()
            val siriusY = cy + rOuterRail * sin(siriusAngle).toFloat()
            drawSiriusBeacon(siriusX, siriusY, 9f * scaleFactor)

            // 2. Antares Counter-Star
            val antaresAngle = (siriusAngle + PI).toFloat()
            val antaresX = cx + rOuterRail * cos(antaresAngle.toDouble()).toFloat()
            val antaresY = cy + rOuterRail * sin(antaresAngle.toDouble()).toFloat()
            drawAntaresBeacon(antaresX, antaresY, 6.5f * scaleFactor)

            // 3. Moon Node (LUNA_01)
            val moonX = cx + rMoonOrbit * cos(moonAngle).toFloat()
            val moonY = cy + rMoonOrbit * sin(moonAngle).toFloat()
            drawMoonNode(moonX, moonY, 8.5f * scaleFactor)

            // 4. Sun Node (SOL_01)
            val sunX = cx + rSun * cos(sunAngle).toFloat()
            val sunY = cy + rSun * sin(sunAngle).toFloat()
            drawSunNode(sunX, sunY, 13f * scaleFactor, declinationResult.declinationDeg)

            // Compass Cardinal Ticks & Labels
            drawCompassDial(cx, cy, rDegreeRing, rOuterRail)
        }
    }
}

private fun DrawScope.drawCartographicGrid(cx: Float, cy: Float, scale: Float, maxR: Float) {
    val gridColor = Color(0x1F00FFAA)
    val gridStep = 40f * scale

    var r = gridStep
    while (r <= maxR * 1.2f) {
        drawCircle(
            color = gridColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        )
        r += gridStep
    }

    // 12 Astrological / Cartographic Sectors (30 degrees each)
    for (i in 0 until 12) {
        val rad = (i * 30 * PI / 180.0).toFloat()
        val endX = cx + (maxR * 1.15f) * cos(rad)
        val endY = cy + (maxR * 1.15f) * sin(rad)
        drawLine(
            color = Color(0x15B87333),
            start = Offset(cx, cy),
            end = Offset(endX, endY),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawNightHemisphere(cx: Float, cy: Float, radius: Float, terminatorAngle: Float) {
    // Night overlay arc on the side opposite the sun
    val startAngleDeg = (terminatorAngle * 180f / PI.toFloat())
    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x35000511), Color(0x85000511)),
            center = Offset(cx, cy),
            radius = radius
        ),
        startAngle = startAngleDeg,
        sweepAngle = 180f,
        useCenter = true,
        size = Size(radius * 2f, radius * 2f),
        topLeft = Offset(cx - radius, cy - radius)
    )
}

private fun DrawScope.drawConcentricOrbits(
    cx: Float,
    cy: Float,
    rMoon: Float,
    rRear: Float,
    rFront: Float,
    rSun: Float,
    rOuter: Float,
    rDegree: Float
) {
    // Moon orbit
    drawCircle(
        color = Color(0x4488CCFF),
        radius = rMoon,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    )

    // Bucket spouts
    drawCircle(
        color = Color(0x33B87333),
        radius = rRear,
        center = Offset(cx, cy),
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f)))
    )
    drawCircle(
        color = Color(0x33B87333),
        radius = rFront,
        center = Offset(cx, cy),
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f)))
    )

    // Sun Ecliptic Rail
    drawCircle(
        color = Color(0x66FFAA00),
        radius = rSun,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )

    // Outer Nav Rail
    drawCircle(
        color = Copper,
        radius = rOuter,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2f)
    )

    // Outer Degree Ring
    drawCircle(
        color = BorderGray,
        radius = rDegree,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawBucketMouthNav(
    cx: Float,
    cy: Float,
    rFront: Float,
    rRear: Float,
    siriusAngle: Float
) {
    val arcSpan = 70f
    val centerDeg = (siriusAngle * 180f / PI.toFloat())

    // Front Spout arc
    drawArc(
        color = CyanAccent,
        startAngle = centerDeg - arcSpan / 2f,
        sweepAngle = arcSpan,
        useCenter = false,
        topLeft = Offset(cx - rFront, cy - rFront),
        size = Size(rFront * 2f, rFront * 2f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Rear Spout arc
    drawArc(
        color = Copper,
        startAngle = centerDeg + 180f - arcSpan / 2f,
        sweepAngle = arcSpan,
        useCenter = false,
        topLeft = Offset(cx - rRear, cy - rRear),
        size = Size(rRear * 2f, rRear * 2f),
        style = Stroke(width = 2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
    )
}

private fun DrawScope.drawAnchorIntersections(cx: Float, cy: Float, radius: Float) {
    val copperCross = Color(0xAAFF8833)

    // B1 - B3 Meridian Anchor Line (Vertical)
    drawLine(
        color = copperCross,
        start = Offset(cx, cy - radius * 1.12f),
        end = Offset(cx, cy + radius * 1.12f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
    )

    // B2 - B4 Equator Line (Horizontal)
    drawLine(
        color = Color(0xAA00FFAA),
        start = Offset(cx - radius * 1.12f, cy),
        end = Offset(cx + radius * 1.12f, cy),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
    )

    // Center Origin Target Reticle (Ka'aba / Sanctuary Meridian)
    drawCircle(
        color = Copper,
        radius = 12f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        color = CyanAccent,
        radius = 4f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawDawnDuskBarrier(cx: Float, cy: Float, length: Float, angleRad: Float) {
    val dx = cos(angleRad) * length
    val dy = sin(angleRad) * length

    val start = Offset(cx - dx, cy - dy)
    val end = Offset(cx + dx, cy + dy)

    val barrierBrush = Brush.linearGradient(
        colors = listOf(DawnGold, Color.White, DuskPurple),
        start = start,
        end = end
    )

    // Glowing halo line
    drawLine(
        brush = barrierBrush,
        start = start,
        end = end,
        strokeWidth = 6f,
        cap = StrokeCap.Round,
        alpha = 0.35f
    )

    // Crisp dashed barrier
    drawLine(
        brush = barrierBrush,
        start = start,
        end = end,
        strokeWidth = 2.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f)),
        cap = StrokeCap.Square
    )

    // Dawn & Dusk End Terminals
    drawCircle(
        color = DawnGold,
        radius = 5f,
        center = start
    )
    drawCircle(
        color = DuskPurple,
        radius = 5f,
        center = end
    )
}

private fun DrawScope.drawBaseAnchorNodes(cx: Float, cy: Float, radius: Float) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }

    // B1: North Sanctuary Anchor
    drawNode(cx, cy - radius, "B1 (NORTH ANCHOR)", CyanAccent, paint)

    // B2: East Equator Anchor
    drawNode(cx + radius, cy, "B2 (EAST EQUATOR)", CyanAccent, paint)

    // B3: South Sanctuary Anchor
    drawNode(cx, cy + radius, "B3 (SOUTH ANCHOR)", CyanAccent, paint)

    // B4: West Equator Anchor
    drawNode(cx - radius, cy, "B4 (WEST EQUATOR)", CyanAccent, paint)

    // Center Anchor Label
    drawContext.canvas.nativeCanvas.drawText(
        "ORIGIN: KA'ABA SANCTUARY",
        cx + 16f,
        cy - 12f,
        paint.apply {
            color = android.graphics.Color.parseColor("#FFAA00")
            textSize = 22f
            isFakeBoldText = true
        }
    )
}

private fun DrawScope.drawNode(
    x: Float,
    y: Float,
    label: String,
    accent: Color,
    textPaint: android.graphics.Paint
) {
    drawCircle(color = accent, radius = 7f, center = Offset(x, y))
    drawCircle(color = Color.White, radius = 7f, center = Offset(x, y), style = Stroke(width = 1.8f))

    drawContext.canvas.nativeCanvas.drawText(
        label,
        x + 12f,
        y + 6f,
        textPaint.apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            textSize = 20f
            isFakeBoldText = false
        }
    )
}

private fun DrawScope.drawSiriusBeacon(x: Float, y: Float, r: Float) {
    // Multi-tier luminous halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, SiriusBlue, Color(0x3388CCFF), Color.Transparent),
            center = Offset(x, y),
            radius = r * 3.5f
        ),
        radius = r * 3.5f,
        center = Offset(x, y)
    )

    // Diamond cross spikes for brilliant celestial star appearance
    val spikeLen = r * 3.2f
    drawLine(Color(0xCCFFFFFF), Offset(x - spikeLen, y), Offset(x + spikeLen, y), strokeWidth = 1.5f)
    drawLine(Color(0xCCFFFFFF), Offset(x, y - spikeLen), Offset(x, y + spikeLen), strokeWidth = 1.5f)

    // Core
    drawCircle(color = Color.White, radius = r, center = Offset(x, y))

    // Label
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#88CCFF")
        textSize = 22f
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("★ SIRIUS (As-Shi'ra)", x + r * 1.5f, y - 6f, paint)
    drawContext.canvas.nativeCanvas.drawText("Primary Beacon 53:49", x + r * 1.5f, y + 16f, paint.apply {
        textSize = 18f
        color = android.graphics.Color.parseColor("#A0C4E2")
    })
}

private fun DrawScope.drawAntaresBeacon(x: Float, y: Float, r: Float) {
    val redColor = Color(0xFFFF4444)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, redColor, Color.Transparent),
            center = Offset(x, y),
            radius = r * 2.8f
        ),
        radius = r * 2.8f,
        center = Offset(x, y)
    )
    drawCircle(color = Color.White, radius = r, center = Offset(x, y))

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FF6666")
        textSize = 20f
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("ANTARES (Alpha Sco)", x + r * 1.5f, y + 4f, paint)
}

private fun DrawScope.drawMoonNode(x: Float, y: Float, r: Float) {
    // Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0x55B0C4DE), Color.Transparent),
            center = Offset(x, y),
            radius = r * 2.2f
        ),
        radius = r * 2.2f,
        center = Offset(x, y)
    )

    // Moon disc
    drawCircle(color = Color(0xFFE8EEF5), radius = r, center = Offset(x, y))

    // Subtle crescent shadow
    drawCircle(
        color = Color(0xFF141922),
        radius = r * 0.85f,
        center = Offset(x - r * 0.35f, y - r * 0.25f)
    )

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#D0E0FF")
        textSize = 20f
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("☽ LUNA_01 (The Moon)", x + r * 1.4f, y + 4f, paint)
}

private fun DrawScope.drawSunNode(x: Float, y: Float, r: Float, declinationDeg: Double) {
    // Solar Corona Rays
    val rayCount = 12
    for (i in 0 until rayCount) {
        val angle = (i * (2 * PI / rayCount)).toFloat()
        val rayInner = r * 1.25f
        val rayOuter = r * 1.85f
        drawLine(
            color = Color(0xBBFFAA00),
            start = Offset(x + rayInner * cos(angle), y + rayInner * sin(angle)),
            end = Offset(x + rayOuter * cos(angle), y + rayOuter * sin(angle)),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }

    // Solar Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF0AA), DawnGold, Color(0x44FFAA00), Color.Transparent),
            center = Offset(x, y),
            radius = r * 3f
        ),
        radius = r * 3f,
        center = Offset(x, y)
    )

    // Core
    drawCircle(color = Color(0xFFFFF7C0), radius = r, center = Offset(x, y))

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FFCC00")
        textSize = 22f
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("☀ SOL_01 (Sun)", x + r * 1.5f, y - 6f, paint)
    drawContext.canvas.nativeCanvas.drawText(
        String.format(java.util.Locale.US, "Dec: %+.2f°", declinationDeg),
        x + r * 1.5f,
        y + 16f,
        paint.apply {
            textSize = 18f
            color = android.graphics.Color.parseColor("#E0AA20")
            isFakeBoldText = false
        }
    )
}

private fun DrawScope.drawCompassDial(cx: Float, cy: Float, rDegree: Float, rOuter: Float) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#DA8A43")
        textSize = 20f
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    // Cardinal directions
    drawContext.canvas.nativeCanvas.drawText("N [000°]", cx, cy - rDegree - 10f, paint)
    drawContext.canvas.nativeCanvas.drawText("E [090°]", cx + rDegree + 35f, cy + 6f, paint)
    drawContext.canvas.nativeCanvas.drawText("S [180°]", cx, cy + rDegree + 24f, paint)
    drawContext.canvas.nativeCanvas.drawText("W [270°]", cx - rDegree - 35f, cy + 6f, paint)

    // Degree tick marks on outer rail
    for (deg in 0 until 360 step 10) {
        val rad = (deg * PI / 180.0).toFloat()
        val isMajor = deg % 30 == 0
        val tickLen = if (isMajor) 10f else 5f
        val r1 = rOuter - tickLen
        val r2 = rOuter

        drawLine(
            color = if (isMajor) Copper else BorderGray,
            start = Offset(cx + r1 * cos(rad), cy + r1 * sin(rad)),
            end = Offset(cx + r2 * cos(rad), cy + r2 * sin(rad)),
            strokeWidth = if (isMajor) 1.8f else 1f
        )
    }
}
