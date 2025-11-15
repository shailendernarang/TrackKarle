package com.example.wealthtracker.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RatingPromptManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        RatingPromptManager.resetRatingData(context)
    }

    @After
    fun tearDown() {
        RatingPromptManager.resetRatingData(context)
    }

    @Test
    fun shouldNotShow_ifAlreadyRated() {
        RatingPromptManager.markUserRated(context)
        val result = RatingPromptManager.shouldShowPrompt(context) { 0.0 }
        assertFalse(result)
    }

    @Test
    fun shouldNotShow_ifShownInThisSession() {
        // Prepare state
        // Enough opens
        repeat(5) { RatingPromptManager.incrementAppOpenCount(context) }
        // Start a new session and mark shown
        RatingPromptManager.startNewSession(context)
        RatingPromptManager.markPromptShown(context)

        val result = RatingPromptManager.shouldShowPrompt(context) { 0.0 }
        assertFalse(result)
    }

    @Test
    fun shouldNotShow_ifOpenCountLessThanFive() {
        // Fewer than 5 opens
        repeat(3) { RatingPromptManager.incrementAppOpenCount(context) }
        RatingPromptManager.startNewSession(context)

        val result = RatingPromptManager.shouldShowPrompt(context) { 0.0 }
        assertFalse(result)
    }

    @Test
    fun shouldShow_whenEligible_andRandomPasses() {
        // Eligible conditions
        repeat(6) { RatingPromptManager.incrementAppOpenCount(context) }
        RatingPromptManager.startNewSession(context)

        val result = RatingPromptManager.shouldShowPrompt(context) { 0.1 } // < 0.3 passes
        assertTrue(result)
    }

    @Test
    fun shouldNotShow_whenEligible_butRandomFails() {
        // Eligible conditions
        repeat(6) { RatingPromptManager.incrementAppOpenCount(context) }
        RatingPromptManager.startNewSession(context)

        val result = RatingPromptManager.shouldShowPrompt(context) { 0.9 } // >= 0.3 fails
        assertFalse(result)
    }
}
