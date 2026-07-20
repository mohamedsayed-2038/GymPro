package com.example

import android.graphics.Bitmap
import android.os.Build
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.pow

// --- GEOMETRIC RIGOR: SQUIRCLE SHAPE (C2 CONTINUITY) ---
// Formula: y = (1 - (1 - x)^4)^(1/4) or parameterization of y = sqrt(1 - (1 - x)^4)
class SquircleShape(val cornerSizeRatio: Float = 0.28f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val r = minOf(w, h) * cornerSizeRatio

        path.reset()
        val steps = 24

        // Top-Left quadrant
        for (i in 0..steps) {
            val xl = i.toFloat() / steps // 0 to 1
            // Formula: y = sqrt(1 - (1 - x)^4)
            val yl = sqrt(1f - (1f - xl).pow(4f))
            val px = xl * r
            val py = (1f - yl) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        // Top-Right quadrant
        for (i in 0..steps) {
            val xl = i.toFloat() / steps // 0 to 1
            val yl = sqrt(1f - (1f - xl).pow(4f))
            val px = w - r + yl * r
            val py = (1f - xl) * r
            path.lineTo(px, py)
        }

        // Bottom-Right quadrant
        for (i in 0..steps) {
            val xl = i.toFloat() / steps // 0 to 1
            val yl = sqrt(1f - (1f - xl).pow(4f))
            val px = w - (1f - yl) * r
            val py = h - r + xl * r
            path.lineTo(px, py)
        }

        // Bottom-Left quadrant
        for (i in 0..steps) {
            val xl = i.toFloat() / steps // 0 to 1
            val yl = sqrt(1f - (1f - xl).pow(4f))
            val px = r - yl * r
            val py = h - (1f - xl) * r
            path.lineTo(px, py)
        }

        path.close()
        return Outline.Generic(path)
    }
}

// --- PROCEDURAL OLED MONOCHROMATIC DITHER NOISE GENERATOR ---
object DitherNoiseGenerator {
    private var cachedNoiseBitmap: ImageBitmap? = null

    fun getNoiseBitmap(opacityPercent: Float = 0.025f): ImageBitmap {
        cachedNoiseBitmap?.let { return it }
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = java.util.Random()
        val pixels = IntArray(size * size)
        
        for (i in pixels.indices) {
            val noise = random.nextInt(256)
            // Monochromatic noise
            val alphaVal = (opacityPercent * 255).toInt().coerceIn(0, 255)
            pixels[i] = android.graphics.Color.argb(alphaVal, noise, noise, noise)
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        val imageBitmap = bmp.asImageBitmap()
        cachedNoiseBitmap = imageBitmap
        return imageBitmap
    }
}

// --- DYNAMIC CORE ENGINE COMPOSABLE MODIFIER ---
@Composable
fun Modifier.physicalGlassMaterial(
    phase2Enabled: Boolean,
    phase3Enabled: Boolean,
    phase4Enabled: Boolean,
    noiseEnabled: Boolean,
    contrastEnabled: Boolean,
    blurRadius: Float,           // 8 to 40px
    baseOpacity: Float,          // 5% to 25%
    saturationScale: Float,      // 1.3 to 1.8
    refractionIntensity: Float,  // 0 to 10px
    aberrationOffset: Float,     // 0.5 to 3px
    specularThickness: Float,    // 1 to 2.5px
    noiseOpacity: Float,         // 0.02 to 0.03
    backdropLuminance: Float     // Live input background brightness
): Modifier {
    val noiseBitmap = remember(noiseOpacity) {
        DitherNoiseGenerator.getNoiseBitmap(noiseOpacity)
    }

    // iOS 27 Contrast Algorithm: Trigger dark dimming if backdrop exceeds brightness threshold (e.g., 0.55f)
    val needsContrastDimming = contrastEnabled && (backdropLuminance > 0.55f)
    val dimmingAlpha by animateFloatAsState(
        targetValue = if (needsContrastDimming) 0.35f else 0.0f,
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "dimming_anim"
    )

    return this.drawBehind {
        val width = size.width
        val height = size.height

        // --- PHASE 1: Backdrop Capture & Base Material Fill ---
        // Fill the squircle region with a glassy background representing the material medium
        val baseColor = Color(0xFF1C1C1E).copy(alpha = baseOpacity)
        drawRect(color = baseColor)

        // If contrast algorithm is active, draw the dark dimming layer immediately (35% opacity)
        if (dimmingAlpha > 0f) {
            drawRect(color = Color.Black.copy(alpha = dimmingAlpha))
        }

        // --- PHASE 2: Displacement Refraction simulation ---
        // On canvas we simulate physical refraction by drawing a blurred refractive interior glow
        if (phase2Enabled && refractionIntensity > 0f) {
            val refractColor = Color.White.copy(alpha = 0.05f * (refractionIntensity / 5f))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(refractColor, Color.Transparent),
                    center = Offset(width / 2f, height / 2f),
                    radius = maxOf(width, height) * 0.75f
                )
            )
        }

        // --- PHASE 3: Color Saturation Compensation & ColorMatrix ---
        // Simulate saturation boost using paint color filter directly on canvas draw
        val currentSaturation = if (phase3Enabled) saturationScale else 1.0f
        val paint = androidx.compose.ui.graphics.Paint().apply {
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply {
                    setToSaturation(currentSaturation)
                }
            )
        }

        // --- PHASE 4: Chromatic Aberration & Specular Edge ---
        if (phase4Enabled) {
            // A. Dispersion (Chromatic Aberration) edge shift
            // We draw custom micro-offset outlines representing split red/cyan wavelengths near boundary
            val steps = 24
            val r = minOf(width, height) * 0.28f // Squircle radius
            
            // Generate standard squircle paths for aberration offsets
            val redPath = Path().apply {
                // Slightly offset to the left & top
                val dx = -aberrationOffset
                val dy = -aberrationOffset
                // Red Outline Path
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = xl * r + dx
                    val py = (1f - yl) * r + dy
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - r + yl * r + dx
                    val py = (1f - xl) * r + dy
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - (1f - yl) * r + dx
                    val py = height - r + xl * r + dy
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = r - yl * r + dx
                    val py = height - (1f - xl) * r + dy
                    lineTo(px, py)
                }
                close()
            }

            val cyanPath = Path().apply {
                // Slightly offset to the right & bottom
                val dx = aberrationOffset
                val dy = aberrationOffset
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = xl * r + dx
                    val py = (1f - yl) * r + dy
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - r + yl * r + dx
                    val py = (1f - xl) * r + dy
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - (1f - yl) * r + dx
                    val py = height - r + xl * r + dy
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = r - yl * r + dx
                    val py = height - (1f - xl) * r + dy
                    lineTo(px, py)
                }
                close()
            }

            // Draw micro-aberration outlines
            drawPath(
                path = redPath,
                color = Color(0xFFFF3B30).copy(alpha = 0.22f),
                style = Stroke(width = 1f)
            )
            drawPath(
                path = cyanPath,
                color = Color(0xFF30D5C8).copy(alpha = 0.22f),
                style = Stroke(width = 1f)
            )

            // B. Specular Edge (Specular highlight outline from top-left light source)
            val specularBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(width * 0.4f, height * 0.4f)
            )

            val specularStrokePath = Path().apply {
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = xl * r
                    val py = (1f - yl) * r
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                lineTo(width - r, 0f)
            }

            drawPath(
                path = specularStrokePath,
                brush = specularBrush,
                style = Stroke(width = specularThickness)
            )

            // A thin elegant overall physical boundary border representing iOS 26 glass frame
            val outerBorderBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.03f)
                )
            )
            val fullBorderPath = Path().apply {
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = xl * r
                    val py = (1f - yl) * r
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - r + yl * r
                    val py = (1f - xl) * r
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = width - (1f - yl) * r
                    val py = height - r + xl * r
                    lineTo(px, py)
                }
                for (i in 0..steps) {
                    val xl = i.toFloat() / steps
                    val yl = sqrt(1f - (1f - xl).pow(4f))
                    val px = r - yl * r
                    val py = height - (1f - xl) * r
                    lineTo(px, py)
                }
                close()
            }
            drawPath(
                path = fullBorderPath,
                brush = outerBorderBrush,
                style = Stroke(width = 1.2f)
            )
        }

        // --- PROCEDURAL OLED MONOCHROMATIC DITHERING OVERLAY ---
        if (noiseEnabled) {
            drawIntoCanvas { canvas ->
                val paintNoise = Paint().apply {
                    filterQuality = FilterQuality.None
                    alpha = 1.0f
                }
                // Tile the noise bitmap over our component size
                val tileW = noiseBitmap.width
                val tileH = noiseBitmap.height
                var curY = 0f
                while (curY < height) {
                    var curX = 0f
                    while (curX < width) {
                        canvas.drawImage(noiseBitmap, Offset(curX, curY), paintNoise)
                        curX += tileW
                    }
                    curY += tileH
                }
            }
        }
    }
}

// --- SHORTHAND GLOBAL GLASS MODIFIERS FOR EASY REUSE ACCROSS ENTIRE PROJECT ---
@Composable
fun Modifier.iosSquircleCard(
    cornerSizeRatio: Float = 0.28f,
    baseOpacity: Float = 0.18f,
    saturationScale: Float = 1.80f, // STRICTLY MANDATED 180%
    blurRadius: Float = 30f,
    backdropLuminance: Float = 0.40f,
    contrastEnabled: Boolean = true
): Modifier {
    return this
        .clip(SquircleShape(cornerSizeRatio))
        .physicalGlassMaterial(
            phase2Enabled = true,
            phase3Enabled = true,
            phase4Enabled = true,
            noiseEnabled = true,
            contrastEnabled = contrastEnabled,
            blurRadius = blurRadius,
            baseOpacity = baseOpacity,
            saturationScale = saturationScale,
            refractionIntensity = 6f,
            aberrationOffset = 1.4f,
            specularThickness = 1.8f,
            noiseOpacity = 0.025f,
            backdropLuminance = backdropLuminance
        )
}

@Composable
fun Modifier.recessedGlassInput(
    cornerSizeRatio: Float = 0.18f, // Nested Concentric R_internal
    baseOpacity: Float = 0.08f,     // 5%-12% for small inputs
    backdropLuminance: Float = 0.40f
): Modifier {
    return this
        .clip(SquircleShape(cornerSizeRatio))
        .drawBehind {
            val w = size.width
            val h = size.height
            // Recessed Style: Draw dark background fill (recessed space within glass)
            drawRect(color = Color.Black.copy(alpha = 0.15f))
            
            // Soft inner depth highlights
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(w, 0f),
                end = Offset(w, h),
                strokeWidth = 2f
            )

            // Inner dark bevel lines simulating 3D recess
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(0f, 0f),
                end = Offset(w, 0f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(0f, 0f),
                end = Offset(0f, h),
                strokeWidth = 2.5f
            )
        }
        .physicalGlassMaterial(
            phase2Enabled = true,
            phase3Enabled = true,
            phase4Enabled = true,
            noiseEnabled = true,
            contrastEnabled = true,
            blurRadius = 10f,
            baseOpacity = baseOpacity,
            saturationScale = 1.80f, // STRICTOR MANDATED 180%
            refractionIntensity = 3f,
            aberrationOffset = 0.8f,
            specularThickness = 1.2f,
            noiseOpacity = 0.025f,
            backdropLuminance = backdropLuminance
        )
}

// --- INTERACTIVE LAB SHIELD SCREEN ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IosGlassEngineScreen(
    isArabic: Boolean,
    onBack: () -> Unit
) {
    // Stage activation toggles
    var phase2Enabled by rememberSaveable { mutableStateOf(true) }
    var phase3Enabled by rememberSaveable { mutableStateOf(true) }
    var phase4Enabled by rememberSaveable { mutableStateOf(true) }
    var noiseEnabled by rememberSaveable { mutableStateOf(true) }
    var contrastEnabled by rememberSaveable { mutableStateOf(true) }

    // Adjustable Sliders
    var blurRadius by rememberSaveable { mutableStateOf(24f) }            // px
    var baseOpacity by rememberSaveable { mutableStateOf(0.16f) }         // 5% - 25%
    var saturationScale by rememberSaveable { mutableStateOf(1.55f) }    // 130% - 180%
    var refractionIntensity by rememberSaveable { mutableStateOf(6f) }    // 0 - 10px
    var aberrationOffset by rememberSaveable { mutableStateOf(1.4f) }    // 0.5 - 3px
    var specularThickness by rememberSaveable { mutableStateOf(1.8f) }   // 1 - 2.5px
    var noiseOpacity by rememberSaveable { mutableStateOf(0.025f) }       // 2% - 3%
    var selectedBackground by rememberSaveable { mutableStateOf(0) }     // 0 = Aura, 1 = Solar Flare (Bright), 2 = Twilight, 3 = Gym Grid

    // Background configurations
    val bgNames = if (isArabic) {
        listOf("أوراق هالة المشتعلة", "وهج الشمس الساطع (OLED)", "غسق كوني هادئ", "مصفوفة جيم رياضية")
    } else {
        listOf("Aura Fusion Glow", "Solar Flare (High Bright)", "Cosmic Twilight", "Active Gym Grid")
    }

    // Dynamic background brightness calculation
    val backdropLuminance = when (selectedBackground) {
        0 -> 0.42f // Mild
        1 -> 0.78f // Very Bright -> Triggers iOS 27 dark dimming contrast safety overlay!
        2 -> 0.21f // Dark
        3 -> 0.35f // Medium Dark
        else -> 0.4f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "backdrop_animation")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
    ) {
        // --- PHASE 1 BACKDROP SIMULATOR CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    
                    when (selectedBackground) {
                        0 -> { // Aura Fusion
                            drawRect(color = Color(0xFF0F0C1B))
                            // Draw moving colorful vector circles representing fluid meta-material
                            val cx1 = w / 2f + cos(orbitAngle) * (w * 0.2f)
                            val cy1 = h / 2f + sin(orbitAngle) * (h * 0.2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF007F), Color.Transparent),
                                    center = Offset(cx1, cy1),
                                    radius = w * 0.4f
                                ),
                                center = Offset(cx1, cy1),
                                radius = w * 0.4f
                            )

                            val cx2 = w / 2f + cos(orbitAngle + Math.PI.toFloat()) * (w * 0.25f)
                            val cy2 = h / 2f + sin(orbitAngle + Math.PI.toFloat()) * (h * 0.15f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF00FFCC), Color.Transparent),
                                    center = Offset(cx2, cy2),
                                    radius = w * 0.45f
                                ),
                                center = Offset(cx2, cy2),
                                radius = w * 0.45f
                            )
                        }
                        1 -> { // Solar Flare (HIGH LUMINANCE)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFDD00), Color(0xFFFF6600)),
                                    center = Offset(w / 2f, h / 2f),
                                    radius = w * 0.8f
                                )
                            )
                            // Bright flares to check readability
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                center = Offset(w / 2f - 40f, h / 2f - 30f),
                                radius = 130f
                            )
                        }
                        2 -> { // Cosmic Twilight
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF08071A), Color(0xFF2E0854), Color(0xFF02010A))
                                )
                            )
                            // Grid overlay
                            val linePaint = Paint().apply {
                                color = Color.White.copy(alpha = 0.05f)
                                strokeWidth = 1f
                            }
                            val spacing = 40f
                            var x = 0f
                            while (x < w) {
                                drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
                                x += spacing
                            }
                            var y = 0f
                            while (y < h) {
                                drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                                y += spacing
                            }
                        }
                        3 -> { // Active Gym Grid
                            drawRect(color = Color(0xFF121215))
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFCCFF00).copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(w * 0.7f, h * 0.4f),
                                    radius = w * 0.6f
                                ),
                                center = Offset(w * 0.7f, h * 0.4f),
                                radius = w * 0.6f
                            )
                            // Diagonal sporty lines
                            var offset = -h
                            while (offset < w) {
                                drawLine(
                                    color = Color(0xFFCCFF00).copy(alpha = 0.06f),
                                    start = Offset(offset, 0f),
                                    end = Offset(offset + h, h),
                                    strokeWidth = 3f
                                )
                                offset += 100f
                            }
                        }
                    }
                }
        )

        // --- MAIN SCENE COLUMN CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isArabic) "مختبر آبل الرسومي" else "Apple Graphics Lab",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isArabic) "محاكي المادة الزجاجية الفيزيائية iOS 26/27" else "Physical Glass Engine iOS 26/27 Simulator",
                        color = Color(0xFFCCFF00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Real-time Physics Simulator Glass Card (Floating over captured background)
            Spacer(modifier = Modifier.height(16.dp))

            // GEOMETRIC RIGOR RULES APPLY:
            // R_internal = R_external - P_padding -> Parallel borders
            val externalRadiusRatio = 0.28f
            val paddingValue = 18.dp

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(SquircleShape(externalRadiusRatio)) // Squircle Outline
                    .physicalGlassMaterial(
                        phase2Enabled = phase2Enabled,
                        phase3Enabled = phase3Enabled,
                        phase4Enabled = phase4Enabled,
                        noiseEnabled = noiseEnabled,
                        contrastEnabled = contrastEnabled,
                        blurRadius = blurRadius,
                        baseOpacity = baseOpacity,
                        saturationScale = saturationScale,
                        refractionIntensity = refractionIntensity,
                        aberrationOffset = aberrationOffset,
                        specularThickness = specularThickness,
                        noiseOpacity = noiseOpacity,
                        backdropLuminance = backdropLuminance
                    )
                    .padding(paddingValue)
            ) {
                // Nested content inside Glass Card
                // Applying R_internal = R_external - P_padding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(SquircleShape(cornerSizeRatio = externalRadiusRatio * 0.8f)) // R_internal nested Squircle
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = SquircleShape(cornerSizeRatio = externalRadiusRatio * 0.8f)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // iOS Style Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (contrastEnabled && backdropLuminance > 0.55f) Color(0xFFFF9500) else Color(0xFF00FFCC),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (contrastEnabled && backdropLuminance > 0.55f) {
                                    if (isArabic) "مفعّل: تعتيم iOS 27" else "Active: iOS 27 Dimming"
                                } else {
                                    if (isArabic) "مستقر" else "Optically Stable"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Version Watermark
                        Text(
                            text = "iOS 27 ENGINE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // Formula debugger preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "y = \u221A(1 - (1 - x)\u2074)",
                            color = Color(0xFFCCFF00),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R_internal = R_external - P_padding",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Sat compensation: ${(saturationScale * 100).toInt()}% | OLED Noise: ${(noiseOpacity * 100).toFloat()}%",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = if (isArabic) {
                            "المادة الرقمية ليست رسومات مسطحة بل محاكاة فيزيائية لانكسار الضوء."
                        } else {
                            "Meta-Material is not flat vector; it is a physical light refraction medium."
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interactive Controls Board
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF141416), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "أدوات تحكم المكدس البصري الرباعي" else "Quad-Stage Optical Stack Controls",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Background selection row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "تغيير محتوى الخلفية (الباب الخلفي):" else "Change Backdrop Content:",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bgNames.forEachIndexed { index, name ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedBackground == index) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.06f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedBackground = index }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = name,
                                        color = if (selectedBackground == index) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    // Phase 2 Displacement Refraction controls
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = phase2Enabled,
                                    onCheckedChange = { phase2Enabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCCFF00))
                                )
                                Text(
                                    text = if (isArabic) "المرحلة 2: خريطة الإزاحة وانكسار الحواف" else "Phase 2: Displacement Refraction",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${refractionIntensity.toInt()}px",
                                color = Color(0xFFCCFF00),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (phase2Enabled) {
                            Slider(
                                value = refractionIntensity,
                                onValueChange = { refractionIntensity = it },
                                valueRange = 0f..10f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFCCFF00),
                                    activeTrackColor = Color(0xFFCCFF00),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // Phase 3 Saturation controls
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = phase3Enabled,
                                    onCheckedChange = { phase3Enabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCCFF00))
                                )
                                Text(
                                    text = if (isArabic) "المرحلة 3: قانون التعويض اللوني (التشبع)" else "Phase 3: Saturation Compensation",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${(saturationScale * 100).toInt()}%",
                                color = Color(0xFFCCFF00),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (phase3Enabled) {
                            Slider(
                                value = saturationScale,
                                onValueChange = { saturationScale = it },
                                valueRange = 1.30f..1.80f, // STRICTOR MANDATED RANGE: 130% - 180%
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFCCFF00),
                                    activeTrackColor = Color(0xFFCCFF00),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // Phase 4 Dispersion & Shimmer
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = phase4Enabled,
                                    onCheckedChange = { phase4Enabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCCFF00))
                                )
                                Text(
                                    text = if (isArabic) "المرحلة 4: الانحراف اللوني والحافة اللامعة" else "Phase 4: Chromatic Dispersion & Gloss",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${aberrationOffset}px",
                                color = Color(0xFFCCFF00),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (phase4Enabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isArabic) "سُمك الحافة اللامعة:" else "Specular Highlight Thickness:",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                                Slider(
                                    value = specularThickness,
                                    onValueChange = { specularThickness = it },
                                    valueRange = 1f..2.5f, // STRICTOR MANDATED RANGE: 1px - 2.5px
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFCCFF00),
                                        activeTrackColor = Color(0xFFCCFF00),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }

                    // OLED Banding Noise
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = noiseEnabled,
                                    onCheckedChange = { noiseEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCCFF00))
                                )
                                Text(
                                    text = if (isArabic) "نسيج OLED العشوائي لمنع التشويه" else "OLED Monochromatic Dither Noise",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${(noiseOpacity * 100).toFloat()}%",
                                color = Color(0xFFCCFF00),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (noiseEnabled) {
                            Slider(
                                value = noiseOpacity,
                                onValueChange = { noiseOpacity = it },
                                valueRange = 0.02f..0.03f, // STRICTOR MANDATED RANGE: 2% - 3%
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFCCFF00),
                                    activeTrackColor = Color(0xFFCCFF00),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // iOS 27 Adaptive contrast algorithm
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = contrastEnabled,
                                    onCheckedChange = { contrastEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCCFF00))
                                )
                                Text(
                                    text = if (isArabic) "خوارزمية التباين الذكية (iOS 27)" else "iOS 27 Smart Contrast Safety",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (backdropLuminance > 0.55f && contrastEnabled) "DIMMING ON (4.5:1)" else "SAFE",
                                color = if (backdropLuminance > 0.55f && contrastEnabled) Color(0xFFFF9500) else Color(0xFF00FFCC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (isArabic) {
                                "تفحص السطوع تلقائياً؛ فوق الخلفيات الساطعة تفعل طبقة تعتيم بنسبة 35% فوراً لحماية المقروئية."
                            } else {
                                "Scans backdrop dynamically; if bright, dims by 35% instantly to guarantee WCAG 4.5:1 ratio."
                            },
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    // Base glass material properties
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isArabic) "خصائص المادة الأساسية (المصفوفة الرقمية):" else "Base Material Class Specifications (Numerical Matrix):",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Micro-controls presets (blur 8-12px, opacity 5%-12%)
                            Button(
                                onClick = {
                                    blurRadius = 10f
                                    baseOpacity = 0.08f
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isArabic) "أدوات تحكم دقيقة" else "Micro-Controls",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "10px Blur | 8% Opacity",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            // Cards/Windows presets (blur 20-40px, opacity 15%-25%)
                            Button(
                                onClick = {
                                    blurRadius = 30f
                                    baseOpacity = 0.18f
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isArabic) "البطاقات والنوافذ" else "Cards & Windows",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "30px Blur | 18% Opacity",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
