package com.stoicjournal

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule(HiltTestActivity::class.java)

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        com.stoicjournal.e2e.E2ESupport.ensureScreenOn()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            StoicJournalNavHost(startDestination = Screen.Today, navController = navController)
        }
    }

    @Test
    fun todayScreen_isDisplayed() {
        composeTestRule.onNodeWithText("Stoic Journal").assertExists()
    }
}
