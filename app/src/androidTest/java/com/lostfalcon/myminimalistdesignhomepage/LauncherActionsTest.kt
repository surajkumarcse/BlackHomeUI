package com.lostfalcon.myminimalistdesignhomepage

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lostfalcon.myminimalistdesignhomepage.ui.theme.MyMinimalistDesignHomePageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calendar = LauncherApp("Calendar", "calendar", id = "0:calendar")
    private val workCalendar = LauncherApp("Calendar (WORK)", "calendar", id = "10:calendar")
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun onlyCompletedUpwardSwipesOpenSearch() {
        var searches = 0
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                HomeScreen(emptyList(), {}, {}, { searches++ }, {}, {})
            }
        }
        val home = composeRule.onNodeWithTag("home-screen")
        home.performTouchInput { swipeDown() }
        home.performTouchInput { swipeLeft() }
        home.performTouchInput { swipe(center, center - Offset(0f, 20f)) }
        home.performTouchInput {
            down(center)
            moveBy(Offset(0f, -height * .4f))
            cancel()
        }
        composeRule.runOnIdle { assertEquals(0, searches) }

        home.performTouchInput { swipeUp() }
        composeRule.runOnIdle { assertEquals(1, searches) }
    }

    @Test
    fun homeFavoriteSupportsTapLongPressAndUpwardSwipe() {
        var launches = 0
        var menus = 0
        var searches = 0
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                HomeScreen(listOf(calendar), { launches++ }, { menus++ }, { searches++ }, {}, {})
            }
        }
        val favorite = composeRule.onNodeWithText("Calendar")
        favorite.performClick()
        favorite.performTouchInput { longClick() }
        composeRule.runOnIdle {
            assertEquals(1, launches)
            assertEquals(1, menus)
        }
        favorite.performTouchInput {
            swipe(center, center - Offset(0f, 500f), durationMillis = 250)
        }
        composeRule.runOnIdle {
            assertEquals(1, searches)
            assertEquals(1, launches)
            assertEquals(1, menus)
        }
    }

    @Test
    fun drawerLongPressRequestsMenuWithoutLaunchingOrTogglingFavorite() {
        var selected: LauncherApp? = null
        var launches = 0
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                AppsScreen(listOf(calendar), false, setOf(calendar.id), emptySet(), { launches++ }, { selected = it })
            }
        }
        composeRule.onNodeWithText("Calendar").performTouchInput { longClick() }
        composeRule.onNodeWithText("HOME").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(calendar, selected)
            assertEquals(0, launches)
        }
        composeRule.onNodeWithText("Calendar").performClick()
        composeRule.runOnIdle { assertEquals(1, launches) }
    }

    @Test
    fun hiddenAppsRemainAccessibleInTheirManagementView() {
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                AppsScreen(listOf(calendar, workCalendar), false, emptySet(), setOf(workCalendar.id), {}, {})
            }
        }
        composeRule.onNodeWithText(workCalendar.label).assertDoesNotExist()
        composeRule.onNodeWithText("Hidden apps").performClick()
        composeRule.onNodeWithText(workCalendar.label).assertIsDisplayed()
        composeRule.onNodeWithText(calendar.label).assertDoesNotExist()
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.onNodeWithText(calendar.label).assertIsDisplayed()
        composeRule.onNodeWithText(workCalendar.label).assertDoesNotExist()
    }

    @Test
    fun menuRoutesActionsAndReflectsFavoriteAndHiddenState() {
        var isFavorite by mutableStateOf(false)
        var isHidden by mutableStateOf(false)
        var renameRequests = 0
        var infoRequests = 0
        var uninstallRequests = 0
        var dismissRequests = 0
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                AppActionsDialog(
                    calendar, isFavorite, isHidden,
                    onDismiss = { dismissRequests++ },
                    onFavoriteToggle = { isFavorite = !isFavorite },
                    onRename = { renameRequests++ },
                    onHiddenToggle = { isHidden = !isHidden },
                    onAppInfo = { infoRequests++ },
                    onUninstall = { uninstallRequests++ },
                )
            }
        }
        composeRule.onNodeWithText("Add to home").performClick()
        composeRule.onNodeWithText("Remove from home").assertIsDisplayed()
        composeRule.onNodeWithText("Hide app").performClick()
        composeRule.onNodeWithText("Unhide app").assertIsDisplayed()
        composeRule.onNodeWithText("Rename app").performClick()
        composeRule.onNodeWithText("App info").performClick()
        composeRule.onNodeWithText("Uninstall").performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.runOnIdle {
            assertTrue(isFavorite)
            assertTrue(isHidden)
            assertEquals(1, renameRequests)
            assertEquals(1, infoRequests)
            assertEquals(1, uninstallRequests)
            assertEquals(1, dismissRequests)
        }
    }

    @Test
    fun renameRejectsBlankNamesTrimsInputAndCanRestoreOriginal() {
        var saved: String? = "not saved"
        composeRule.setContent {
            MyMinimalistDesignHomePageTheme {
                RenameAppDialog("Calendar", "Agenda", {}, { saved = it })
            }
        }
        composeRule.onNodeWithText("Agenda").performTextReplacement("   ")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("   ").performTextReplacement("  My agenda  ")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.runOnIdle { assertEquals("My agenda", saved) }
        composeRule.onNodeWithText("Restore original name").performClick()
        composeRule.runOnIdle { assertNull(saved) }
    }

    @Test
    fun searchUsesWebSearchAndOnlyFallsBackWhenNeeded() {
        val intents = mutableListOf<Intent>()
        launchWebSearch(context) {
            intents += it
            true
        }
        assertEquals(1, intents.size)
        assertEquals(Intent.ACTION_WEB_SEARCH, intents.single().action)
        assertEquals("", intents.single().getStringExtra(SearchManager.QUERY))
        assertNull(intents.single().`package`)

        intents.clear()
        launchWebSearch(context) {
            intents += it
            intents.size == 2
        }
        assertEquals(listOf(Intent.ACTION_WEB_SEARCH, Intent.ACTION_VIEW), intents.map { it.action })
        assertEquals("https://www.google.com", intents.last().dataString)
    }

    @Test
    fun uninstallIntentRetainsTheSelectedProfile() {
        val workUser = UserHandle.getUserHandleForUid(1_000_000)
        val intent = uninstallIntent(workCalendar.copy(user = workUser))

        assertEquals(Intent.ACTION_DELETE, intent.action)
        assertEquals("package:calendar", intent.dataString)
        @Suppress("DEPRECATION")
        val actualUser = intent.getParcelableExtra<UserHandle>("android.intent.extra.USER")
        assertEquals(workUser, actualUser)
    }

    @Test
    fun preferencesPersistChangesWithoutMixingProfilesAndMigrateFavorites() {
        val name = "launcher_actions_test_${System.nanoTime()}"
        val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            val apps = listOf(calendar, workCalendar)
            sharedPreferences.edit().putStringSet("favorite_packages", setOf("calendar")).commit()
            val preferences = LauncherPreferences(sharedPreferences)
            assertEquals(setOf(calendar.id, workCalendar.id), preferences.favoriteAppIds(apps))
            val favorites = preferences.toggleFavorite(preferences.favoriteAppIds(apps), workCalendar.id)
            preferences.toggleHidden(emptySet(), workCalendar.id)
            preferences.renameApp(calendar.id, "  Agenda  ")

            val restored = LauncherPreferences(sharedPreferences)
            assertEquals(setOf(calendar.id), restored.favoriteAppIds(apps))
            assertEquals(setOf(workCalendar.id), restored.hiddenAppIds())
            assertEquals(mapOf(calendar.id to "Agenda"), restored.appLabels(apps))
            assertEquals(setOf(calendar.id), favorites)
            restored.renameApp(calendar.id, null)
            assertTrue(restored.appLabels(apps).isEmpty())
            restored.toggleHidden(restored.hiddenAppIds(), workCalendar.id)
            assertFalse(restored.hiddenAppIds().contains(workCalendar.id))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
