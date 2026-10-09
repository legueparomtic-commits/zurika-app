package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServiceCategory
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.getAppStrings
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val category: ServiceCategory,
    val titleFr: String,
    val titleEn: String,
    val subtitleFr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accentColor: Color,
    val drawableResName: String,
    val highlightsFr: List<String>,
    val highlightsEn: List<String>
)

val onboardingSlides = listOf(
    OnboardingSlide(
        category = ServiceCategory.STATION,
        titleFr = "Stations-Service & Carburant",
        titleEn = "Gas Stations & Fuel Availability",
        subtitleFr = "Localisez en temps réel les stations ouvertes 24h/24 à Lomé et au Togo avec le suivi des stocks de carburant.",
        subtitleEn = "Locate 24/7 open gas stations across Lomé and Togo with real-time fuel availability.",
        icon = Icons.Default.LocalGasStation,
        accentColor = ZurikaPrimary,
        drawableResName = "img_station_banner_1786356166469",
        highlightsFr = listOf(
            "Disponibilité Super & Gazole en direct",
            "Filtre par stations ouvertes 24h/24",
            "Itinéraire GPS optimisé"
        ),
        highlightsEn = listOf(
            "Live Petrol & Diesel availability",
            "Filter 24/7 open stations",
            "Optimized GPS navigation"
        )
    ),
    OnboardingSlide(
        category = ServiceCategory.MECHANIC,
        titleFr = "Mécanique Auto & Moto",
        titleEn = "Auto & Moto Mechanics",
        subtitleFr = "Trouvez un garage agréé ou un mécanicien ambulant qualifié pour un diagnostic ou une réparation rapide.",
        subtitleEn = "Find verified garages and mobile mechanics for fast diagnostics and roadside repair.",
        icon = Icons.Default.Build,
        accentColor = ZurikaAmber,
        drawableResName = "img_mechanic_banner_1786356179978",
        highlightsFr = listOf(
            "Mécaniciens certifiés ZURIKA",
            "Intervention d'urgence sur place",
            "Avis & notes vérifiés"
        ),
        highlightsEn = listOf(
            "ZURIKA certified mechanics",
            "On-site emergency repair",
            "Verified user ratings & reviews"
        )
    ),
    OnboardingSlide(
        category = ServiceCategory.VULCANIZER,
        titleFr = "Vulcanisateur & Pneus 24h",
        titleEn = "24/7 Tire Repair & Vulcanizer",
        subtitleFr = "Une crevaison sur la route ? Trouvez immédiatement le vulcanisateur le plus proche pour gonflage ou colmatage.",
        subtitleEn = "Puncture on the road? Locate the nearest tire repair workshop for instant inflation and patch.",
        icon = Icons.Default.DirectionsCar,
        accentColor = ZurikaStatusOpen,
        drawableResName = "img_onboarding_hero_1786364104947",
        highlightsFr = listOf(
            "Dépannage crevaison express",
            "Services ouverts nuit & jour",
            "Couverture nationale au Togo"
        ),
        highlightsEn = listOf(
            "Express puncture repair",
            "24/7 night & day service",
            "Nationwide coverage across Togo"
        )
    ),
    OnboardingSlide(
        category = ServiceCategory.TOWING,
        titleFr = "Dépannage & Remorquage Urgent",
        titleEn = "SOS Emergency Towing",
        subtitleFr = "Envoyez une alerte SOS géolocalisée avec votre position exacte pour un remorquage rapide par camion plateau.",
        subtitleEn = "Send a geotagged SOS alert with your exact position for fast flatbed towing truck dispatch.",
        icon = Icons.Default.Agriculture,
        accentColor = ZurikaStatusClosed,
        drawableResName = "img_towing_banner_1786356193954",
        highlightsFr = listOf(
            "Alerte SOS géolocalisée en 1 clic",
            "Suivi de l'intervenant en temps réel",
            "Historique sauvegardé hors-ligne"
        ),
        highlightsEn = listOf(
            "1-click geotagged SOS alert",
            "Real-time tow truck GPS tracking",
            "Offline local history saved in Room"
        )
    )
)

@Composable
fun OnboardingCarouselScreen(
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    onLanguageToggle: (AppLanguage) -> Unit = {},
    onFinishOnboarding: (ServiceCategory?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { onboardingSlides.size })
    val isLastPage = pagerState.currentPage == onboardingSlides.size - 1

    val strings = remember(currentLanguage) { getAppStrings(currentLanguage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_carousel_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Toggle Pill
                Surface(
                    onClick = {
                        val nextLang = if (currentLanguage == AppLanguage.FRENCH) AppLanguage.ENGLISH else AppLanguage.FRENCH
                        onLanguageToggle(nextLang)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentLanguage.flag, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentLanguage.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Skip Button ("Passer")
                TextButton(
                    onClick = { onFinishOnboarding(null) },
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Passer" else "Skip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )
                }
            }

            // Main ViewPager with 4 Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val slide = onboardingSlides[page]
                OnboardingSlideContent(
                    slide = slide,
                    language = currentLanguage,
                    onSelectCategory = {
                        onFinishOnboarding(slide.category)
                    }
                )
            }

            // Bottom Navigation & Controls
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        repeat(onboardingSlides.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 28.dp else 8.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "dot_width"
                            )
                            val color = if (isSelected) onboardingSlides[pagerState.currentPage].accentColor else Color.Gray.copy(alpha = 0.4f)

                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Navigation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button (if page > 0)
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("onboarding_prev_button"),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = Color.White
                                )
                            }
                        }

                        // Primary Next / Finish Button
                        val currentAccent = onboardingSlides[pagerState.currentPage].accentColor
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onFinishOnboarding(null)
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isLastPage) {
                                        if (currentLanguage == AppLanguage.FRENCH) "Explorer ZURIKA Togo" else "Explore ZURIKA Togo"
                                    } else {
                                        if (currentLanguage == AppLanguage.FRENCH) "Suivant" else "Next"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isLastPage) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(
    slide: OnboardingSlide,
    language: AppLanguage,
    onSelectCategory: () -> Unit
) {
    val context = LocalContext.current
    val title = if (language == AppLanguage.FRENCH) slide.titleFr else slide.titleEn
    val subtitle = if (language == AppLanguage.FRENCH) slide.subtitleFr else slide.subtitleEn
    val highlights = if (language == AppLanguage.FRENCH) slide.highlightsFr else slide.highlightsEn

    val imageResId = remember(slide.drawableResName) {
        context.resources.getIdentifier(slide.drawableResName, "drawable", context.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Hero Image Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, slide.accentColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dark Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DarkBackground.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(DarkSurface, slide.accentColor.copy(alpha = 0.2f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = slide.accentColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Category Floating Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = slide.accentColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Service ZURIKA #${slide.category.ordinal + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Slide Title
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Slide Subtitle
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Highlights Bullet Checklist
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                highlights.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = slide.accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = slide.accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Direct Category Shortcut Button
        OutlinedButton(
            onClick = onSelectCategory,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, slide.accentColor.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = slide.accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.FRENCH) "Afficher directement ces services sur la carte" else "View these services on map",
                color = slide.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
