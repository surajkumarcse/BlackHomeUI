package com.lostfalcon.myminimalistdesignhomepage

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsKeyboardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun swipingToAppsShowsKeyboardAndReturningHomeHidesIt() {
        waitForKeyboard(visible = false)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        waitForKeyboard(visible = true)

        composeRule.onRoot().performTouchInput { swipeRight() }
        waitForKeyboard(visible = false)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        waitForKeyboard(visible = true)
    }

    private fun waitForKeyboard(visible: Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == visible
        }
    }
}
