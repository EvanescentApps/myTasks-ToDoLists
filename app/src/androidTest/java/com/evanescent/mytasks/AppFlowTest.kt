/*
 * Copyright (c) 2026. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evanescent.mytasks.ui.FlowActivity
import com.evanescent.mytasks.ui.home.TasksActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @Test
    fun testTasksActivityLaunch() {
        ActivityScenario.launch(TasksActivity::class.java).use {
            // Check if FAB is displayed - this confirms the main UI is loaded
            onView(withId(R.id.fab)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testFlowActivityLaunch() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FlowActivity::class.java).apply {
            putExtra("taskDuration", 10) // 10 seconds
            putExtra("taskTitle", "Test Task")
        }
        ActivityScenario.launch<FlowActivity>(intent).use {
            // Verify that the fixed layout issues don't cause a crash and views are visible
            onView(withId(R.id.taskTitle)).check(matches(withText("Test Task")))
            onView(withId(R.id.countdownTextView)).check(matches(isDisplayed()))
        }
    }
}

