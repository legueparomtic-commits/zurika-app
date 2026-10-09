package com.example.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class LocationType {
    CITY,
    DISTRICT,
    ADDRESS,
    LANDMARK
}

data class SearchResultLocation(
    val id: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val zoom: Float = 14.5f,
    val type: LocationType = LocationType.CITY
)

object LocationSearchHelper {

    // Preset of recognized Togolese cities, major West African hubs, and Lomé districts
    val PRESET_LOCATIONS = listOf(
        SearchResultLocation(
            id = "city_lome",
            name = "Lomé",
            subtitle = "Capitale du Togo • Région Maritime",
            latitude = 6.1375,
            longitude = 1.2125,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_kpalime",
            name = "Kpalimé",
            subtitle = "Grand carrefour • Région des Plateaux",
            latitude = 6.9070,
            longitude = 0.6280,
            zoom = 13.0f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_sokode",
            name = "Sokodé",
            subtitle = "Deuxième ville du Togo • Région Centrale",
            latitude = 8.9830,
            longitude = 1.1330,
            zoom = 13.0f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_kara",
            name = "Kara",
            subtitle = "Nord du Togo • Région de la Kara",
            latitude = 9.5510,
            longitude = 1.1860,
            zoom = 13.0f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_atakpame",
            name = "Atakpamé",
            subtitle = "Ville aux sept collines • Région des Plateaux",
            latitude = 7.5330,
            longitude = 1.1330,
            zoom = 13.0f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_dapaong",
            name = "Dapaong",
            subtitle = "Extrême Nord • Région des Savanes",
            latitude = 10.8630,
            longitude = 0.2070,
            zoom = 13.0f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_tsevie",
            name = "Tsévié",
            subtitle = "Chef-lieu • Région Maritime",
            latitude = 6.4270,
            longitude = 1.2130,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_aneho",
            name = "Aného",
            subtitle = "Ancienne capitale • Lac Togo",
            latitude = 6.2280,
            longitude = 1.5970,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_notse",
            name = "Notsé",
            subtitle = "Cité historique Ewe • Région des Plateaux",
            latitude = 6.9500,
            longitude = 1.1670,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_bassar",
            name = "Bassar",
            subtitle = "Région de la Kara",
            latitude = 9.2500,
            longitude = 0.7830,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_mango",
            name = "Mango (Sansanné-Mango)",
            subtitle = "Oti • Région des Savanes",
            latitude = 10.3590,
            longitude = 0.4730,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "city_tabligbo",
            name = "Tabligbo",
            subtitle = "Yoto • Région Maritime",
            latitude = 6.5830,
            longitude = 1.5000,
            zoom = 13.5f,
            type = LocationType.CITY
        ),
        SearchResultLocation(
            id = "dist_deckon",
            name = "Déckon",
            subtitle = "Centre d'affaires & Boulevard du 13 Janvier • Lomé",
            latitude = 6.1310,
            longitude = 1.2180,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_tokoin",
            name = "Tokoin",
            subtitle = "CHU Sylvanus Olympio, Aéroport & Habitat • Lomé",
            latitude = 6.1455,
            longitude = 1.2188,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_be",
            name = "Bè",
            subtitle = "Quartier historique, Marché & Lagune • Lomé",
            latitude = 6.1320,
            longitude = 1.2420,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_agoe",
            name = "Agoè-Nyivé",
            subtitle = "Carrefour 2 Lions, Assiyéyé & Échangeur • Lomé",
            latitude = 6.2080,
            longitude = 1.1950,
            zoom = 15.0f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_hedzranawoe",
            name = "Hedzranawoé",
            subtitle = "Grand Marché aux fripes, Aéroport • Lomé",
            latitude = 6.1680,
            longitude = 1.2350,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_adidogome",
            name = "Adidogomé",
            subtitle = "Université de Lomé Ouest, Douane & Kpalimé Route",
            latitude = 6.1620,
            longitude = 1.1550,
            zoom = 15.0f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_nyekonakpoe",
            name = "Nyékonakpoè",
            subtitle = "Zone des Ambassades & Frontière Ghana • Lomé",
            latitude = 6.1280,
            longitude = 1.2020,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_baguida",
            name = "Baguida",
            subtitle = "Route d'Aného, Plage & Résidences • Lomé",
            latitude = 6.1600,
            longitude = 1.3250,
            zoom = 15.0f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_kegue",
            name = "Kégué",
            subtitle = "Stade de Kégué & Cité Résidentielle • Lomé",
            latitude = 6.1750,
            longitude = 1.2320,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_kodjoviakope",
            name = "Kodjoviakopé",
            subtitle = "Plage & Poste Frontière Aflao • Lomé",
            latitude = 6.1200,
            longitude = 1.2050,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "dist_amoutieve",
            name = "Amoutiévé",
            subtitle = "Centre-ville & Marché Saint-Joseph • Lomé",
            latitude = 6.1360,
            longitude = 1.2260,
            zoom = 15.5f,
            type = LocationType.DISTRICT
        ),
        SearchResultLocation(
            id = "lm_aeroport",
            name = "Aéroport International Gnassingbé Eyadéma (AIGE)",
            subtitle = "Terminal Aérien • Lomé, Togo",
            latitude = 6.1650,
            longitude = 1.2540,
            zoom = 15.0f,
            type = LocationType.LANDMARK
        ),
        SearchResultLocation(
            id = "lm_port",
            name = "Port Autonome de Lomé (PAL)",
            subtitle = "Zone Portuaire en eau profonde • Lomé, Togo",
            latitude = 6.1400,
            longitude = 1.2800,
            zoom = 15.0f,
            type = LocationType.LANDMARK
        ),
        SearchResultLocation(
            id = "lm_grand_marche",
            name = "Grand Marché de Lomé (Assigamé)",
            subtitle = "Cathédrale & Grand Marché • Lomé, Togo",
            latitude = 6.1250,
            longitude = 1.2230,
            zoom = 16.0f,
            type = LocationType.LANDMARK
        ),
        SearchResultLocation(
            id = "addr_bd_13_janvier",
            name = "Boulevard du 13 Janvier",
            subtitle = "Boulevard Circulaire • Lomé, Togo",
            latitude = 6.1295,
            longitude = 1.2160,
            zoom = 16.0f,
            type = LocationType.ADDRESS
        ),
        SearchResultLocation(
            id = "addr_av_liberation",
            name = "Avenue de la Libération",
            subtitle = "Centre administratif • Lomé, Togo",
            latitude = 6.1330,
            longitude = 1.2210,
            zoom = 16.0f,
            type = LocationType.ADDRESS
        )
    )

    /**
     * Search preset cities/districts/addresses matching query.
     */
    fun searchPresets(query: String): List<SearchResultLocation> {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return emptyList()
        return PRESET_LOCATIONS.filter {
            it.name.lowercase().contains(clean) ||
            it.subtitle.lowercase().contains(clean)
        }
    }

    /**
     * Suspend function resolving an address or city via Android Geocoder
     * with fallback to preset locations if offline or Geocoder returns empty.
     */
    suspend fun resolveLocation(query: String, context: Context): SearchResultLocation? = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext null

        // 1. Try exact or best match from presets first
        val presetMatch = PRESET_LOCATIONS.firstOrNull {
            it.name.equals(clean, ignoreCase = true)
        } ?: PRESET_LOCATIONS.firstOrNull {
            it.name.contains(clean, ignoreCase = true) || clean.contains(it.name, ignoreCase = true)
        }

        if (presetMatch != null) {
            return@withContext presetMatch
        }

        // 2. Try Android Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultList: List<Address>? = null
                    val lock = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocationName(clean, 3, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            resultList = addresses
                            lock.countDown()
                        }
                        override fun onError(errorMessage: String?) {
                            lock.countDown()
                        }
                    })
                    lock.await(2500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    resultList
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(clean, 3)
                }

                val first = addresses?.firstOrNull()
                if (first != null && first.hasLatitude() && first.hasLongitude()) {
                    val addressLine = first.getAddressLine(0) ?: "${first.locality ?: clean}, ${first.countryName ?: ""}"
                    return@withContext SearchResultLocation(
                        id = "geo_${first.latitude}_${first.longitude}",
                        name = first.locality ?: first.featureName ?: clean,
                        subtitle = addressLine,
                        latitude = first.latitude,
                        longitude = first.longitude,
                        zoom = if (first.locality != null && first.thoroughfare == null) 13.5f else 15.5f,
                        type = LocationType.ADDRESS
                    )
                }
            }
        } catch (e: Exception) {
            // Geocoder service may not be available on some devices/emulators
        }

        // 3. Fallback to partial preset search
        searchPresets(clean).firstOrNull()
    }
}
