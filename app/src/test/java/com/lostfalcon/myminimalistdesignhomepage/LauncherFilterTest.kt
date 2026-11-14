package com.lostfalcon.myminimalistdesignhomepage

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherFilterTest {
    private val apps = listOf(
        LauncherApp("Calendar", "calendar"),
        LauncherApp("Google Maps", "maps"),
        LauncherApp("Notion", "notion"),
    )

    @Test
    fun emptyQueryReturnsAllApps() {
        assertEquals(apps, filterApps(apps, "  "))
    }

    @Test
    fun queryFiltersLabelsIgnoringCase() {
        assertEquals(
            listOf(LauncherApp("Google Maps", "maps")),
            filterApps(apps, "MAP"),
        )
    }

    @Test
    fun queryFindsWorkProfileSuffix() {
        val workApp = LauncherApp("Teams (WORK)", "teams", id = "10:teams")

        assertEquals(listOf(workApp), filterApps(listOf(workApp), "work"))
    }

    @Test
    fun renamedAppsAreSortedAndSearchableByTheirNewName() {
        val renamed = applyAppLabels(apps, mapOf("notion" to "A notebook"))

        assertEquals(listOf("A notebook", "Calendar", "Google Maps"), renamed.map { it.label })
        assertEquals(listOf(apps[2].copy(label = "A notebook")), filterApps(renamed, "notebook"))
    }

    @Test
    fun renameDoesNotAffectAnotherProfileWithTheSamePackage() {
        val personal = LauncherApp("Calendar", "calendar", id = "0:calendar")
        val work = LauncherApp("Calendar (WORK)", "calendar", id = "10:calendar")

        val renamed = applyAppLabels(listOf(personal, work), mapOf(personal.id to "Agenda"))

        assertEquals(listOf(personal.copy(label = "Agenda"), work), renamed)
    }
}
