package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // Animatable states for progressive staggered animations
    val symbolScale = remember { Animatable(0.4f) }
    val symbolAlpha = remember { Animatable(0f) }
    
    val nameOffsetY = remember { Animatable(30f) }
    val nameAlpha = remember { Animatable(0f) }
    
    val sloganAlpha = remember { Animatable(0f) }

    // Infinite breathing glow behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Step 1: Symbol appears (scale + fade)
        launch {
            symbolAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            symbolScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        delay(500)

        // Step 2: App Name "ZURIKA" appears (slide up + fade)
        launch {
            nameAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            nameOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(500)

        // Step 3: Slogan appears (fade)
        sloganAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearOutSlowInEasing)
        )

        // Total 5.0s display before automatic transition to signup screen
        delay(3400)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate-900
                        Color(0xFF1E3A8A), // Deep Blue
                        Color(0xFF0284C7)  // Zurika Ocean
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Background Decorative Radial Grids
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .alpha(0.12f)
                .background(ZurikaPrimary, CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Step 1: Animated Symbol Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = symbolScale.value
                        scaleY = symbolScale.value
                        alpha = symbolAlpha.value
                    }
            ) {
                // Outer Glow ring
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(136.dp)
                ) {}

                // Main Emblem Circle
                Surface(
                    shape = CircleShape,
                    color = ZurikaPrimary,
                    shadowElevation = 16.dp,
                    modifier = Modifier.size(108.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pin",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(-8.dp))
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = "Station",
                                tint = ZurikaAmber,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Step 2: Animated App Name "ZURIKA"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = nameOffsetY.value
                        alpha = nameAlpha.value
                    }
            ) {
                Text(
                    text = "ZURIKA",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "ASSISTANCE ROUTIÈRE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 3: Animated Slogan
            Text(
                text = "Trouvez. Appelez. Dépannez. Partez.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ZurikaAmber,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(sloganAlpha.value)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

