/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.electro.todolist.data.model.SerialListObject
import timber.log.Timber

// Assuming SerialListObject is defined like this (or similar, from data package):
//

class ListsAdapter(
    // The list of SerialListObject is now managed internally and updated via updateLists()
    private var currentLists: MutableList<SerialListObject>,
    // Callbacks for actions. The Activity/Fragment will provide these lambdas.
    private val onListSelected: (String) -> Unit, // Callback when a list is clicked (pass list ID)
    private val onListMove: (Int, Int) -> Unit, // Callback when a list is moved (from, to)
    private val onListDelete: (String) -> Unit, // Callback when a list is to be deleted (pass list ID)
    private val onListLongPress: (String) -> Unit // Optional: for long press actions
) : RecyclerView.Adapter<ListsAdapter.ViewHolderList>() {

    // Removed: private var currentListSelected by Delegates.notNull<Int>()
    // The 'selected' state should ideally come from the data (SerialListObject.isCurrentSelected)
    // or be managed by the ViewModel.

    private val logEnabled = false // Keep for debugging if needed

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderList {
        val inflater = LayoutInflater.from(parent.context)
        val listItemView = inflater.inflate(R.layout.list_item, parent, false)
        return ViewHolderList(listItemView)
    }

    override fun onBindViewHolder(holder: ViewHolderList, position: Int) {
        val listObject = currentLists[position]

        holder.titleText.text = listObject.title

        // Apply visual selection state based on the data
        if (listObject.isCurrentSelected) {
            holder.linearLayout.setBackgroundResource(R.drawable.list_item_bg_selected)
            // Removed: currentListSelected = holder.bindingAdapterPosition (no longer needed here)
        } else {
            holder.linearLayout.setBackgroundResource(R.drawable.list_item_bg) // Assuming you have a normal background
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            // Invoke the callback, passing the ID of the selected list
            onListSelected(listObject.id)
            // The caller (e.g., ChangeListFragment) will handle dismissing itself
            // No direct dismiss here.
        }

        // Optional: Long press listener
        holder.itemView.setOnLongClickListener {
            onListLongPress(listObject.id)
            true // Consume the long click
        }
    }

    override fun getItemCount(): Int = currentLists.size

    /**
     * Updates the adapter's internal list with new data from the ViewModel.
     * Call this from the Activity/Fragment's LiveData observer.
     */
    fun updateLists(newLists: List<SerialListObject>) {
        // For better performance, consider using DiffUtil. For simplicity, we'll clear and add.
        currentLists.clear()
        currentLists.addAll(newLists)
        notifyDataSetChanged() // Re-draws the entire list
        Timber.d("ListsAdapter updated with ${newLists.size} items.")
    }

    /**
     * Handles the visual reordering of items during drag-and-drop.
     * This is called by ItemTouchHelperCallback.
     *
     * @param fromPosition The starting position of the dragged item.
     * @param toPosition The target position of the dragged item.
     */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || fromPosition >= currentLists.size ||
            toPosition < 0 || toPosition >= currentLists.size) {
            Timber.e("moveItem: Invalid positions. From: $fromPosition, To: $toPosition, List size: ${currentLists.size}")
            return // Avoid IndexOutOfBoundsException
        }

        val movedItem = currentLists.removeAt(fromPosition)
        currentLists.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)

        // IMPORTANT: Inform the ViewModel about the move for persistence.
        // This callback will be handled by the ViewModel.
        onListMove(fromPosition, toPosition)
        Timber.d("ListsAdapter visually moved item from $fromPosition to $toPosition")
    }

    /**
     * Handles the deletion of an item visually.
     * This is typically called after a swipe action from ItemTouchHelperCallback.
     * The actual data deletion is handled by the ViewModel.
     *
     * @param position The position of the item to remove.
     */
    fun deleteItemOnAdapter(position: Int) {
        if (position >= 0 && position < currentLists.size) {
            val deletedListId = currentLists[position].id // Get the ID before removing
            currentLists.removeAt(position)
            notifyItemRemoved(position)
            // Inform the ViewModel about the deletion
            onListDelete(deletedListId)
            Timber.d("ListsAdapter visually removed item at $position (ID: $deletedListId)")
        }
    }

    // The onSwipe method (if you intend to implement swipe for lists)
    // This will be called by ListsItemTouchCallback.onSwiped
    fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) {
            Timber.e("onSwipe: Invalid position received.")
            return
        }
        val swipedList = currentLists[position]

        if (direction == ItemTouchHelper.LEFT) { // Example: Swipe left to delete
            Timber.i("Swipe LEFT to delete list: ${swipedList.title}")
            // Temporarily remove for visual feedback, then inform ViewModel
            deleteItemOnAdapter(position) // This will trigger onListDelete callback
        } else if (direction == ItemTouchHelper.RIGHT) { // Example: Swipe right for some other action
            Timber.i("Swipe RIGHT for list: ${swipedList.title}")
            // Implement other swipe action logic here, e.g., onListArchive(swipedList.id)
            // If the item should remain, you might need to call notifyItemChanged(position)
            // to reset its position after the swipe animation.
        }
    }


    inner class ViewHolderList(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val linearLayout: LinearLayout = itemView.findViewById(R.id.bottomSheetParent) // Assuming this is the parent layout
        val titleText: TextView = itemView.findViewById(R.id.text)
    }
}

