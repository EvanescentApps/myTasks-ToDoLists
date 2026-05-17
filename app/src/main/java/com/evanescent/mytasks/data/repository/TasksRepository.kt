/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

@file:Suppress("PropertyName")

package com.evanescent.mytasks.data.repository

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.util.DisplayMetrics
import com.evanescent.mytasks.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // Extension function for SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.evanescent.mytasks.data.manager.FileManager
import com.evanescent.mytasks.data.manager.ListManager
import com.evanescent.mytasks.data.model.SerialListObject
import com.evanescent.mytasks.data.model.Task
import com.evanescent.mytasks.data.model.TaskList
import com.evanescent.mytasks.data.model.Priority
import com.evanescent.mytasks.data.db.AppDatabase
import com.evanescent.mytasks.data.db.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStoreSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")

open class TasksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listManager: ListManager,
    private val fileManager: FileManager,
    private val taskDao: TaskDao
) {

    private val applicationContext = context.applicationContext
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

    // --- Task-related Operations (Room-backed) ---

    /**
     * Retrieves all tasks for a given list ID as a reactive Flow.
     */
    fun getTasksFlowForList(listId: String): Flow<List<Task>> {
        return taskDao.getTasksForList(listId)
    }

    /**
     * Retrieves all tasks for a given list ID once.
     */
    suspend fun getTasksForList(listId: String): List<Task> {
        return taskDao.getTasksForListOnce(listId)
    }

    /**
     * Adds a new task at the very top of the list.
     */
    suspend fun addTaskAtTop(task: Task) {
        val currentListId = getCurrentListName()
        val taskWithList = task.copy(listId = currentListId)
        taskDao.insertTaskAtTop(taskWithList)
        Timber.d("Task added to DB at TOP: ${task.title}")
    }

    /**
     * Deletes a task.
     */
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        Timber.d("Task deleted from DB: ${task.title}")
    }

    /**
     * Sets the 'done' status.
     * Reverting back to simple update. The DB/ViewModel will naturally handle ordering.
     */
    suspend fun setTaskDone(task: Task, done: Boolean) {
        val updatedTask = task.copy(done = done)
        taskDao.updateTask(updatedTask)
        Timber.d("TasksRepository: setTaskDone called for task ${task.id} -> $done")
    }

    /**
     * Updates an existing task.
     */
    suspend fun updateTask(originalTask: Task, updatedTask: Task) {
        // Since we use auto-generated IDs, updatedTask should have the same ID as originalTask
        taskDao.updateTask(updatedTask)
    }

    /**
     * Saves the new task order after drag-and-drop.
     * Tasks MUST come pre-organized by done group and pre-normalized with correct positions!
     * (TasksAdapter.onDropCompleted() handles this reorganization.)
     * We just persist them to the database as-is.
     */
    suspend fun saveTaskPositions(tasks: List<Task>) {
        Timber.d("TasksRepository: Saving positions for ${tasks.size} tasks")
        taskDao.updateTasksOrder(tasks)
        Timber.d("TasksRepository: Task positions persisted to DB")
    }

    /**
     * Deletes all tasks marked as 'done' from the currently selected list.
     */
    suspend fun deleteAllDoneTasksFromCurrentList() {
        taskDao.deleteAllDoneTasks(getCurrentListName())
    }

    /**
     * Adds a collection of tasks to a specific list (e.g., used during import).
     */
    suspend fun addTasksToList(listId: String, tasksToAdd: List<Task>) {
        tasksToAdd.forEach { task ->
            taskDao.insertTask(task.copy(listId = listId))
        }
    }

    // --- List Management Operations (NEW/Modified) ---

    /**
     * Gets the title of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The title of the list, or null if not found.
     */
    suspend fun getListTitle(listId: String): String? {
        return listManager.getListTitle(listId)
    }

    /**
     * Gets the current position of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The position of the list, or 0 if not found (or default).
     */
    suspend fun getListPosition(listId: String): Int {
        return listManager.getListPosition(listId)
    }

    /**
     * Creates a new list and sets it as the current one.
     * @param name The title of the new list.
     * @return The unique ID of the newly created list.
     */
    suspend fun createList(name: String): String {
        return listManager.createList(name)
    }

    /**
     * Renames an existing list.
     * @param newName The new title for the list.
     * @param listId The ID of the list to rename.
     */
    suspend fun renameList(newName: String, listId: String) {
        listManager.renameList(newName, listId)
    }

    /**
     * Deletes a list and its associated tasks.
     * @param listId The ID of the list to delete.
     */
    suspend fun deleteList(listId: String) {
        // Delete tasks belonging to this list from Room
        taskDao.deleteTasksByListId(listId)
        Timber.d("Deleted all tasks from Room for list ID: $listId")

        // Then, delete the list entry itself from the ListManager
        listManager.deleteList(listId)
        Timber.d("List '$listId' deleted from ListManager.")
    }

    /**
     * Retrieves all user-defined lists as a map of ID to Title.
     * @return A MutableMap where keys are list IDs and values are list titles.
     */
    suspend fun getAllLists(): MutableMap<String, String> {
        return listManager.getAllListsAsMap()
    }

    /**
     * Retrieves all user-defined lists as a list of TaskList objects.
     */
    suspend fun getAllListsObjects(): List<TaskList> {
        return listManager.getAllLists()
    }

    /**
     * Saves the new order (positions) of lists after a drag-and-drop operation.
     * @param lists The reordered list of SerialListObject.
     */
    suspend fun saveListPositions(lists: List<SerialListObject>) {
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
    suspend fun getDefaultList() {
        listManager.getDefaultList()
    }

    /**
     * Adds demonstration tasks to the current list.
     */
    suspend fun createDemoData() {
        val currentListId = getCurrentListName()
        
        val demoTasks = listOf(
            Task(title = context.getString(R.string.demo_task_1_title), description = context.getString(R.string.demo_task_1_desc), listId = currentListId, position = 0),
            Task(title = context.getString(R.string.demo_task_2_title), description = context.getString(R.string.demo_task_2_desc), listId = currentListId, position = 1, priorityName = Priority.VERY_HIGH.first, priorityColor = Priority.VERY_HIGH.second),
            Task(title = context.getString(R.string.demo_task_3_title), description = context.getString(R.string.demo_task_3_desc), listId = currentListId, position = 2, priorityName = Priority.HIGH.first, priorityColor = Priority.HIGH.second),
            Task(title = context.getString(R.string.demo_task_4_title), description = context.getString(R.string.demo_task_4_desc), listId = currentListId, position = 3, date = System.currentTimeMillis() + 3600000),
            Task(title = context.getString(R.string.demo_task_5_title), description = context.getString(R.string.demo_task_5_desc), listId = currentListId, position = 4, duration = 1500000),
            Task(title = context.getString(R.string.demo_task_6_title), description = context.getString(R.string.demo_task_6_desc), listId = currentListId, position = 5, done = true)
        )
        
        addTasksToList(currentListId, demoTasks)
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

    companion object {
        // DP extension property - kept for convenience, not directly related to repository logic
        @Suppress("unused")
        val Number.dp get() = toFloat() * (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}

