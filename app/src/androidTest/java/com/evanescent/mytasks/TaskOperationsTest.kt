/*
 * Copyright (c) 2026. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evanescent.mytasks.ui.home.TasksActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskOperationsTest {

    private var idlingResource: IdlingResource? = null

    @Before
    fun registerIdlingResource() {
        // Espresso will wait for the RecyclerView animations/operations to finish
    }

    @After
    fun unregisterIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun testAddReorderAddSwitch() {
        val scenario = ActivityScenario.launch(TasksActivity::class.java)

        try {
            // ================================================================
            // STEP 1: Cleanup — dismiss any leftover dialog from previous runs
            // ================================================================
            try {
                onView(withText("Modifier une tâche")).check(matches(isDisplayed()))
                pressBack()
            } catch (_: Exception) {
                // No dialog open — good
            }

            // ================================================================
            // STEP 2: Add 3 tasks and verify initial order
            // (newest added first → expected: C, B, A)
            // ================================================================
            addTask("Task A")
            addTask("Task B")
            addTask("Task C")

            // Assert each position independently
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(0, hasDescendant(withText("Task C")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(1, hasDescendant(withText("Task B")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(2, hasDescendant(withText("Task A")))))

            // ================================================================
            // STEP 3: Drag "Task C" from position 0 to position 2
            // Expected order after drag: B(0), A(1), C(2)
            // ================================================================
            onView(withId(R.id.tasksRecyclerview))
                .perform(dragAndDrop(0, 2))

            // Wait for DB persistence + adapter refresh
            onView(isRoot()).perform(waitFor(2500))

            // Assert each position independently
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(0, hasDescendant(withText("Task B")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(1, hasDescendant(withText("Task A")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(2, hasDescendant(withText("Task C")))))

            // ================================================================
            // STEP 4: Add "Task D" → should appear at position 0 (top)
            // Expected order: D(0), B(1), A(2), C(3)
            // ================================================================
            addTask("Task D")
            onView(isRoot()).perform(waitFor(1000))

            // Assert each position independently
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(0, hasDescendant(withText("Task D")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(1, hasDescendant(withText("Task B")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(2, hasDescendant(withText("Task A")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(3, hasDescendant(withText("Task C")))))

            // ================================================================
            // STEP 5: Leave the list and come back (simulate navigation)
            // The order must survive an activity recreation.
            // ================================================================
            scenario.recreate()
            onView(isRoot()).perform(waitFor(2000))

            // Assert order still preserved after return
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(0, hasDescendant(withText("Task D")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(1, hasDescendant(withText("Task B")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(2, hasDescendant(withText("Task A")))))
            onView(withId(R.id.tasksRecyclerview))
                .check(matches(atPosition(3, hasDescendant(withText("Task C")))))

        } finally {
            scenario.close()
        }
    }

    // ============================================================================
    // HELPERS
    // ============================================================================

    private fun addTask(title: String) {
        onView(withId(R.id.fab)).perform(click())
        // Wait for the AddTaskFragment dialog slide-up animation to fully complete.
        // The device has animations enabled, so the EditText's globalVisibleRect
        // is empty during the animation, failing the "is displayed" constraint.
        onView(isRoot()).perform(waitFor(1500))
        // Re-check that the EditText is now fully visible before typing
        onView(withId(R.id.title_edit_text))
            .check(matches(isDisplayed()))
        onView(withId(R.id.title_edit_text))
            .perform(typeText(title), closeSoftKeyboard())
        onView(withId(R.id.save_task)).perform(click())
        // Wait for Room insert + Flow emission + adapter refresh
        onView(isRoot()).perform(waitFor(2500))
    }

    /**
     * Simulates drag-and-drop in a RecyclerView using direct MotionEvent injection.
     *
     * ItemTouchHelper requires a specific sequence:
     * 1. ACTION_DOWN at the source item center
     * 2. ~800ms hold for long-press → ItemTouchHelper enters ACTION_STATE_DRAG
     * 3. Incremental ACTION_MOVE events (interpolated) → onMove() fires repeatedly
     * 4. ACTION_UP at target position → clearView() → onDropCompleted()
     */
    private fun dragAndDrop(fromPosition: Int, toPosition: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "dragAndDrop from $fromPosition to $toPosition"

            override fun perform(uiController: UiController, view: View) {
                val recyclerView = view as RecyclerView
                uiController.loopMainThreadUntilIdle()

                val fromVH = recyclerView.findViewHolderForAdapterPosition(fromPosition)
                val toVH = recyclerView.findViewHolderForAdapterPosition(toPosition)
                checkNotNull(fromVH) {
                    "Source ViewHolder not found at position $fromPosition. Is the item visible?"
                }
                checkNotNull(toVH) {
                    "Target ViewHolder not found at position $toPosition. Is the item visible?"
                }

                val fromItem = fromVH.itemView
                val toItem = toVH.itemView

                val fromLoc = IntArray(2)
                fromItem.getLocationOnScreen(fromLoc)
                val toLoc = IntArray(2)
                toItem.getLocationOnScreen(toLoc)

                val startX = fromLoc[0] + fromItem.width / 2f
                val startY = fromLoc[1] + fromItem.height / 2f
                val endX = toLoc[0] + toItem.width / 2f
                val endY = toLoc[1] + toItem.height / 2f

                val downTime = android.os.SystemClock.uptimeMillis()
                var eventTime = downTime

                // 1. ACTION_DOWN — initiates gesture detection in ItemTouchHelper
                var motionEvent = MotionEvent.obtain(
                    downTime, eventTime, MotionEvent.ACTION_DOWN, startX, startY, 0
                )
                uiController.injectMotionEvent(motionEvent)
                motionEvent.recycle()

                // 2. Hold for long-press threshold (~800ms)
                uiController.loopMainThreadForAtLeast(800)

                // 3. Incremental ACTION_MOVE events from start → end (20 steps at 25ms each)
                val steps = 20
                for (i in 1..steps) {
                    eventTime = android.os.SystemClock.uptimeMillis()
                    val fraction = i.toFloat() / steps
                    val moveX = startX + (endX - startX) * fraction
                    val moveY = startY + (endY - startY) * fraction

                    motionEvent = MotionEvent.obtain(
                        downTime, eventTime, MotionEvent.ACTION_MOVE, moveX, moveY, 0
                    )
                    uiController.injectMotionEvent(motionEvent)
                    motionEvent.recycle()

                    uiController.loopMainThreadForAtLeast(25)
                }

                // 4. ACTION_UP — triggers clearView() which calls onDropCompleted()
                eventTime = android.os.SystemClock.uptimeMillis()
                motionEvent = MotionEvent.obtain(
                    downTime, eventTime, MotionEvent.ACTION_UP, endX, endY, 0
                )
                uiController.injectMotionEvent(motionEvent)
                motionEvent.recycle()

                // Let the adapter chain complete: onDropCompleted → ViewModel → DB → LiveData → adapter refresh
                uiController.loopMainThreadUntilIdle()
                uiController.loopMainThreadForAtLeast(1500)
            }
        }
    }

    /** Custom matcher: checks that a RecyclerView has a specific item at a given position. */
    private fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }
            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    /**
     * Custom ViewAction that pauses on the UI thread.
     * Safer than Thread.sleep because it doesn't block the instrumentation thread.
     */
    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "wait for $millis ms"

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
}
