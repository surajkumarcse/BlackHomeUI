package com.lostfalcon.myminimalistdesignhomepage

import android.content.SharedPreferences
import java.util.Locale

internal const val PREFERENCES_NAME = "launcher_preferences"
private const val FAVORITES_KEY = "favorite_packages"
private const val HIDDEN_APPS_KEY = "hidden_apps"
private const val APP_LABEL_PREFIX = "app_label:"

internal class LauncherPreferences(private val preferences: SharedPreferences) {
    fun favoriteAppIds(apps: List<LauncherApp>): Set<String> {
        if (preferences.contains(FAVORITES_KEY)) {
            val stored = preferences.getStringSet(FAVORITES_KEY, emptySet()).orEmpty().toSet()
            val migrated = apps
                .filter { it.id in stored || it.packageName in stored }
                .mapTo(mutableSetOf()) { it.id }
            if (migrated != stored) {
                preferences.edit().putStringSet(FAVORITES_KEY, migrated).apply()
            }
            return migrated
        }

        val preferredNames = listOf(
            "google calendar", "calendar", "notion", "gpay", "google pay", "messages", "maps",
        )
        val defaults = preferredNames.mapNotNull { name ->
            apps.firstOrNull { it.label.equals(name, ignoreCase = true) }?.id
        }.toSet()
        preferences.edit().putStringSet(FAVORITES_KEY, defaults).apply()
        return defaults
    }

    fun toggleFavorite(favorites: Set<String>, appId: String): Set<String> =
        toggleMembership(FAVORITES_KEY, favorites, appId)

    fun hiddenAppIds(): Set<String> =
        preferences.getStringSet(HIDDEN_APPS_KEY, emptySet()).orEmpty().toSet()

    fun toggleHidden(hiddenApps: Set<String>, appId: String): Set<String> =
        toggleMembership(HIDDEN_APPS_KEY, hiddenApps, appId)

    fun appLabels(apps: List<LauncherApp>): Map<String, String> = buildMap {
        apps.forEach { app ->
            preferences.getString("$APP_LABEL_PREFIX${app.id}", null)
                ?.takeIf { it.isNotBlank() }
                ?.let { put(app.id, it) }
        }
    }

    fun renameApp(appId: String, label: String?) {
        val editor = preferences.edit()
        val normalizedLabel = label?.trim()
        if (normalizedLabel.isNullOrEmpty()) {
            editor.remove("$APP_LABEL_PREFIX$appId")
        } else {
            editor.putString("$APP_LABEL_PREFIX$appId", normalizedLabel)
        }
        editor.apply()
    }

    private fun toggleMembership(key: String, current: Set<String>, appId: String): Set<String> {
        val updated = current.toMutableSet().apply {
            if (!add(appId)) remove(appId)
        }.toSet()
        preferences.edit().putStringSet(key, updated).apply()
        return updated
    }
}

internal fun applyAppLabels(
    apps: List<LauncherApp>,
    labels: Map<String, String>,
): List<LauncherApp> = apps
    .map { app -> labels[app.id]?.let { app.copy(label = it) } ?: app }
    .sortedBy { it.label.lowercase(Locale.getDefault()) }
