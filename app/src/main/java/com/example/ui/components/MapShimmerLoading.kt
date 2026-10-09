package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ZurikaAmber
import com.example.ui.theme.ZurikaPrimary

/**
 * Creates an animated linear gradient shimmer brush for loading skeleton elements.
 */
@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1400f,
    durationMs: Int = 1200
): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.04f),
        Color.White.copy(alpha = 0.18f),
        ZurikaAmber.copy(alpha = 0.25f),
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.04f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 350f, translateAnimation - 350f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * Full Map Shimmer Overlay displayed while dépanneurs, garages and roadside services
 * are being retrieved or refreshed.
 */
@Composable
fun MapShimmerLoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    statusMessage: String = "Recherche des dépanneurs et garages à proximité..."
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(260)),
        modifier = modifier
    ) {
        val shimmerBrush = rememberShimmerBrush()

        // Radar rotation animation for scanner effect
        val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
        val radarPulse by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radar_scale"
        )
        val radarAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radar_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .testTag("map_shimmer_overlay")
        ) {
            // Center pulsating radar beam representing GPS scanning in Lomé
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size((180 * radarPulse).dp)
                    .clip(CircleShape)
                    .border(2.dp, ZurikaPrimary.copy(alpha = radarAlpha), CircleShape)
                    .background(ZurikaPrimary.copy(alpha = radarAlpha * 0.4f))
            )

            // Top Shimmer Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 20.dp, end = 20.dp)
                    .testTag("shimmer_loading_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Chargement",
                            tint = ZurikaAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ZURIKA GPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ZurikaAmber,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ZurikaAmber)
                            )
                        }
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Floating Shimmering Marker Placeholders in simulated Lomé locations
            // Dépanneur Marker Skeleton 1 (Nord - Déckon)
            ShimmerMarkerPlaceholder(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-40).dp, y = (-75).dp),
                shimmerBrush = shimmerBrush,
                categoryIcon = Icons.Default.Build,
                labelWidth = 72.dp
            )

            // Dépanneur Marker Skeleton 2 (Est - Bè / Lagune)
            ShimmerMarkerPlaceholder(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 65.dp, y = (-20).dp),
                shimmerBrush = shimmerBrush,
                categoryIcon = Icons.Default.Warning,
                labelWidth = 84.dp
            )

            // Station Marker Skeleton 3 (Sud - Boulevard du Mono / Port)
            ShimmerMarkerPlaceholder(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-15).dp, y = 70.dp),
                shimmerBrush = shimmerBrush,
                categoryIcon = Icons.Default.LocalGasStation,
                labelWidth = 64.dp
            )

            // Bottom Shimmer Service Card Skeleton
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .fillMaxWidth()
                    .testTag("shimmer_service_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row: Avatar + Title & Address bars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(shimmerBrush)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Title bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                            // Address bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                        }

                        // Status badge placeholder
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                    }

                    // Middle Row: Distance & ETA chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(shimmerBrush)
                        )
                    }

                    // Action buttons placeholder row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shimmering marker pin skeleton mimicking a dépanneur / garage pin during map data load.
 */
@Composable
private fun ShimmerMarkerPlaceholder(
    modifier: Modifier = Modifier,
    shimmerBrush: Brush,
    categoryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    labelWidth: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shimmer Pin Head
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(2.dp, shimmerBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Shimmer Label Pill
        Box(
            modifier = Modifier
                .width(labelWidth)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(shimmerBrush)
        )
    }
}
