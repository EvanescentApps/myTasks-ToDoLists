/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.fragments

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels // For ViewModel delegation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ListsItemTouchCallback
import com.electro.todolist.R
import com.electro.todolist.data.model.SerialListObject
import com.electro.todolist.ui.home.ListsAdapter
import com.electro.todolist.ui.home.TasksViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

// SerialListObject is correctly defined in data package, no need to redefine here
// You might remove the ListObject data class if it's no longer used elsewhere for consistency.
// @Keep and @Serializable are correctly on SerialListObject.

class ChangeListFragment : BottomSheetDialogFragment() {

    // Use viewModels() delegate to get a ViewModel instance scoped to the owning Activity.
    // This assumes TasksViewModel has a default constructor or is provided by a ViewModelFactory.
    private val tasksViewModel: TasksViewModel by viewModels(
        ownerProducer = { requireActivity() } // Scope to the parent Activity
    )

    private lateinit var listsAdapter: ListsAdapter // Renamed for clarity
    private lateinit var recyclerView: RecyclerView // Reference to RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_lists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- BottomSheet Behavior Setup ---
        (dialog as? BottomSheetDialog)?.let { d ->
            val bottomSheetInternal = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let {
                val bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                // Set peek height to full height or wrap content to make it fully expanded initially
                bottomSheetBehavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels // Example: full screen height
                (it.parent as? CoordinatorLayout)?.requestLayout() // Request layout to ensure proper display
            }
        }

        recyclerView = view.findViewById(R.id.all_lists)
        val createListButton: LinearLayout = view.findViewById(R.id.createList)
        val settingsButton: LinearLayout = view.findViewById(R.id.settings)

        // --- RecyclerView and Adapter Setup ---
        // Initialize adapter with empty list and provide callbacks to ViewModel
        listsAdapter = ListsAdapter(
            currentLists = mutableListOf(), // Start with an empty list
            onListSelected = { listId -> // When a list item is clicked
                tasksViewModel.changeList(listId) // Tell ViewModel to change current list
                dismiss() // Dismiss the fragment after selection
            },
            onListMove = { fromPosition, toPosition -> // When a list item is dragged
                tasksViewModel.reorderLists(fromPosition, toPosition) // Tell ViewModel to reorder
            },
            onListDelete = { listId -> // When a list item is swiped/deleted
                // Show a confirmation dialog before actual deletion
                showDeleteConfirmationDialog(listId)
            },
            onListLongPress = { listId ->
                // Optionally handle long press, e.g., show rename/delete options in a context menu
                showListOptionsDialog(listId)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listsAdapter

            val itemTouchHelperCallback = ListsItemTouchCallback(listsAdapter, context)
            val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }

        // --- Observe LiveData from ViewModel ---
        tasksViewModel.allLists.observe(viewLifecycleOwner) { lists: List<SerialListObject>? ->
            if (lists.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Aucune liste créée", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss() // Dismiss if no lists exist
            } else {
                // Update the adapter's data when ViewModel's allLists LiveData changes
                listsAdapter.updateLists(lists)
                Timber.d("ChangeListFragment: allLists observed, adapter updated with ${lists.size} items.")
            }
        }

        // --- Button Listeners ---
        createListButton.setOnClickListener {
            showCreateListDialog()
        }

        settingsButton.setOnClickListener {
            showSettingsComingSoonDialog()
        }
    }

    /**
     * Shows a dialog to confirm list deletion.
     */
    private fun showDeleteConfirmationDialog(listId: String) {
        val listTitle = tasksViewModel.allLists.value?.firstOrNull { it.id == listId }?.title ?: "cette liste"
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle("Supprimer la liste ?")
            .setMessage("Voulez-vous vraiment supprimer '$listTitle' ? Toutes les tâches qu'elle contient seront également supprimées.")
            .setPositiveButton("Supprimer") { dialog, _ ->
                tasksViewModel.deleteList(listId) // Trigger deletion via ViewModel
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                // If cancelled, notify adapter to refresh the item to restore its original position
                listsAdapter.notifyDataSetChanged() // Or notifyItemChanged(position) if you store it
                dialog.cancel()
            }
            .show()
    }

    /**
     * Shows a dialog with options for a long-pressed list (e.g., rename, delete).
     */
    private fun showListOptionsDialog(listId: String) {
        val listTitle = tasksViewModel.allLists.value?.firstOrNull { it.id == listId }?.title ?: ""

        val options = arrayOf("Renommer la liste", "Supprimer la liste")

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle(listTitle)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showRenameListDialog(listId, listTitle) // Rename option
                    1 -> showDeleteConfirmationDialog(listId)    // Delete option
                }
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows a dialog to rename an existing list.
     */
    private fun showRenameListDialog(listId: String, currentTitle: String) {
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, view as ViewGroup?, false)
        val input = viewInflated.findViewById<EditText>(R.id.input)
        input.setText(currentTitle) // Pre-fill with current title

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle("Renommer la liste")
            .setView(viewInflated)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank() && newName != currentTitle) {
                    tasksViewModel.renameList(listId, newName) // Call ViewModel to rename
                } else if (newName.isBlank()) {
                    Toast.makeText(requireContext(), "Le nom de la liste ne peut pas être vide", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }


    /**
     * Shows a dialog for creating a new list.
     */
    private fun showCreateListDialog() {
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, view as ViewGroup?, false)
        val input = viewInflated.findViewById<EditText>(R.id.input)

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle("Créer une nouvelle liste")
            .setView(viewInflated)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val newListTitle = input.text.toString().trim()
                if (newListTitle.isNotBlank()) {
                    tasksViewModel.createNewList(newListTitle) // Call ViewModel to create list
                    // ViewModel will update allLists LiveData, which will update the adapter
                    dismiss() // Dismiss the dialog after creation
                } else {
                    Toast.makeText(requireContext(), "Le nom de la liste ne peut pas être vide", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    /**
     * Shows a "feature coming soon" dialog for settings.
     */
    private fun showSettingsComingSoonDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle("Fonctionnalité en cours de développement")
            .setMessage("Les paramètres ne sont pas disponibles pour l'instant, ce sera pour une prochaine mise à jour.")
            .setPositiveButton("D'accord") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // No longer need inner ViewHolder class, as ListsAdapter already has its own ViewHolder.
    // The commented-out ItemAdapter is also no longer needed.

    companion object {
        const val TAG = "ChangeListFragment"

        @JvmStatic
        fun newInstance() = ChangeListFragment() // No arguments needed, ViewModel provides data
    }
}