/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

@file:Suppress("PropertyName")

package com.electro.todolist.data.repository

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // Extension function for SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.electro.todolist.data.manager.FileManager
import com.electro.todolist.data.manager.ListManager
import com.electro.todolist.data.model.SerialListObject
import com.electro.todolist.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.dataStoreSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")
open class TasksRepository private constructor(private val context: Context) {

    private val applicationContext = context.applicationContext

    private val listManager: ListManager = ListManager(context)
    private val fileManager: FileManager = FileManager(context)

    private val json = Json { ignoreUnknownKeys = true }


    private val COUNTER_KEY = intPreferencesKey("counter")

    // --- LOGIQUE DATASTORE DÉPLACÉE ICI ---

    // Définir les clés ici pour qu'elles soient privées au Repository
    // Une fonction publique pour que le ViewModel puisse observer
    val counterFlow: Flow<Int> = applicationContext.dataStoreSettings.data
        .map { preferences ->
            preferences[COUNTER_KEY] ?: 0
        }

    // Une fonction publique pour que le ViewModel puisse demander une action
    suspend fun incrementCounter() {
        applicationContext.dataStoreSettings.edit { settings ->
            val currentCounterValue = settings[COUNTER_KEY] ?: 0
            settings[COUNTER_KEY] = currentCounterValue + 1
        }
    }

    @Suppress("unused")
    private suspend fun save(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        applicationContext.dataStoreSettings.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    @Suppress("unused")
    private fun getIntFlow(KEY: Preferences.Key<Int>): Flow<Int> {
        return applicationContext.dataStoreSettings.data
            .map { preferences ->
                preferences[KEY] ?: 0
            }
    }

    // --- Task-related Operations ---

    /**
     * Retrieves all tasks for a given list ID.
     * @param listId The ID of the list to fetch tasks from.
     * @return A sorted list of Task objects.
     */
    fun getTasksForList(listId: String): List<Task> {
        val prefs = context.getSharedPreferences(listId, AppCompatActivity.MODE_PRIVATE)
        return prefs.all.mapNotNull { entry ->
            val taskJson = entry.value as? String
            if (taskJson != null) {
                try {
                    json.decodeFromString<Task>(taskJson)
                } catch (e: Exception) {
                    Timber.e(e, "Error decoding task JSON from list '%s': %s", listId, taskJson)
                    null // Return null if deserialization fails
                }
            } else {
                null // Return null if value is not a String
            }
        }.sortedWith(compareBy<Task> { it.done }.thenBy { it.position }) // Sort by done status then position
    }

    /**
     * Adds a new task to the currently selected list.
     * @param task The Task object to add.
     */
    fun addTask(task: Task) {
        val currentListPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        currentListPrefs.edit {
            putString(task.creationDate.toString(), json.encodeToString(task))
        }
        Timber.d("Task added to list '${getCurrentListName()}': ${task.title}")
    }

    /**
     * Deletes a task from the currently selected list.
     * @param task The Task object to delete.
     */
    fun deleteTask(task: Task) {
        val currentListPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        currentListPrefs.edit {
            remove(task.creationDate.toString())
        }
        Timber.d("Task deleted from list '${getCurrentListName()}': ${task.title}")
    }

    /**
     * Sets the 'done' status of a task in the currently selected list.
     * @param task The Task object to update.
     * @param done The new 'done' status.
     */
    fun setTaskDone(task: Task, done: Boolean) {
        val currentListPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        val updatedTask = task.copy(done = done) // Create a new task with updated 'done' status
        currentListPrefs.edit {
            putString(updatedTask.creationDate.toString(), json.encodeToString(updatedTask))
        }
        Timber.d("Task '${updatedTask.title}' done status set to $done in list '${getCurrentListName()}'")
    }

    /**
     * Updates an existing task's properties in the currently selected list.
     * @param originalTask The original Task object (used for identifying the entry).
     * @param updatedTask The Task object with updated properties.
     */
    fun updateTask(originalTask: Task, updatedTask: Task) {
        val currentListPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        currentListPrefs.edit {
            // Use the original task's unique ID to overwrite the old entry
            putString(originalTask.creationDate.toString(), json.encodeToString(updatedTask))
        }
        Timber.d("Task updated in list '${getCurrentListName()}': ${updatedTask.title}")
    }

    /**
     * Saves the new order (positions) of tasks after a drag-and-drop operation.
     * @param tasks The reordered list of Task objects.
     */
    fun saveTaskPositions(tasks: List<Task>) {
        val listPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        listPrefs.edit {
            tasks.forEachIndexed { index, task ->
                val updatedTask = task.copy(position = index) // Create a copy with the new position
                // Overwrite the existing task with updated position
                putString(updatedTask.creationDate.toString(), json.encodeToString(updatedTask))
            }
        }
        Timber.d("Task positions saved for list '${getCurrentListName()}'")
    }

    /**
     * Deletes all tasks marked as 'done' from the currently selected list.
     */
    fun deleteAllDoneTasksFromCurrentList() {
        val listPrefs = context.getSharedPreferences(getCurrentListName(), AppCompatActivity.MODE_PRIVATE)
        listPrefs.edit {
            // Iterate over a copy of the current tasks to avoid ConcurrentModificationException
            listPrefs.all.mapNotNull { entry -> entry.value as? String }.forEach { taskJson ->
                try {
                    val task = json.decodeFromString<Task>(taskJson)
                    if (task.done) {
                        remove(task.creationDate.toString()) // Remove only if done
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error decoding task for deletion check during deleteAllDoneTasks: %s", taskJson)
                }
            }
        }
        Timber.d("All done tasks deleted from list '${getCurrentListName()}'")
    }

    /**
     * Adds a collection of tasks to a specific list (e.g., used during import).
     * @param listId The ID of the list to add tasks to.
     * @param tasksToAdd The list of Task objects to add.
     */
    fun addTasksToList(listId: String, tasksToAdd: List<Task>) {
        val listPrefs = context.getSharedPreferences(listId, AppCompatActivity.MODE_PRIVATE)
        listPrefs.edit {
            tasksToAdd.forEach { task ->
                putString(task.creationDate.toString(), json.encodeToString(task))
            }
        }
        Timber.d("Added ${tasksToAdd.size} tasks to list '$listId'")
    }

    // --- List Management Operations (NEW/Modified) ---

    /**
     * Gets the title of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The title of the list, or null if not found.
     */
    fun getListTitle(listId: String): String? {
        return listManager.getListTitle(listId)
    }

    /**
     * Gets the current position of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The position of the list, or 0 if not found (or default).
     */
    fun getListPosition(listId: String): Int {
        return listManager.getListPosition(listId)
    }

    /**
     * Creates a new list and sets it as the current one.
     * @param name The title of the new list.
     * @return The unique ID of the newly created list.
     */
    fun createList(name: String): String {
        return listManager.createList(name) // Assuming ListManager.createList now returns the ID
    }

    /**
     * Renames an existing list.
     * @param newName The new title for the list.
     * @param listId The ID of the list to rename.
     */
    fun renameList(newName: String, listId: String) {
        listManager.renameList(newName, listId)
    }

    /**
     * Deletes a list and its associated tasks.
     * @param listId The ID of the list to delete.
     */
    fun deleteList(listId: String) {
        // First, delete the SharedPreferences file for the tasks within this list
        context.getSharedPreferences(listId, AppCompatActivity.MODE_PRIVATE).edit { clear() }
        Timber.d("Cleared tasks from SharedPreferences for list ID: $listId")

        // Then, delete the list entry itself from the ListManager
        listManager.deleteList(listId)
        Timber.d("List '$listId' deleted from ListManager.")
    }

    /**
     * Retrieves all user-defined lists as a map of ID to Title.
     * @return A MutableMap where keys are list IDs and values are list titles.
     */
    fun getAllLists(): MutableMap<String, String> {
        return listManager.getAllListsAsMap() // Assuming ListManager has this method
    }

    /**
     * Saves the new order (positions) of lists after a drag-and-drop operation.
     * @param lists The reordered list of SerialListObject.
     */
    fun saveListPositions(lists: List<SerialListObject>) {
        listManager.saveListPositions(lists) // Delegate to ListManager
    }

    /**
     * Gets the ID of the currently selected list.
     * @return The ID string of the current list.
     */
    fun getCurrentListName(): String = listManager.currentListName

    /**
     * Sets the ID of the currently selected list.
     * @param newName The ID string of the list to set as current.
     */
    fun setCurrentListName(newName: String) {
        listManager.currentListName = newName
    }

    /**
     * Ensures a default list exists and sets it as current if no other list is selected.
     */
    fun getDefaultList() {
        listManager.getDefaultList()
    }

    // --- File Management Operations (Delegated to FileManager) ---

    /**
     * Writes a list of tasks to a specified URI (file).
     * @param tasksToExport The list of Task objects to write.
     * @param uri The URI of the file to write to.
     */
    fun writeTaskListTofile(tasksToExport: List<Task>, uri: Uri) {
        fileManager.writeTaskListTofile(tasksToExport, uri)
    }

    /**
     * Reads text content from a specified URI (file).
     * @param uri The URI of the file to read from.
     * @return The text content of the file.
     */
    fun readTextContent(uri: Uri): String = fileManager.readTextContent(uri)

    // --- Singleton Pattern ---
    companion object {
        @Volatile
        private var INSTANCE: TasksRepository? = null

        fun getInstance(appContext: Context): TasksRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TasksRepository(appContext).also { INSTANCE = it }
            }
        }

        // DP extension property - kept for convenience, not directly related to repository logic
        @Suppress("unused")
        val Number.dp get() = toFloat() * (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}