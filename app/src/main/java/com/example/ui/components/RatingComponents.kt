package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Interactive 1-5 Star Rating Selector with smooth spring bounce animation.
 */
@Composable
fun InteractiveStarRatingSelector(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 38.dp,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.testTag("interactive_star_rating_selector"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { starIndex ->
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 1.28f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "star_scale_$starIndex"
            )

            val isFilled = starIndex <= rating
            val starColor by animateColorAsState(
                targetValue = if (isFilled) ZurikaAmber else Color.Gray.copy(alpha = 0.4f),
                label = "star_color_$starIndex"
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .clickable(enabled = enabled) {
                        isPressed = true
                        onRatingChange(starIndex.toFloat())
                    }
                    .padding(4.dp)
                    .testTag("star_rate_button_$starIndex"),
                contentAlignment = Alignment.Center
            ) {
                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        kotlinx.coroutines.delay(160)
                        isPressed = false
                    }
                }

                Icon(
                    imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Étoile $starIndex",
                    tint = starColor,
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}

/**
 * Descriptive assessment text paired with current star rating.
 */
@Composable
fun RatingAppraisalBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    val (label, color, icon) = when {
        rating >= 4.8f -> Triple("5.0 • Prestation Exceptionnelle ! 🌟", ZurikaAmber, "⭐")
        rating >= 4.0f -> Triple("4.0 • Très Satisfait 👍", ZurikaStatusOpen, "👍")
        rating >= 3.0f -> Triple("3.0 • Conforme aux attentes 👌", ZurikaPrimary, "👌")
        rating >= 2.0f -> Triple("2.0 • Prestation moyenne ⚠️", Color(0xFFFF9800), "⚠️")
        else -> Triple("1.0 • Insatisfaisant / Problème rencontré 🚨", ZurikaStatusClosed, "🚨")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier.testTag("rating_appraisal_badge")
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Quick feedback chips to quickly describe positive or constructive aspects of the intervention.
 */
val DEFAULT_INTERVENTION_FEEDBACK_TAGS = listOf(
    "⏱️ Très ponctuel",
    "🛠️ Travail soigné",
    "💰 Prix transparent & juste",
    "⚡ Dépannage ultra-rapide",
    "🤝 Aimable & Professionnel",
    "🧰 Matériel de qualité",
    "🚗 Véhicule réparé sur place"
)

@Composable
fun QuickFeedbackTagsSelector(
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    availableTags: List<String> = DEFAULT_INTERVENTION_FEEDBACK_TAGS
) {
    Column(modifier = modifier) {
        Text(
            text = "Points forts de l'intervention :",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(availableTags) { tag ->
                val isSelected = selectedTags.contains(tag)
                Surface(
                    onClick = { onTagToggle(tag) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ZurikaPrimary else DarkSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ZurikaPrimary else Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("tag_chip_${tag.take(8)}")
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reassuring indicator showing storage into Firestore sub-document.
 */
@Composable
fun FirestoreSubDocumentIndicator(
    serviceId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Firestore Sync",
                tint = ZurikaStatusOpen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Stocké sur Firestore : /services/$serviceId/reviews",
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}
