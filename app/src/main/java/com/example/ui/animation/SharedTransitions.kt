package com.example.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * CompositionLocals to cleanly propagate SharedTransitionScope and AnimatedVisibilityScope
 * across nested map composables, markers, preview cards, list items and detail screens.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val FluidSpringBoundsTransform: BoundsTransform = BoundsTransform { initialBounds, targetBounds ->
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
val FluidGentleBoundsTransform: BoundsTransform = BoundsTransform { initialBounds, targetBounds ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Shared container bounds transformation between map popup card / list item and service details sheet.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceCard(
    serviceId: String,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceCard.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "service_card_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidSpringBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        this
    }
}

/**
 * Shared hero banner image / icon avatar transformation between map popup and details sheet header.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceHero(
    serviceId: String,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceHero.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "service_hero_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidSpringBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        this
    }
}

/**
 * Shared typography element for service title.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceTitle(
    serviceId: String
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceTitle.sharedElement(
                state = rememberSharedContentState(key = "service_title_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidGentleBoundsTransform
            )
        }
    } else {
        this
    }
}

/**
 * Shared status badge element (OUVERT / FERMÉ / OCCUPÉ).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceStatus(
    serviceId: String
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceStatus.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "service_status_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidSpringBoundsTransform
            )
        }
    } else {
        this
    }
}

/**
 * Shared rating badge element.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceRating(
    serviceId: String
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceRating.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "service_rating_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidGentleBoundsTransform
            )
        }
    } else {
        this
    }
}

/**
 * Shared actions row element.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedServiceActions(
    serviceId: String
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedServiceActions.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "service_actions_$serviceId"),
                animatedVisibilityScope = animatedScope,
                boundsTransform = FluidSpringBoundsTransform
            )
        }
    } else {
        this
    }
}
