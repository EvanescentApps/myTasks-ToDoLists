/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.home

import android.R.attr.data
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter.formatIpAddress
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Import for new Activity Result API
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ItemTouchHelperCallback
import com.electro.todolist.R
import com.electro.todolist.data.model.Task
import com.electro.todolist.data.repository.TasksRepository
import com.electro.todolist.databinding.ActivityTasksBinding
import com.electro.todolist.ui.fragments.AddTaskFragment
import com.electro.todolist.ui.fragments.BottomFragment
import com.electro.todolist.ui.fragments.BottomFragmentActions
import com.electro.todolist.ui.fragments.ChangeListFragment
import com.electro.todolist.ui.details.TaskDetailsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore

// DataStore for settings (consider if this should be part of a SettingsViewModel/Repository)
val Context.dataStoreSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TasksActivity : AppCompatActivity(), BottomFragmentActions { // Implement the interface

    private lateinit var adapter: TasksAdapter
    private lateinit var b: ActivityTasksBinding
    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var itemTouchHelper: ItemTouchHelper
/*
    private lateinit var scrollListener: RecyclerView.OnScrollListener
*/



    val tasksViewModel: TasksViewModel by viewModels {
        TasksViewModelFactory(TasksRepository.getInstance(this.applicationContext))
    }

    // New Activity Result Launcher for Export
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "Fichier sauvegardé avec succès !", Toast.LENGTH_LONG).show()
            // Persist URI permissions if you want to access it later without user prompt
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            tasksViewModel.exportTaskListToFile(uri)
        } else {
            Toast.makeText(this, "Exportation annulée.", Toast.LENGTH_SHORT).show()
            Timber.tag("Tasks to file").i("File creation cancelled by user.")
        }
    }

    // New Activity Result Launcher for Import
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "Fichier importé avec succès !", Toast.LENGTH_LONG).show()
            tasksViewModel.importTaskListFromFile(uri)
        } else {
            Toast.makeText(this, "Importation annulée.", Toast.LENGTH_SHORT).show()
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
                    Snackbar.make(findViewById(R.id.activity), "Tâche terminée", Snackbar.LENGTH_LONG)
                        .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
                        .setAction("Annuler") {
                            tasksViewModel.setTaskDone(updatedTask, false) // Undo done state
                        }.show()
                }
            }
        }
    }


    @Suppress("unused")
    private suspend fun save(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        dataStoreSettings.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    @Suppress("PropertyName")
    val COUNTER = intPreferencesKey("counter")

    @Suppress("unused")
    suspend fun incrementCounter() {
        dataStoreSettings.edit { settings ->
            val currentCounterValue = settings[COUNTER] ?: 0
            settings[COUNTER] = currentCounterValue + 1
        }
    }

    private fun getIntFlow(KEY: Preferences.Key<Int>): Flow<Int> {
        return dataStoreSettings.data
            .map { preferences ->
                preferences[KEY] ?: 0
            }
    }

    fun scrollToTask(position: Int) {
        b.includeRecycler.tasksRecyclerview.smoothScrollToPosition(position)
    }

    // This function will be called by the ItemTouchHelperCallback for swipe actions
    private fun showUndoSnackbar(deletedTask: Task, originalPosition: Int) { // Changed visibility to private
        Snackbar.make(b.activity, "Tâche supprimée", Snackbar.LENGTH_LONG)
            .setAnchorView(b.fab)
            .setAction("Annuler") {
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

    @SuppressLint("CommitPrefEdits", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Observe DataStore counter (consider moving this to ViewModel if it affects UI)
        getIntFlow(COUNTER).asLiveData().observe(this) {
            Timber.tag("INT").e("Counter: $it")
        }

        // IP address lookup (fine to keep here if it's purely informational and not business logic)
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION") // formatIpAddress is deprecated, but common for older APIs
                val localIP = formatIpAddress(wifiManager.connectionInfo.ipAddress)
                val publicIP = getPublicIPAddress()?.trim()
                Timber.tag("IP ADDRESS").i("Local IP : %s - Public IP : %s", localIP, publicIP)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting IP address")
        }

        // --- Initialize RecyclerView and Adapter ---
        adapter = TasksAdapter(
            currentTasks = mutableListOf(), // Start with empty list
            context = this,
            onTaskChecked = { task, isChecked ->
                tasksViewModel.setTaskDone(task, isChecked)
            },
            onTaskSwipedToDelete = { task, originalPosition ->
                showUndoSnackbar(task, originalPosition) // Show Snackbar here, then call ViewModel's deleteTask on Snackbar dismissal
            },
            onTaskSwipedToDone = { task ->
                tasksViewModel.setTaskDone(task, true) // Mark as done
            },
            onTaskMoved = { from, to ->
                // This callback from adapter's onDropCompleted
                tasksViewModel.swapItems(from, to) // Persist the new order
            },
            onTaskClicked = { task, position ->
                val intent = Intent(this, TaskDetailsActivity::class.java).apply {
                    putExtra("currentTask", Json.encodeToString(task))
                    putExtra("currentList", tasksViewModel.getCurrentListNameForUI()) // Get current list name from ViewModel
                    putExtra("position", position)
                }
                taskDetailsLauncher.launch(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            },
            onEmptyStateChanged = { isEmpty ->
                setEmptyState(isEmpty)
            }
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

        // --- Observe LiveData from ViewModel ---
        // Observe the list of tasks
        tasksViewModel.tasks.observe(this) { newTasks ->
            adapter.updateTasks(newTasks) // Update adapter with new data
        }

        // Observe the current list name (for toolbar title)
        tasksViewModel.currentListName.observe(this) { name ->
            updateListName(name) // Update toolbar title
        }

        // Observe Snackbar messages from ViewModel
        tasksViewModel.snackbarEvent.observe(this) { message ->
            Snackbar.make(b.activity, message, Snackbar.LENGTH_LONG)
                .setAnchorView(b.fab)
                .show()
        }
        // --- End LiveData Observation ---

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

            lifecycleScope.launch(Dispatchers.IO) {
                incrementCounter() // Consider moving this DataStore interaction to ViewModel
            }
        }

        b.includeRecycler.addTask.setOnClickListener {
            AddTaskFragment.newInstance()
                .show(supportFragmentManager, "dialog")
        }

        if (intent.hasExtra("shortcut")) {
            AddTaskFragment.newInstance()
                .show(supportFragmentManager, "dialog")
        }

        // Context menu item (BottomFragment)
        findViewById<View>(R.id.contextItem).setOnClickListener {
            // Pass the current list ID to the BottomFragment
            tasksViewModel.currentListId.value?.let { listId ->
                BottomFragment.newInstance(listId).show(supportFragmentManager, "dialog")
            } ?: run {
                Toast.makeText(this, "Impossible d'ouvrir le menu contextuel sans liste sélectionnée.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Impossible d'exporter: nom de liste introuvable.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onImportList() {
        // Trigger the Activity Result API for file opening
        importFileLauncher.launch(arrayOf("text/*")) // Allow selection of any text file
    }

    override fun onCreateNewList(listTitle: String) {
        tasksViewModel.createNewList(listTitle, emptyList(),true) // Pass 'true' if the new list should be selected immediately
    }
    // --- End BottomFragmentActions Implementation ---


    private fun getPublicIPAddress(): String? {
        var value: String? = null
        val es = Executors.newSingleThreadExecutor()
        val result = es.submit<String?> {
            try {
                val url = URL("https://api.my-ip.io/ip.txt")
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                    val r = BufferedReader(InputStreamReader(`in`))
                    val total = StringBuilder()
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        total.append(line).append('\n')
                    }
                    urlConnection.disconnect()
                    return@submit total.toString()
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: IOException) {
                Timber.tag("Public IP: ").e(e.message ?: "Unknown IOException")
            }
            null
        }
        try {
            value = result.get()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            es.shutdown()
        }
        return value
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Handle settings action, e.g., startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}