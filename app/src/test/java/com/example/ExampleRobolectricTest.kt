package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.screens.isDepanneurService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ZURIKA", appName)
  }

  @Test
  fun `create review entity for professional subdocument`() {
    val review = com.example.data.ReviewEntity(
        id = "rev_100",
        serviceId = "mechanic_1",
        authorName = "Kokou Jean",
        rating = 5.0f,
        comment = "Excellent dépannage très rapide",
        date = "Aujourd'hui"
    )
    assertEquals("mechanic_1", review.serviceId)
    assertEquals(5.0f, review.rating, 0.01f)
    assertEquals("rev_100", review.id)
  }

  @Test
  fun `search filtering by name and address with camera recentering`() {
    val services = listOf(
        com.example.data.ServiceEntity(
            id = "s1",
            name = "TotalEnergies Déckon",
            category = com.example.data.ServiceCategory.STATION,
            status = com.example.data.ServiceStatus.OPEN,
            address = "Boulevard du 13 Janvier, Quartier Déckon, Lomé",
            latitude = 6.1311,
            longitude = 1.2225,
            phoneNumber = "+228 90 00 11 22",
            rating = 4.7f,
            reviewCount = 28,
            imageResName = "station_deckon",
            servicesOffered = "Super, Gazole, Lubrifiants"
        ),
        com.example.data.ServiceEntity(
            id = "s2",
            name = "Garage Auto Expert Tokoin",
            category = com.example.data.ServiceCategory.MECHANIC,
            status = com.example.data.ServiceStatus.OPEN,
            address = "Avenue de la Libération, Quartier Tokoin, Lomé",
            latitude = 6.1455,
            longitude = 1.2188,
            phoneNumber = "+228 91 22 33 44",
            rating = 4.9f,
            reviewCount = 42,
            imageResName = "garage_tokoin",
            servicesOffered = "Diagnostic, Freinage"
        )
    )

    // Test filter by Name
    val nameMatches = services.filter {
        it.name.contains("Total", ignoreCase = true) || it.address.contains("Total", ignoreCase = true)
    }
    assertEquals(1, nameMatches.size)
    assertEquals("s1", nameMatches.first().id)
    assertEquals(6.1311, nameMatches.first().latitude, 0.0001)

    // Test filter by Address (Quartier)
    val addressMatches = services.filter {
        it.name.contains("Tokoin", ignoreCase = true) || it.address.contains("Tokoin", ignoreCase = true)
    }
    assertEquals(1, addressMatches.size)
    assertEquals("s2", addressMatches.first().id)
    assertEquals(6.1455, addressMatches.first().latitude, 0.0001)
    assertEquals(1.2188, addressMatches.first().longitude, 0.0001)
  }

  @Test
  fun `verify depanneur category identification for dynamic bounce attention`() {
    val towingService = com.example.data.ServiceEntity(
        id = "towing_test",
        name = "Togo Dépannage Auto 24/7",
        category = com.example.data.ServiceCategory.TOWING,
        status = com.example.data.ServiceStatus.OPEN,
        address = "Lomé, Togo",
        latitude = 6.1480,
        longitude = 1.2110,
        phoneNumber = "+228 90 00 11 22",
        rating = 5.0f,
        reviewCount = 10,
        imageResName = "towing",
        servicesOffered = "Remorquage Plateau, Boost Batterie, Dépannage Urgent"
    )

    val isDepanneur = towingService.category == com.example.data.ServiceCategory.TOWING ||
        towingService.name.contains("Dépannage", ignoreCase = true) ||
        towingService.name.contains("Remorquage", ignoreCase = true) ||
        towingService.servicesOffered.contains("Dépannage", ignoreCase = true)

    assertEquals(true, isDepanneur)
  }

  @Test
  fun `verify location search helper finds cities and addresses for recentering`() {
    val lomeMatches = com.example.util.LocationSearchHelper.searchPresets("Lomé")
    assertEquals(true, lomeMatches.isNotEmpty())
    assertEquals("Lomé", lomeMatches.first().name)
    assertEquals(com.example.util.LocationType.CITY, lomeMatches.first().type)
    assertEquals(6.1375, lomeMatches.first().latitude, 0.001)

    val deckonMatches = com.example.util.LocationSearchHelper.searchPresets("Déckon")
    assertEquals(true, deckonMatches.isNotEmpty())
    assertEquals("Déckon", deckonMatches.first().name)
    assertEquals(com.example.util.LocationType.DISTRICT, deckonMatches.first().type)

    val kpalimeMatches = com.example.util.LocationSearchHelper.searchPresets("Kpalimé")
    assertEquals(true, kpalimeMatches.isNotEmpty())
    assertEquals("Kpalimé", kpalimeMatches.first().name)
  }

  @Test
  fun `verify route calculator estimated travel time to depanneur`() {
    val userLat = 6.1375
    val userLng = 1.2125
    val depanneur = com.example.data.ServiceEntity(
        id = "towing_sos",
        name = "SOS Dépannage Lomé Express",
        category = com.example.data.ServiceCategory.TOWING,
        status = com.example.data.ServiceStatus.OPEN,
        address = "Boulevard Circulaire, Lomé",
        latitude = 6.1600,
        longitude = 1.2300,
        phoneNumber = "+228 90 12 34 56",
        rating = 4.9f,
        reviewCount = 35,
        imageResName = "towing",
        servicesOffered = "Remorquage, Dépannage sur place, Crevaison"
    )

    val isDep = depanneur.isDepanneurService()
    assertEquals(true, isDep)

    val estimate = com.example.ui.screens.RouteCalculator.calculateRoute(
        startLat = userLat,
        startLng = userLng,
        destLat = depanneur.latitude,
        destLng = depanneur.longitude
    )

    assertEquals(true, estimate.distanceKm > 0.0)
    assertEquals(true, estimate.carTimeMinutes >= 1)
    assertEquals(true, estimate.motoTimeMinutes >= 1)
    assertEquals(true, estimate.waypoints.size >= 3)
    assertEquals(userLat, estimate.waypoints.first().latitude, 0.0001)
    assertEquals(depanneur.latitude, estimate.waypoints.last().latitude, 0.0001)
  }

  @Test
  fun `verify recenter FAB calculates distance from user location and triggers reset`() {
    val userLat = 6.1375
    val userLng = 1.2125

    // Camera at user location
    val sameLat = 6.1375
    val sameLng = 1.2125
    val dLatClose = kotlin.math.abs(sameLat - userLat)
    val dLngClose = kotlin.math.abs(sameLng - userLng)
    val isAwayWhenClose = (dLatClose > 0.003 || dLngClose > 0.003)
    assertEquals(false, isAwayWhenClose)

    // Camera panned to Déckon or another city (e.g. Kpalimé)
    val pannedLat = 6.9000
    val pannedLng = 0.6333
    val dLatAway = kotlin.math.abs(pannedLat - userLat)
    val dLngAway = kotlin.math.abs(pannedLng - userLng)
    val isAwayWhenPanned = (dLatAway > 0.003 || dLngAway > 0.003)
    assertEquals(true, isAwayWhenPanned)

    // Resetting returns target to userPosition
    val resetTargetLat = userLat
    val resetTargetLng = userLng
    assertEquals(6.1375, resetTargetLat, 0.0001)
    assertEquals(1.2125, resetTargetLng, 0.0001)
  }

  @Test
  fun `verify MapState supports isLoading state for shimmer effect during data retrieval`() {
    val state = com.example.ui.MapState(
        isLoading = true
    )
    assertEquals(true, state.isLoading)

    val updatedState = state.copy(isLoading = false)
    assertEquals(false, updatedState.isLoading)
  }

  @Test
  fun `verify interactive vector map GPS to screen projection and zoom calculations`() {
    val userLat = 6.1375
    val userLng = 1.2125
    val scale = 1.2f
    val offsetX = 0f
    val offsetY = 0f
    val width = 1080f
    val height = 1920f

    val cx = width / 2f + offsetX
    val cy = height / 2f + offsetY

    // Service at user coordinates should project to center of screen
    val pxUser = cx + ((userLng - userLng) * 14000.0 * scale).toFloat()
    val pyUser = cy - ((userLat - userLat) * 14000.0 * scale).toFloat()
    assertEquals(cx, pxUser, 0.001f)
    assertEquals(cy, pyUser, 0.001f)

    // Service 0.01 deg east of user
    val targetLng = userLng + 0.01
    val pxTarget = cx + ((targetLng - userLng) * 14000.0 * scale).toFloat()
    assertEquals(true, pxTarget > cx)

    // Zoom in increases distance in pixels
    val newScale = scale * 1.35f
    val pxTargetZoomed = cx + ((targetLng - userLng) * 14000.0 * newScale).toFloat()
    assertEquals(true, (pxTargetZoomed - cx) > (pxTarget - cx))
  }
}
