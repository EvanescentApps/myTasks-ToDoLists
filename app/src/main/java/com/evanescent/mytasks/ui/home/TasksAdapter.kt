/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.ui.home

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evanescent.mytasks.R
import com.evanescent.mytasks.data.model.Priority
import com.evanescent.mytasks.data.model.Task
import com.evanescent.mytasks.data.timestampToDate
import com.evanescent.mytasks.data.timestampToDuration
import com.google.android.material.chip.Chip
import at.connyduck.sparkbutton.SparkButton
import timber.log.Timber

class TasksAdapter(
    private val context: Context,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskSwipedToDelete: (Task, Int) -> Unit,
    private val onTaskSwipedToDone: (Task) -> Unit,
    private val onListOrderChanged: ((List<Task>) -> Unit)? = null,
    private val onTaskClicked: (Task, Int) -> Unit,
    private val onEmptyStateChanged: (Boolean) -> Unit
) : ListAdapter<Task, TasksAdapter.ViewHolder>(TaskDiffCallback()) {

    private var dragList: MutableList<Task>? = null
    private var isDragging = false

    /**
     * Updates the adapter's internal list with new data from the ViewModel.
     * Call this from TasksActivity's tasksViewModel.tasks.observe() block.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        // submitList calcule les différences en arrière-plan
        submitList(newTasks) {
            // Callback optionnel une fois la liste mise à jour
            onEmptyStateChanged(newTasks.isEmpty())
        }
    }

    /**
     * Retrieves a Task at a given position. Used by ItemTouchHelperCallback.
     */
    fun getTaskAt(position: Int): Task? {
        return if (position >= 0 && position < itemCount) getItem(position) else null
    }

    /**
     * Temporarily removes a Task from the adapter for visual feedback during swipe-to-delete.
     * The actual data deletion is handled by the ViewModel after Snackbar dismissal.
     * @return The removed task, so it can be passed to the ViewModel for potential undo.
     */
    fun removeTaskAt(position: Int): Task? {
        if (position >= 0 && position < itemCount) {
            val item = getItem(position)
            val currentListMutable = currentList.toMutableList()
            currentListMutable.removeAt(position)

            submitList(currentListMutable)
            onEmptyStateChanged(currentListMutable.isEmpty())
            return item
        }
        return null
    }


    /**
     * Handles the visual movement of items during drag-and-drop.
     * This method is called repeatedly while the user is dragging.
     * The actual data reordering is saved by the ViewModel after drop completion.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Initialize the temporary list on the first move
        if (dragList == null) {
            dragList = currentList.toMutableList()
            isDragging = true
        }

        // Swap items in the temporary list
        // Note: We use dragList!! because we just initialized it above
        if (fromPosition < dragList!!.size && toPosition < dragList!!.size) {
            java.util.Collections.swap(dragList!!, fromPosition, toPosition)
            // Visually notify the adapter that an item moved (without refreshing the whole list)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    /**
     * Called when the drag-and-drop operation is completed (user lifts finger).
     * This is where the ViewModel should be informed to persist the new order.
     */
    fun onDropCompleted() { // Plus de paramètres from/to
        dragList?.let { newList ->
            // 1. On fige l'état visuel avec submitList pour être propre
            submitList(newList.toList())

            // 2. On prévient l'activité qu'il faut sauvegarder TOUTE la liste
            // Vous devez ajouter ce callback au constructeur, voir étape 4
            onListOrderChanged?.invoke(newList.toList())
        }
        // Reset la liste temporaire
        dragList = null
        isDragging = false

        Timber.d("TasksAdapter: Drag-and-drop completed. List saved.")
    }

    // Surcharge submitList pour bloquer les mises à jour pendant le drag
    override fun submitList(list: List<Task>?, commitCallback: Runnable?) {
        if (isDragging) {
            // Si on est en train de drag, on IGNORE les mises à jour venant du ViewModel
            // pour éviter que la liste saute sous le doigt
            return
        }
        super.submitList(list, commitCallback)
    }

    // Surcharge aussi la version sans callback pour être sûr
    override fun submitList(list: List<Task>?) {
        if (isDragging) return
        super.submitList(list)
    }

    /**
     * Returns the current list of tasks held by the adapter in their current visual order.
     * Used by ItemTouchHelperCallback to get the final order after a drag-and-drop.
     */
    fun getTasks(): List<Task> = currentList


    /**
     * Handles swipe actions and delegates them to the appropriate callbacks.
     */
    fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) {
            Timber.e("onSwipe: Invalid position received.")
            return
        }
        val swipedTask = getItem(position)

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
        val task: Task = getItem(position)

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
                Timber.tag("task").d("Checkbox clicked for item ${task.id}, new state: $newDoneState")
                onTaskChecked(task, newDoneState)
            }, 600)
        }

        viewHolder.itemParent.setOnClickListener {
            onTaskClicked(task, position)
        }
    }

    // Dans TasksAdapter
    public override fun getItem(position: Int): Task {
        return super.getItem(position)
    }

}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        // On compare les IDs uniques (changez 'uid' par votre vraie clé primaire si différent, ex: 'id')
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        // On compare tout le contenu (data class fait ça très bien avec equals())
        return oldItem == newItem
    }
}

