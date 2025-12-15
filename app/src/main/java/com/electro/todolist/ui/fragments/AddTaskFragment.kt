/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.electro.todolist.R
import com.electro.todolist.data.model.Task // Ensure Task data class is correctly imported
import com.electro.todolist.ui.home.TasksViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber // Using Timber for logging

/**
 * A BottomSheetDialogFragment for adding new tasks.
 * It communicates with TasksViewModel to save new tasks.
 */
class AddTaskFragment : BottomSheetDialogFragment() {

    // Use viewModels() delegate to get a ViewModel instance scoped to the owning Activity
    // This assumes TasksViewModel has a default constructor or is provided by a ViewModelFactory
    // if it has dependencies (like TasksRepository).
    // In your case, since TasksViewModel takes TasksRepository, you'd likely use an Activity-scoped
    // ViewModel provider or a Hilt/Koin injection setup. For simplicity, assuming default constructor for now.
    // If using a ViewModelFactory, it would be:
    // private val tasksViewModel: TasksViewModel by activityViewModels { YourViewModelFactory(requireContext()) }
    private val tasksViewModel: TasksViewModel by viewModels(
        ownerProducer = { requireActivity() } // Scope to the parent Activity
    )

    private lateinit var taskTitleEditText: EditText
    private lateinit var descriptionEditText: EditText // Renamed for clarity

    // No longer need param1/param2 as they were unused.
    // currentList will be retrieved from ViewModel if needed.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the style for the BottomSheet.
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_task, container, false)

        taskTitleEditText = view.findViewById(R.id.title_edit_text)
        descriptionEditText = view.findViewById(R.id.description)

        val saveTaskButton = view.findViewById<Button>(R.id.save_task)
        val addDescriptionButton = view.findViewById<ImageButton>(R.id.showDescription)

        // Clear focus and request focus on title for immediate typing
        taskTitleEditText.text.clear()
        taskTitleEditText.requestFocus()

        descriptionEditText.text.clear() // Clear description text as well

        addDescriptionButton.setOnClickListener {
            descriptionEditText.visibility = View.VISIBLE
            descriptionEditText.requestFocus()
            // Optionally, show keyboard for description if it wasn't already
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(descriptionEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        saveTaskButton.setOnClickListener {
            saveTask()
            dismiss() // Dismiss the dialog after saving
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust BottomSheet behavior for expansion
        (dialog as? BottomSheetDialog)?.let { d ->
            val bottomSheetInternal = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let {
                val bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetBehavior.peekHeight = it.height // Set peek height to full height when expanded
                (it.parent as? CoordinatorLayout)?.requestLayout() // Request layout to ensure proper display
            }
        }
    }

    /**
     * Handles the task saving logic.
     * It extracts data from EditTexts and calls the ViewModel to add the task.
     */
    private fun saveTask() {
        val title = taskTitleEditText.text.toString().trim() // Trim whitespace
        val description = descriptionEditText.text.toString().trim() // Trim whitespace

        // Hide keyboard before processing
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)

        if (title.isBlank()) {
            // Optionally show a short toast or error if title is empty
            // Toast.makeText(requireContext(), "Task title cannot be empty", Toast.LENGTH_SHORT).show()
            Timber.d("Task title is blank, not saving.")
            return // Don't save if title is empty
        }

        // Generate ID and create Task object
        val taskId = Task.generateId(5) // Ensure this generates a sufficiently unique ID
        val creationTimeId = System.currentTimeMillis() // Use a timestamp for sorting/unique key in SharedPreferences
        val newTask = Task(
            title = title,
            description = description,
            creationDate = creationTimeId,
            done = false,
            uid = taskId,
            position = 0 // Position will be handled by ViewModel/Repository on save
        )

        // Call ViewModel to add the task
        tasksViewModel.addTask(newTask)

        Timber.d("New task created and sent to ViewModel: ${newTask.title}")
        // UI fields are cleared when fragment dismisses implicitly
    }

    // No need to override onDismiss to call saveTask(), as saveTask() is now triggered by the button click.
    // onDismiss is useful for cleanup or logging, not primary actions.
    override fun onDismiss(dialog: DialogInterface) {
        Timber.d("AddTaskFragment dismissed.")
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "AddTaskFragment" // A tag for showing the fragment

        @JvmStatic
        fun newInstance() = AddTaskFragment() // No arguments needed now, ViewModel handles current list context
    }
}