package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val description: String
) {
    FRENCH("fr", "Français", "🇫🇷", "Langue officielle du Togo"),
    ENGLISH("en", "English", "🇬🇧", "International Language")
}

data class AppStrings(
    val profileTitle: String,
    val languageSectionTitle: String,
    val languageSectionSubtitle: String,
    val tabInterventions: String,
    val tabSearches: String,
    val tabFavorites: String,
    val offlineCacheActive: String,
    val logout: String,
    val deleteAccount: String,
    val confirmDeleteTitle: String,
    val confirmDeleteMessage: String,
    val confirmButton: String,
    val cancelButton: String,
    val searchPlaceholder: String,
    val findNearest: String,
    val requestAssistance: String,
    val recentSearchesHeader: String,
    val clearAll: String,
    val noInterventions: String,
    val noSearches: String,
    val noFavorites: String,
    val statusPending: String,
    val statusAccepted: String,
    val statusEnRoute: String,
    val statusCompleted: String,
    val statusCancelled: String
)

val FrenchStrings = AppStrings(
    profileTitle = "Mon Profil & Cache Room",
    languageSectionTitle = "Langue de l'application",
    languageSectionSubtitle = "Sélectionnez votre langue d'affichage pour une expérience optimale au Togo.",
    tabInterventions = "Dépannages",
    tabSearches = "Recherches",
    tabFavorites = "Favoris",
    offlineCacheActive = "Mise en cache locale Room active (Mode Hors-Ligne)",
    logout = "Déconnexion",
    deleteAccount = "Supprimer le compte",
    confirmDeleteTitle = "Supprimer mon compte ?",
    confirmDeleteMessage = "Êtes-vous sûr de vouloir supprimer définitivement votre compte ZURIKA ? Vos données de profil, favoris et historique seront effacés.",
    confirmButton = "Confirmer",
    cancelButton = "Annuler",
    searchPlaceholder = "Rechercher une station, un mécanicien...",
    findNearest = "Trouver le plus proche",
    requestAssistance = "Urgence Dépannage",
    recentSearchesHeader = "Dernières recherches (Cache Room) :",
    clearAll = "Effacer tout",
    noInterventions = "Aucune demande de dépannage enregistrée.",
    noSearches = "Aucune recherche récente enregistrée.",
    noFavorites = "Aucun établissement en favori.",
    statusPending = "En attente",
    statusAccepted = "Accepté",
    statusEnRoute = "En route",
    statusCompleted = "Terminé",
    statusCancelled = "Annulé"
)

val EnglishStrings = AppStrings(
    profileTitle = "My Profile & Room Cache",
    languageSectionTitle = "Application Language",
    languageSectionSubtitle = "Select your preferred language for the best user experience in Togo.",
    tabInterventions = "Assistance",
    tabSearches = "Searches",
    tabFavorites = "Favorites",
    offlineCacheActive = "Active Room Local Cache (Offline Mode)",
    logout = "Log Out",
    deleteAccount = "Delete Account",
    confirmDeleteTitle = "Delete my account?",
    confirmDeleteMessage = "Are you sure you want to permanently delete your ZURIKA account? Your profile data, favorites, and history will be cleared.",
    confirmButton = "Confirm",
    cancelButton = "Cancel",
    searchPlaceholder = "Search for gas station, mechanic...",
    findNearest = "Find Nearest",
    requestAssistance = "Emergency Assistance",
    recentSearchesHeader = "Recent Searches (Room Cache):",
    clearAll = "Clear All",
    noInterventions = "No emergency assistance history found.",
    noSearches = "No recent searches found.",
    noFavorites = "No saved favorite places.",
    statusPending = "Pending",
    statusAccepted = "Accepted",
    statusEnRoute = "En Route",
    statusCompleted = "Completed",
    statusCancelled = "Cancelled"
)

fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.FRENCH -> FrenchStrings
        AppLanguage.ENGLISH -> EnglishStrings
    }
}
