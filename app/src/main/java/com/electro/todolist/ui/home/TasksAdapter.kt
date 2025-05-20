/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.electro.todolist.data.model.Priority
import com.electro.todolist.data.model.Task
import com.electro.todolist.data.timestampToDate
import com.electro.todolist.data.timestampToDuration
import com.google.android.material.chip.Chip
import com.varunest.sparkbutton.SparkButton
import timber.log.Timber

class TasksAdapter(
    private var currentTasks: MutableList<Task>,
    private val context: Context,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskSwipedToDelete: (Task, Int) -> Unit,
    private val onTaskSwipedToDone: (Task) -> Unit,
    private val onTaskMoved: (Int, Int) -> Unit, // This callback triggers ViewModel update
    private val onTaskClicked: (Task, Int) -> Unit,
    private val onEmptyStateChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    /**
     * Updates the adapter's internal list with new data from the ViewModel.
     * Call this from TasksActivity's tasksViewModel.tasks.observe() block.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        currentTasks.clear()
        currentTasks.addAll(newTasks)
        notifyDataSetChanged()
        onEmptyStateChanged(newTasks.isEmpty())
    }

    /**
     * Retrieves a Task at a given position. Used by ItemTouchHelperCallback.
     */
    fun getTaskAt(position: Int): Task? {
        return if (position >= 0 && position < currentTasks.size) currentTasks[position] else null
    }

    /**
     * Temporarily removes a Task from the adapter for visual feedback during swipe-to-delete.
     * The actual data deletion is handled by the ViewModel after Snackbar dismissal.
     * @return The removed task, so it can be passed to the ViewModel for potential undo.
     */
    fun removeTaskAt(position: Int): Task? {
        if (position >= 0 && position < currentTasks.size) {
            val removedTask = currentTasks.removeAt(position)
            notifyItemRemoved(position)
            onEmptyStateChanged(currentTasks.isEmpty())
            return removedTask
        }
        return null
    }

    /**
     * Handles the visual movement of items during drag-and-drop.
     * This method is called repeatedly while the user is dragging.
     * The actual data reordering is saved by the ViewModel after drop completion.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || fromPosition >= currentTasks.size ||
            toPosition < 0 || toPosition >= currentTasks.size) {
            Timber.e("onItemMove: Invalid positions. From: $fromPosition, To: $toPosition, List size: ${currentTasks.size}")
            return
        }
        // Perform the swap in the adapter's internal list for visual feedback
        val movedItem = currentTasks.removeAt(fromPosition)
        currentTasks.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
        Timber.d("Adapter visually moved item from $fromPosition to $toPosition")
        // IMPORTANT: onTaskMoved is NOT called here. It's called after the drag finishes.
    }

    /**
     * Called when the drag-and-drop operation is completed (user lifts finger).
     * This is where the ViewModel should be informed to persist the new order.
     */
    fun onDropCompleted(fromPosition: Int, toPosition: Int) {
        onTaskMoved(fromPosition, toPosition) // Trigger callback to ViewModel for persistence
        Timber.d("TasksAdapter: Drag-and-drop completed. Notifying ViewModel about move from $fromPosition to $toPosition")
    }

    /**
     * Returns the current list of tasks held by the adapter in their current visual order.
     * Used by ItemTouchHelperCallback to get the final order after a drag-and-drop.
     */
    fun getTasks(): List<Task> = currentTasks

    /**
     * Handles swipe actions and delegates them to the appropriate callbacks.
     */
    fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) {
            Timber.e("onSwipe: Invalid position received.")
            return
        }
        val swipedTask = currentTasks[position]

        when (direction) {
            ItemTouchHelper.LEFT -> { // Swipe to Delete
                val removedTask = removeTaskAt(position) // This handles notifyItemRemoved()
                if (removedTask != null) {
                    onTaskSwipedToDelete(removedTask, position) // Trigger callback to ViewModel
                }
            }
            ItemTouchHelper.RIGHT -> { // Swipe to Mark Done
                // Mark task as done. This should be handled by the ViewModel.
                onTaskSwipedToDone(swipedTask) // Trigger callback to ViewModel
            }
            else -> {
                Timber.w("Unhandled swipe direction: $direction")
            }
        }
    }

    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        val newCheckBox: SparkButton = itemView.findViewById(R.id.new_checkbox)
        val dateChip: Chip = itemView.findViewById(R.id.date_chip)
        val priorityChip: Chip = itemView.findViewById(R.id.priority_chip)
        val durationChip: Chip = itemView.findViewById(R.id.duration_chip)
        val itemParent: LinearLayout = itemView.findViewById(R.id.itemParent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val taskView = inflater.inflate(R.layout.item_task, parent, false)
        return ViewHolder(taskView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val task: Task = currentTasks[position]

        viewHolder.titleTextView.text = task.title
        if (task.description.isNullOrBlank()) {
            viewHolder.descriptionTextView.visibility = View.GONE
        } else {
            viewHolder.descriptionTextView.text = task.description
            viewHolder.descriptionTextView.visibility = View.VISIBLE
        }

        viewHolder.newCheckBox.isChecked = task.done

        if (task.done) {
            viewHolder.titleTextView.paintFlags = viewHolder.titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            listOf(viewHolder.titleTextView, viewHolder.descriptionTextView,
                viewHolder.dateChip, viewHolder.priorityChip, viewHolder.durationChip,
                viewHolder.newCheckBox).forEach { it.alpha = 0.65f }
        } else {
            viewHolder.titleTextView.paintFlags = viewHolder.titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            listOf(viewHolder.titleTextView, viewHolder.descriptionTextView,
                viewHolder.dateChip, viewHolder.priorityChip, viewHolder.durationChip,
                viewHolder.newCheckBox).forEach { it.alpha = 1.0f }
        }

        val timestamp = task.date
        if (timestamp != null && timestamp != 0L) {
            val dateText = timestampToDate(timestamp)
            viewHolder.dateChip.text = dateText
            viewHolder.dateChip.visibility = if (dateText.isNotBlank()) View.VISIBLE else View.GONE
        } else {
            viewHolder.dateChip.visibility = View.GONE
        }

        if (task.priority != Priority.NONE) {
            viewHolder.priorityChip.apply {
                visibility = View.VISIBLE
                text = task.priority.first
                setTextColor(ContextCompat.getColor(context, task.priority.second))
            }
        } else {
            viewHolder.priorityChip.visibility = View.GONE
        }

        val durationTimestamp = task.duration
        if (durationTimestamp != null && durationTimestamp != 0L) {
            val durationText = timestampToDuration(durationTimestamp)
            viewHolder.durationChip.text = durationText
            viewHolder.durationChip.visibility = if (durationText.isNotBlank()) View.VISIBLE else View.GONE
        } else {
            viewHolder.durationChip.visibility = View.GONE
        }

        viewHolder.newCheckBox.setOnClickListener {
            if (!viewHolder.newCheckBox.isChecked) {
                viewHolder.newCheckBox.playAnimation()
            }

            val newDoneState = !viewHolder.newCheckBox.isChecked
            viewHolder.newCheckBox.isChecked = newDoneState

            Handler(Looper.getMainLooper()).postDelayed({
                Timber.tag("task").d("Checkbox clicked for item ${task.uid}, new state: $newDoneState")
                onTaskChecked(task, newDoneState)
            }, 600)
        }

        viewHolder.itemParent.setOnClickListener {
            onTaskClicked(task, position)
        }
    }

    override fun getItemCount(): Int = currentTasks.size
}