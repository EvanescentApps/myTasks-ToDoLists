/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.evanescent.mytasks.ItemTouchHelperCallback
import com.evanescent.mytasks.R
import com.evanescent.mytasks.data.model.Task
import com.evanescent.mytasks.data.repository.TasksRepository
import com.evanescent.mytasks.databinding.ActivityTasksBinding
import com.evanescent.mytasks.ui.details.TaskDetailsActivity
import com.evanescent.mytasks.ui.fragments.AddTaskFragment
import com.evanescent.mytasks.ui.fragments.BottomFragment
import com.evanescent.mytasks.ui.fragments.BottomFragmentActions
import com.evanescent.mytasks.ui.fragments.ChangeListFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

// DataStore for settings (consider if this should be part of a SettingsViewModel/Repository)
/*
val Context.dataStoreSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")
*/

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TasksActivity : AppCompatActivity(), BottomFragmentActions { // Implement the interface

    private lateinit var adapter: TasksAdapter
    private lateinit var b: ActivityTasksBinding
    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var itemTouchHelper: ItemTouchHelper

    val tasksViewModel: TasksViewModel by viewModels()

    // New Activity Result Launcher for Export
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, getString(R.string.file_saved_success), Toast.LENGTH_LONG).show()
            // Persist URI permissions if you want to access it later without user prompt
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            tasksViewModel.exportTaskListToFile(uri)
        } else {
            Toast.makeText(this, getString(R.string.export_cancelled), Toast.LENGTH_SHORT).show()
            Timber.tag("Tasks to file").i("File creation cancelled by user.")
        }
    }

    // New Activity Result Launcher for Import
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, getString(R.string.file_imported_success), Toast.LENGTH_LONG).show()
            tasksViewModel.importTaskListFromFile(uri)
        } else {
            Toast.makeText(this, getString(R.string.import_cancelled), Toast.LENGTH_SHORT).show()
            Timber.tag("Tasks from file").i("File opening cancelled by user.")
        }
    }


    private val taskDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!

            b.bottomAppBar.performShow()

            Timber.tag("Activity Result").e("Received result for Task Details Activity")

            val position = data.getIntExtra("position", -1)
            val taskJson = data.getStringExtra("returnTask")
            val deleteBool = data.getBooleanExtra("delete", false)

            if (position == -1 || taskJson.isNullOrBlank()) {
                Timber.tag("DATA").e("Position or TaskJson is null on result Task Details Activity")
                return@registerForActivityResult
            }

            val originalTasks = tasksViewModel.tasks.value // Get current tasks from LiveData
            if (originalTasks == null || position >= originalTasks.size || position < 0) {
                Timber.tag("DATA").e("Invalid position $position or tasks list empty/too small for request, from Task Details Activity")
                return@registerForActivityResult
            }

            val originalTaskInList = originalTasks[position] // Get the original task based on position

            if (deleteBool) {
                tasksViewModel.deleteTask(originalTaskInList) // Tell ViewModel to delete the task
            } else { // Update task
                val updatedTask = Json.decodeFromString<Task>(taskJson) // Parse the updated task

                tasksViewModel.updateTask(originalTaskInList, updatedTask) // ViewModel method to handle updates

                // If task was marked done, show specific Snackbar (UI-specific)
                if (updatedTask.done) {
                    Snackbar.make(findViewById(R.id.activity), getString(R.string.task_completed), Snackbar.LENGTH_LONG)
                        .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
                        .setAction(getString(R.string.undo)) {
                            tasksViewModel.setTaskDone(updatedTask, false) // Undo done state
                        }.show()
                }
            }
        }
    }




    fun scrollToTask(position: Int) {
        b.includeRecycler.tasksRecyclerview.smoothScrollToPosition(position)
    }

    // This function will be called by the ItemTouchHelperCallback for swipe actions
    private fun showUndoSnackbar(deletedTask: Task, originalPosition: Int) { // Changed visibility to private
        Snackbar.make(b.activity, getString(R.string.task_deleted), Snackbar.LENGTH_LONG)
            .setAnchorView(b.fab)
            .setAction(getString(R.string.undo)) {
                // User wants to undo: tell ViewModel to restore
                tasksViewModel.restoreItem(deletedTask, originalPosition) // ViewModel handles restoring to original position
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    // If Snackbar dismissed NOT by "Annuler" button
                    if (event != DISMISS_EVENT_ACTION) {
                        tasksViewModel.deleteTask(deletedTask) // Permanently delete in ViewModel
                    }
                }
            })
            .show()
    }

    fun setEmptyState(enabled: Boolean) {
        b.includeRecycler.emptyTasks.apply {
            visibility = if (enabled) View.VISIBLE else View.GONE
            translationY = 0F // Ensure it's at its original position
        }
    }

    // This method is now called by the ItemTouchHelperCallback via TasksAdapter.onDropCompleted
    fun swapItems(fromPosition: Int, toPosition: Int) {
        tasksViewModel.swapItems(fromPosition, toPosition)
        Timber.tag("Activity position").i("from: %s to: %s", fromPosition, toPosition)
    }

    fun setTaskDone(task: Task, done: Boolean = true) {
        tasksViewModel.setTaskDone(task, done)
    }

    fun setSwipeRefreshEnabled(isEnabled: Boolean = true) {
        b.swipeRefresh.isEnabled = isEnabled
    }

    private fun updateListName(newName: String) {
        b.toolbarLayout.title = newName
        b.toolbar.title = newName
    }

    fun changeList(newSelectedListId: String) {
        tasksViewModel.changeList(newSelectedListId)
    }

    private fun onTaskClicked(task: Task, position: Int) {
        val intent = Intent(this, TaskDetailsActivity::class.java).apply {
            putExtra("currentTask", Json.encodeToString(task))
            putExtra("currentList", tasksViewModel.getCurrentListNameForUI())
            putExtra("position", position)
        }
        taskDetailsLauncher.launch(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    @SuppressLint("CommitPrefEdits", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(b.root)


        logIpAddresses()


        // --- Initialize RecyclerView and Adapter ---
        adapter = TasksAdapter(
            context = this,
            onTaskChecked = { task, isChecked ->
                tasksViewModel.setTaskDone(task, isChecked)
            },
            onTaskSwipedToDelete = ::showUndoSnackbar // Show Snackbar here, then call ViewModel's deleteTask on Snackbar dismissal
            ,
            onTaskSwipedToDone = { task ->
                tasksViewModel.setTaskDone(task, true) // Mark as done
            },
            onListOrderChanged = { newTaskList ->
                tasksViewModel.updateTasksOrder(newTaskList)
            },
            onTaskClicked = ::onTaskClicked,
            onEmptyStateChanged = ::setEmptyState
        )

        // Permettre le scroll (et le swipe refresh) même quand on touche l'état vide
        b.includeRecycler.emptyTasks.setOnTouchListener { _, event ->
            // On transfère l'événement tactile directement à la RecyclerView
            b.includeRecycler.tasksRecyclerview.dispatchTouchEvent(event)
        }

        b.includeRecycler.tasksRecyclerview.adapter = adapter
        b.includeRecycler.tasksRecyclerview.layoutManager = LinearLayoutManager(this)

        // ItemTouchHelper setup
        itemTouchHelperCallback = ItemTouchHelperCallback(adapter, this)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(b.includeRecycler.tasksRecyclerview)

        // --- Observe Flows from ViewModel ---
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    tasksViewModel.tasks.collect { newTasks ->
                        adapter.updateTasks(newTasks)
                    }
                }

                launch {
                    tasksViewModel.currentListName.collect { name ->
                        updateListName(name)
                    }
                }

                launch {
                    tasksViewModel.snackbarEvent.collect { message ->
                        Snackbar.make(b.activity, message, Snackbar.LENGTH_LONG)
                            .setAnchorView(b.fab)
                            .show()
                    }
                }

                launch {
                    tasksViewModel.counter.collect { count ->
                        Timber.tag("INT").e("Counter from ViewModel: $count")
                    }
                }
            }
        }
        // --- End Flow Observation ---

        b.swipeRefresh.isEnabled = false // Disable swipe refresh initially
        b.swipeRefresh.setOnRefreshListener {
            // Implement ViewModel refresh logic here if needed
            Handler(Looper.getMainLooper()).postDelayed({
                b.swipeRefresh.isRefreshing = false
            }, 1100)
        }

        // FAB and AddTask button click listeners
        b.fab.setOnClickListener {
            AddTaskFragment.newInstance()
                .show(supportFragmentManager, "dialog")

            tasksViewModel.onAddTaskClicked()
        }

        b.includeRecycler.addTask.setOnClickListener {
            AddTaskFragment.newInstance()
                .show(supportFragmentManager, "dialog")
        }

        if (intent.hasExtra("shortcut")) {
            AddTaskFragment.newInstance()
                .show(supportFragmentManager, "dialog")
        }

        // Bottom App Bar Menu handling
        b.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.contextItem -> {
                    // Pass the current list ID to the BottomFragment
                    tasksViewModel.currentListId.value?.let { listId ->
                        BottomFragment.newInstance(listId).show(supportFragmentManager, "dialog")
                    } ?: run {
                        Toast.makeText(this, getString(R.string.error_context_menu_no_list), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        // Function to show the ChangeListFragment
        fun showListsBottomSheet() {
            ChangeListFragment.newInstance()
                .show(supportFragmentManager, "dialog")
        }

        // Click listeners for showing the ChangeListFragment
        b.bottomAppBar.setNavigationOnClickListener { showListsBottomSheet() }
        b.toolbarLayout.setOnClickListener { showListsBottomSheet() }
        b.appBar.setOnClickListener { showListsBottomSheet() }
        b.toolbar.setOnClickListener { showListsBottomSheet() }

        // Hidden Demo Mode: Long click on the toolbar area
        val demoLongClickListener = View.OnLongClickListener {
            Toast.makeText(this, getString(R.string.init_demo_mode), Toast.LENGTH_SHORT).show()
            tasksViewModel.startDemoMode()
            true
        }
        b.toolbar.setOnLongClickListener(demoLongClickListener)
        b.toolbarLayout.setOnLongClickListener(demoLongClickListener)
        b.appBar.setOnLongClickListener(demoLongClickListener)

        // Touch listener to show bottom app bar (if hidden)
        b.includeRecycler.parentRecyclerV.setOnTouchListener { _, _ ->
            b.bottomAppBar.performShow()
            true
        }
    }

    override fun onStop() {
        super.onStop()
        // The ViewModel, via its Repository, should handle saving the current list state and task positions.
        // No explicit call needed here unless you have a specific "save now" trigger.
        // The ViewModel should ideally observe changes to tasks and save automatically.
    }


    // --- Implementation of BottomFragmentActions ---
    override fun onRenameList(listId: String, newName: String) {
        tasksViewModel.renameList(listId, newName)
    }

    override fun onDeleteList(listId: String) {
        tasksViewModel.deleteList(listId)
    }

    override fun onDeleteAllDoneTasks() {
        tasksViewModel.deleteAllDoneTasks()
    }

    override fun onExportList(listId: String) {
        // Trigger the Activity Result API for file creation
        // We can pass current list name as a suggested file name
        tasksViewModel.currentListName.value?.let { name ->
            exportFileLauncher.launch(name.replace(" ", "_")) // Sanitize for file name
        } ?: run {
            Toast.makeText(this, getString(R.string.error_export_no_list), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onImportList() {
        // Trigger the Activity Result API for file opening
        importFileLauncher.launch(arrayOf("text/*")) // Allow selection of any text file
    }

    override fun onCreateNewList(listTitle: String) {
        tasksViewModel.createNewList(listTitle, emptyList(),true) // Pass 'true' if the new list should be selected immediately
    }

    override fun onChangeLanguage() {
        showLanguageDialog()
    }
    // --- End BottomFragmentActions Implementation ---


    // Une vraie fonction suspendante, propre et sans thread manuel
    private suspend fun getPublicIPAddress(): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.my-ip.io/ip.txt")
            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Timber.tag("Public IP").e(e, "Error fetching public IP")
            null
        }
    }



    private fun showLanguageDialog() {
        val languages = arrayOf("Français", "English")
        val languageCodes = arrayOf("fr", "en")
        
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle(R.string.action_language)
            .setItems(languages) { _, which: Int ->
                val locale = androidx.core.os.LocaleListCompat.forLanguageTags(languageCodes[which])
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locale)
            }
            .show()
    }

    private fun logIpAddresses() {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

                @Suppress("DEPRECATION")
                val ipAddress = wifiManager.connectionInfo.ipAddress
                val localIP = java.net.InetAddress.getByAddress(
                    java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(ipAddress).array()
                ).hostAddress

                val publicIP = getPublicIPAddress()?.trim()
                Timber.tag("IP ADDRESS").i("Local IP : %s - Public IP : %s", localIP, publicIP)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting IP address")
        }
    }
}

