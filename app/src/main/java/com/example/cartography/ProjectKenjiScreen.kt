package com.example.cartography

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderGray
import com.example.ui.theme.ChassisDark
import com.example.ui.theme.Copper
import com.example.ui.theme.CopperDark
import com.example.ui.theme.CopperLight
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DawnGold
import com.example.ui.theme.DuskPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SiriusBlue
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectKenjiScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Simulation Engine State
    val engine = remember { SolarDeclinationEngine() }
    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableDoubleStateOf(1.0) }
    var dayOfYear by remember { mutableIntStateOf(172) } // default: Summer Solstice (~June 21)
    var latitudeDeg by remember { mutableDoubleStateOf(21.4225) } // Sanctuary Coordinates (Mecca)
    val longitudeDeg by remember { mutableDoubleStateOf(39.8262) }

    // Orbital Angles
    var sunAngle by remember { mutableDoubleStateOf(PI / 3) }
    var moonAngle by remember { mutableDoubleStateOf(0.0) }
    var siriusAngle by remember { mutableDoubleStateOf(0.0) }

    // Zoom & Pan
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // UI Dialogs
    var showPartnerDialog by remember { mutableStateOf(false) }
    var activeScriptureIndex by remember { mutableIntStateOf(-1) }

    // Terminal Logs
    val terminalLogs = remember {
        mutableStateListOf(
            TerminalLogEntry("09:44:00", "[SYSTEM INITIALIZED] Calculating unified Solar Declination & Dawn/Dusk Barrier over central coordinate intersection...", true),
            TerminalLogEntry("09:44:02", "[CARTOGRAPHY] B1-B3 Anchor Meridian aligned with Ka'aba Origin (21.42° N, 39.82° E).", true),
            TerminalLogEntry("09:44:05", "[SIRIUS BEACON] As-Shi'ra (Alpha CMa) locked onto outer celestial rail.", true)
        )
    }

    fun addLog(msg: String, isSystem: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        terminalLogs.add(TerminalLogEntry(time, msg, isSystem))
        if (terminalLogs.size > 80) {
            terminalLogs.removeAt(0)
        }
    }

    // Solar calculation results
    val declinationResult = remember(dayOfYear) {
        engine.calculateSolarDeclination(dayOfYear)
    }
    val barrierState = remember(latitudeDeg, longitudeDeg, declinationResult, sunAngle) {
        engine.computeDawnDuskBarrier(latitudeDeg, longitudeDeg, declinationResult, sunAngle)
    }

    // Celestial animation ticker
    LaunchedEffect(isPlaying, speedMultiplier) {
        while (isPlaying) {
            delay(32) // ~30 FPS
            sunAngle = (sunAngle + 0.004 * speedMultiplier) % (2 * PI)
            moonAngle = (moonAngle + 0.0032 * speedMultiplier) % (2 * PI)
            siriusAngle = (siriusAngle + 0.0016 * speedMultiplier) % (2 * PI)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp)
                .widthIn(max = 940.dp)
                .align(Alignment.TopCenter)
        ) {
            // Chassis Tier-3 Container
            Card(
                colors = CardDefaults.cardColors(containerColor = ChassisDark),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(4.dp, BorderGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, ambientColor = CopperDark, spotColor = Copper)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Bar with Bimetallic Press Zone Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PROJECT KENJI // MUDOS-6G",
                                color = Copper,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "CELESTIAL CARTOGRAPHY",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Bimetallic Press Zone Button
                        Box(
                            modifier = Modifier
                                .testTag("bimetallic_press_zone")
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(BorderGray, Copper)
                                    )
                                )
                                .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                                .clickable {
                                    addLog("[PARTNER ROUTING] Invoked Engineering Portal Gateway.")
                                    showPartnerDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ENGINEERING PORTAL\nPARTNER ROUTING",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dashboard Header Subtitles
                    Text(
                        text = "Audhu billahi minash shaitanir rajim | Bismillahirrahmanirrahim",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Intersection: (B1-B3 Anchor) × (B2-B4 Equator) | High-Precision Solar Declination Engine",
                        color = DawnGold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Celestial Visualizer Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Copper),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 380.dp, max = 540.dp)
                            .aspectRatio(1.15f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CelestialVisualizer(
                                sunAngle = sunAngle,
                                moonAngle = moonAngle,
                                siriusAngle = siriusAngle,
                                declinationResult = declinationResult,
                                barrierState = barrierState,
                                zoomScale = zoomScale,
                                onZoomChange = { newZoom ->
                                    zoomScale = newZoom
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay Status Indicators
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xCC050505), RoundedCornerShape(4.dp))
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ZOOM: ${String.format(Locale.US, "%.2fx", zoomScale)} | 36:40 ACTIVE",
                                        color = CyanAccent,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                val statusText = if (!barrierState.isPolarNight && Math.sin(sunAngle) > 0.0) {
                                    "☀ DAYLIGHT [DAWN PASS]"
                                } else {
                                    "☽ NIGHT [DUSK PASS]"
                                }
                                val statusColor = if (!barrierState.isPolarNight && Math.sin(sunAngle) > 0.0) DawnGold else DuskPurple

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xCC050505), RoundedCornerShape(4.dp))
                                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Boolean Navigation Bar (Zoom controls matching the HTML specification)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                zoomScale = (zoomScale + 0.15f).coerceAtMost(3.0f)
                                addLog("[BOOLEAN WEST] Zoom scale: ${String.format(Locale.US, "%.2f", zoomScale)}x (Focused observation).")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF121417),
                                contentColor = CyanAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Copper),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_boolean_west")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zoom In",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BOOLEAN WEST (ZOOM IN)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.5f)
                                addLog("[BOOLEAN EAST] Zoom scale: ${String.format(Locale.US, "%.2f", zoomScale)}x (Wide field view).")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF121417),
                                contentColor = CyanAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Copper),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_boolean_east")
                        ) {
                            Text(
                                text = "BOOLEAN EAST (ZOOM OUT)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Zoom Out",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation & Transit Controls
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRANSIT ENGINE CONTROLS",
                                    color = Copper,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            isPlaying = !isPlaying
                                            addLog(if (isPlaying) "[ENGINE] Simulation resumed." else "[ENGINE] Simulation paused.")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("toggle_play_pause")
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = CyanAccent
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            zoomScale = 1.0f
                                            sunAngle = PI / 3
                                            moonAngle = 0.0
                                            siriusAngle = 0.0
                                            addLog("[ENGINE] Reset coordinates to initial alignment.")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Reset Alignment",
                                            tint = TextMuted
                                        )
                                    }
                                }
                            }

                            // Speed Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speed:",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                listOf(0.5, 1.0, 2.0, 5.0).forEach { spd ->
                                    FilterChip(
                                        selected = speedMultiplier == spd,
                                        onClick = {
                                            speedMultiplier = spd
                                            addLog("[SPEED] Rate set to ${spd}x.")
                                        },
                                        label = {
                                            Text(
                                                "${spd}x",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Copper,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color.Black,
                                            labelColor = CyanAccent
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = speedMultiplier == spd,
                                            borderColor = if (speedMultiplier == spd) Copper else BorderGray
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Day of Year Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DAY OF YEAR (Spencer Fourier Series): $dayOfYear / 366",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Dec: ${String.format(Locale.US, "%+.2f°", declinationResult.declinationDeg)}",
                                    color = DawnGold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = dayOfYear.toFloat(),
                                onValueChange = {
                                    dayOfYear = it.toInt()
                                },
                                valueRange = 1f..366f,
                                colors = SliderDefaults.colors(
                                    thumbColor = DawnGold,
                                    activeTrackColor = Copper,
                                    inactiveTrackColor = BorderGray
                                ),
                                modifier = Modifier.testTag("day_slider")
                            )

                            // Quick astronomical presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AstronomicalPresetButton("Equinox I (Day 80)", 80) {
                                    dayOfYear = 80
                                    addLog("[CALENDAR] Vernal Equinox selected (δ ≈ 0°).")
                                }
                                AstronomicalPresetButton("Summer Solstice (Day 172)", 172) {
                                    dayOfYear = 172
                                    addLog("[CALENDAR] Summer Solstice selected (δ ≈ +23.44°).")
                                }
                                AstronomicalPresetButton("Equinox II (Day 266)", 266) {
                                    dayOfYear = 266
                                    addLog("[CALENDAR] Autumnal Equinox selected (δ ≈ 0°).")
                                }
                                AstronomicalPresetButton("Winter Solstice (Day 355)", 355) {
                                    dayOfYear = 355
                                    addLog("[CALENDAR] Winter Solstice selected (δ ≈ -23.44°).")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Latitude Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "LOCAL LATITUDE: ${String.format(Locale.US, "%.2f°", latitudeDeg)}",
                                    color = SiriusBlue,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (barrierState.isPolarDay) "POLAR DAY" else if (barrierState.isPolarNight) "POLAR NIGHT" else "NORMAL TRANSIT",
                                    color = if (barrierState.isPolarDay || barrierState.isPolarNight) Color(0xFFFF5555) else TerminalGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = latitudeDeg.toFloat(),
                                onValueChange = {
                                    latitudeDeg = it.toDouble()
                                },
                                valueRange = -90f..90f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SiriusBlue,
                                    activeTrackColor = SiriusBlue,
                                    inactiveTrackColor = BorderGray
                                ),
                                modifier = Modifier.testTag("latitude_slider")
                            )

                            // Quick Latitude Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AstronomicalPresetButton("Sanctuary (21.42° N)", 21) {
                                    latitudeDeg = 21.4225
                                    addLog("[LOCATION] Sanctuary Coordinates locked (21.42° N).")
                                }
                                AstronomicalPresetButton("Equator (0.0°)", 0) {
                                    latitudeDeg = 0.0
                                    addLog("[LOCATION] Terrestrial Equator set.")
                                }
                                AstronomicalPresetButton("Tropic of Cancer (23.44° N)", 23) {
                                    latitudeDeg = 23.44
                                    addLog("[LOCATION] Tropic of Cancer zenith set.")
                                }
                                AstronomicalPresetButton("Arctic Circle (66.56° N)", 66) {
                                    latitudeDeg = 66.56
                                    addLog("[LOCATION] Arctic Circle set (Polar transitions active).")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Astronomical Telemetry Grid
                    Text(
                        text = "HIGH-PRECISION TELEMETRY READOUT",
                        color = Copper,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        TelemetryCard(
                            label = "SOLAR DECLINATION (δ)",
                            value = "${String.format(Locale.US, "%+.3f°", declinationResult.declinationDeg)} (${String.format(Locale.US, "%.4f", declinationResult.declinationRad)} rad)",
                            accent = DawnGold,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "SEASONAL AXIAL TILT (ε)",
                            value = "${String.format(Locale.US, "%.3f°", declinationResult.axialTiltDeg)} (Epoch 2026.7)",
                            accent = CopperLight,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "EQUATION OF TIME (EoT)",
                            value = "${String.format(Locale.US, "%+.2f", declinationResult.equationOfTimeMinutes)} min (Solar discrepancy)",
                            accent = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "BARRIER HOUR ANGLE (h₀)",
                            value = "${String.format(Locale.US, "%.2f°", barrierState.hourAngleDeg)} (${String.format(Locale.US, "%.3f", barrierState.hourAngleRad)} rad)",
                            accent = DuskPurple,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "TERMINATOR NORMAL [E,N,U]",
                            value = "[${String.format(Locale.US, "%.2f", barrierState.terminatorNormalVector[0])}, ${String.format(Locale.US, "%.2f", barrierState.terminatorNormalVector[1])}, ${String.format(Locale.US, "%.2f", barrierState.terminatorNormalVector[2])}]",
                            accent = SiriusBlue,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "SIRIUS-ANTARES BEACON",
                            value = "Angle: ${String.format(Locale.US, "%.1f°", (siriusAngle * 180 / PI) % 360)} | 53:49 Locked",
                            accent = TerminalGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Terminal Log Block (matching the HTML specification)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CARTOGRAPHY TERMINAL LOG",
                                color = Copper,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = {
                                terminalLogs.clear()
                                addLog("[TERMINAL CLEARED] Listening for cartographic events...", true)
                            }
                        ) {
                            Text(
                                text = "CLEAR",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = BorderGray,
                                shape = RoundedCornerShape(3.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.Black)
                                .padding(start = 6.dp) // leave space for cyan left accent
                                .border(
                                    width = 3.dp,
                                    brush = Brush.verticalGradient(listOf(CyanAccent, Copper)),
                                    shape = RoundedCornerShape(0.dp)
                                )
                        ) {
                            val listState = rememberLazyListState()

                            LaunchedEffect(terminalLogs.size) {
                                if (terminalLogs.isNotEmpty()) {
                                    listState.animateScrollToItem(terminalLogs.size - 1)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .padding(8.dp)
                                    .testTag("terminal_log_block")
                            ) {
                                items(terminalLogs.size) { idx ->
                                    val entry = terminalLogs[idx]
                                    Text(
                                        text = "[${entry.timestamp}] ${entry.message}",
                                        color = if (entry.isSystem) CyanAccent else TerminalGreen,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Scriptural References & Celestial Signs Section
                    Text(
                        text = "SCRIPTURAL REFERENCES & CELESTIAL SIGNS",
                        color = Copper,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Astrological balance, invariant orbital tracks & terrestrial sanctuaries",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SCRIPTURAL_REFERENCES.forEachIndexed { index, item ->
                            val isExpanded = activeScriptureIndex == index
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) Color(0xFF0F1216) else SurfaceDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isExpanded) Copper else BorderGray
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeScriptureIndex = if (isExpanded) -1 else index
                                        addLog("[REFERENCE] Inspected ${item.surahAyah.replace("\n", " ")}.")
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.surahAyah.replace("\n", " "),
                                                color = CopperLight,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "— ${item.theme}",
                                                color = CyanAccent,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${item.translation}\"",
                                        color = Color(0xFFD1D5DB),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Transliteration:",
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = item.transliteration,
                                                color = DawnGold,
                                                fontSize = 10.sp,
                                                fontStyle = FontStyle.Italic,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Cartographic Significance:",
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = item.contextNote,
                                                color = SiriusBlue,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Spencer's High-Precision Mathematical Formulation Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07080A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CopperDark),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "MATHEMATICAL FORMULATIONS // SPENCER FOURIER EXPANSION",
                                color = Copper,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "γ = 2π / 365 × (dayOfYear - 1)\n" +
                                        "δ = 0.006918 - 0.399912·cos(γ) + 0.070257·sin(γ) - 0.006758·cos(2γ) + 0.000907·sin(2γ) ...\n" +
                                        "cos(h₀) = -tan(φ) · tan(δ)  [Terminator Dawn/Dusk Barrier Condition]\n" +
                                        "Terminator Normal = [cos(δ)sin(h), sin(φ)cos(δ)cos(h) - cos(φ)sin(δ), cos(φ)cos(δ)cos(h) + sin(φ)sin(δ)]",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Partner Routing Dialog
    if (showPartnerDialog) {
        AlertDialog(
            onDismissRequest = { showPartnerDialog = false },
            containerColor = ChassisDark,
            title = {
                Text(
                    text = "ENGINEERING PORTAL // PARTNER ROUTING",
                    color = Copper,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Ecosystem Hooks:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Primary Platform: Google Ecosystem / Earth Cartography Enhancement\n" +
                                "• Partner Stack: Gates Foundation Cartography Gateway\n" +
                                "• Coordinate Anchor: (B1-B3 Anchor) × (B2-B4 Equator)\n" +
                                "• MUDOS Engine: MUDOS-6G High-Precision Solar Declination",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target Repository:\nhttps://github.com/gatesfoundation/engineering-prototype-cartography",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPartnerDialog = false
                        addLog("[REDIRECT] Opening partner repository gateway...")
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/gatesfoundation/engineering-prototype-cartography")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            addLog("[ERROR] Web browser activity not available: ${e.message}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Copper,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Open", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN GATEWAY", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPartnerDialog = false }
                ) {
                    Text("CLOSE", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun TelemetryCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AstronomicalPresetButton(
    title: String,
    target: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF14171C))
            .border(1.dp, CopperDark, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
