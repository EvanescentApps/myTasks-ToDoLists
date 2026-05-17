@file:Suppress("PropertyName")

package com.evanescent.mytasks.data.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.evanescent.mytasks.R
import com.evanescent.mytasks.data.db.TaskListDao
import com.evanescent.mytasks.data.model.SerialListObject
import com.evanescent.mytasks.data.model.TaskList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID // For generating unique IDs
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class ListManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskListDao: TaskListDao
) {

    // SharedPreferences for storing list metadata (IDs, titles, positions)
    private val listsPrefs: SharedPreferences = context.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)

    // Key for storing the serialized list of SerialListObjects (LEGACY)
    private val LISTS_SERIALIZED_KEY = "listsSerialized"
    // Key for storing the ID of the last opened list
    private val LAST_OPENED_LIST_KEY = "lastOpenedListId"

    // Current active list ID, managed by the ViewModel/Repository
    var currentListName: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                listsPrefs.edit { putString(LAST_OPENED_LIST_KEY, value) }
                Timber.tag("ListManager").d("currentListName persisted: $value")
            }
        }

    // --- Initialization and Default List Logic ---

    init {
        // Migrate from SharedPreferences to Room if necessary
        migrateIfNecessary()

        // Initialize currentListName to a default value
        val lastOpenedId = listsPrefs.getString(LAST_OPENED_LIST_KEY, "") ?: ""
        currentListName = lastOpenedId
        // Ensure default lists exist if app is launched for the first time
        runBlocking {
            getDefaultList()
        }
    }

    private fun migrateIfNecessary() {
        val listsJson = listsPrefs.getString(LISTS_SERIALIZED_KEY, null)
        if (listsJson != null) {
            try {
                val legacyLists: List<SerialListObject> = Json.decodeFromString(listsJson)
                if (legacyLists.isNotEmpty()) {
                    runBlocking {
                        val existingLists = taskListDao.getAllLists()
                        if (existingLists.isEmpty()) {
                            Timber.tag("ListManager").d("Migrating lists from SharedPreferences to Room.")
                            val newLists = legacyLists.map {
                                TaskList(id = it.id, title = it.title, position = it.position)
                            }
                            taskListDao.insertLists(newLists)
                        }
                    }
                }
                // Clear the legacy key to avoid repeated migration
                listsPrefs.edit { remove(LISTS_SERIALIZED_KEY) }
            } catch (e: Exception) {
                Timber.e(e, "Error migrating lists from SharedPreferences.")
            }
        }
    }

    /**
     * Ensures that default lists are present if the app is run for the first time,
     * and ensures currentListName points to a valid list.
     */
    suspend fun getDefaultList() {
        val allLists = taskListDao.getAllLists()

        if (allLists.isEmpty()) {
            Timber.tag("ListManager").d("No lists found, creating default lists.")
            createDefaultLists()
            val newlyCreatedLists = taskListDao.getAllLists()
            currentListName = newlyCreatedLists.firstOrNull()?.id ?: "defaultList"
        } else {
            // If current selection is invalid or empty, pick the first one
            if (currentListName.isEmpty() || allLists.none { it.id == currentListName }) {
                currentListName = allLists.firstOrNull()?.id ?: "defaultList"
                Timber.tag("ListManager").d("Selection was invalid, reset to: $currentListName")
            }
        }
    }

    /**
     * Creates a set of default lists and saves them.
     */
    private suspend fun createDefaultLists() {
        val defaultLists = listOf(
            TaskList(id = "list1", title = context.getString(R.string.list_my_tasks), position = 0),
            TaskList(id = "list2", title = context.getString(R.string.list_today), position = 1),
            TaskList(id = "list3", title = context.getString(R.string.list_tomorrow), position = 2)
        )
        taskListDao.insertLists(defaultLists)
        listsPrefs.edit {
            putString(LAST_OPENED_LIST_KEY, "list1") // Set a default first opened list
        }
    }

    // --- List Retrieval ---

    /**
     * Retrieves all lists as a list of TaskList.
     * @return A list of TaskList, or an empty list if none exist.
     */
    suspend fun getAllLists(): List<TaskList> {
        return taskListDao.getAllLists()
    }

    /**
     * Retrieves all lists as a MutableMap of ID to Title.
     * Used by `TasksViewModel.loadAllUserLists` to rebuild the `SerialListObject`s.
     * @return A MutableMap where keys are list IDs and values are list titles.
     */
    suspend fun getAllListsAsMap(): MutableMap<String, String> {
        return getAllLists().associate { it.id to it.title }.toMutableMap()
    }

    /**
     * Gets the title of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The title of the list, or null if not found.
     */
    suspend fun getListTitle(listId: String): String? {
        return taskListDao.getListById(listId)?.title
    }

    /**
     * Gets the current display position of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The position of the list, or 0 if not found (or a default).
     */
    suspend fun getListPosition(listId: String): Int {
        return taskListDao.getListById(listId)?.position ?: 0
    }

    // --- List Modification ---

    /**
     * Creates a new list with the given name and adds it to the collection.
     * @param name The title of the new list.
     * @return The unique ID of the newly created list.
     */
    suspend fun createList(name: String): String {
        val newListId = UUID.randomUUID().toString() // Generate a unique ID
        val currentLists = taskListDao.getAllLists()
        val newPosition = currentLists.size // Add to the end

        taskListDao.insertList(TaskList(newListId, name, newPosition))

        Timber.d("New list created: ID=$newListId, Name='$name', Position=$newPosition")
        return newListId
    }

    /**
     * Renames an existing list.
     * @param newName The new title for the list.
     * @param listId The ID of the list to rename.
     */
    suspend fun renameList(newName: String, listId: String) {
        val list = taskListDao.getListById(listId)
        if (list != null) {
            taskListDao.updateList(list.copy(title = newName))
            Timber.d("List '$listId' renamed to '$newName'.")
        } else {
            Timber.w("Attempted to rename non-existent list with ID: $listId")
        }
    }

    /**
     * Deletes a list from the collection.
     * Note: Deleting the list's *tasks* SharedPreferences is handled in TasksRepository.
     * @param listId The ID of the list to delete.
     */
    suspend fun deleteList(listId: String) {
        taskListDao.deleteListById(listId)
        Timber.d("List '$listId' successfully deleted.")

        val remainingLists = taskListDao.getAllLists()
        // Re-assign positions for remaining lists
        remainingLists.forEachIndexed { index, listObject ->
            if (listObject.position != index) {
                taskListDao.updateList(listObject.copy(position = index))
            }
        }

        // If the deleted list was the last opened, update that preference
        if (listsPrefs.getString(LAST_OPENED_LIST_KEY, "") == listId) {
            listsPrefs.edit {
                putString(LAST_OPENED_LIST_KEY, remainingLists.firstOrNull()?.id ?: "")
            }
        }
    }

    /**
     * Saves the new order (positions) of lists after a drag-and-drop operation.
     * This updates the `position` property for each `TaskList` in the stored list.
     * @param lists The reordered list of SerialListObject from the adapter.
     */
    suspend fun saveListPositions(lists: List<SerialListObject>) {
        val updatedLists = lists.mapIndexed { index, serialListObject ->
            TaskList(id = serialListObject.id, title = serialListObject.title, position = index)
        }
        taskListDao.insertLists(updatedLists)
        Timber.d("List positions saved.")
    }
}

