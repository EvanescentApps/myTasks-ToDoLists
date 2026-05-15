@file:Suppress("PropertyName")

package com.evanescent.mytasks.data.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.evanescent.mytasks.data.model.SerialListObject
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID // For generating unique IDs
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class ListManager @Inject constructor(@ApplicationContext private val context: Context) {

    // SharedPreferences for storing list metadata (IDs, titles, positions)
    private val listsPrefs: SharedPreferences = context.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)

    // Key for storing the serialized list of SerialListObjects
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
        // Initialize currentListName to a default value
        val lastOpenedId = listsPrefs.getString(LAST_OPENED_LIST_KEY, "") ?: ""
        currentListName = lastOpenedId
        // Ensure default lists exist if app is launched for the first time
        getDefaultList()
    }

    /**
     * Ensures that default lists are present if the app is run for the first time,
     * and ensures currentListName points to a valid list.
     */
    fun getDefaultList() {
        val allLists = getAllLists()

        if (allLists.isEmpty()) {
            Timber.tag("ListManager").d("No lists found, creating default lists.")
            createDefaultLists()
            val newlyCreatedLists = getAllLists()
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
    private fun createDefaultLists() {
        val serialLists = arrayListOf(
            SerialListObject(id = "list1", title = "Mes tâches", position = 0),
            SerialListObject(id = "list2", title = "Aujourd'hui", position = 1),
            SerialListObject(id = "list3", title = "Demain", position = 2)
        )
        val listsSerialized = Json.encodeToString(serialLists)

        listsPrefs.edit {
            putString(LISTS_SERIALIZED_KEY, listsSerialized)
            putString(LAST_OPENED_LIST_KEY, "list1") // Set a default first opened list
        }
    }

    // --- List Retrieval ---

    /**
     * Retrieves all lists as a list of SerialListObject.
     * This is the primary method for the Repository/ViewModel to get list data.
     * @return A list of SerialListObject, or an empty list if none exist.
     */
    fun getAllLists(): List<SerialListObject> {
        val listsJson = listsPrefs.getString(LISTS_SERIALIZED_KEY, null)
        return if (listsJson != null) {
            try {
                Json.decodeFromString(listsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding serialized lists from SharedPreferences.")
                emptyList() // Return empty list on error
            }
        } else {
            emptyList()
        }
    }

    /**
     * Retrieves all lists as a MutableMap of ID to Title.
     * Used by `TasksViewModel.loadAllUserLists` to rebuild the `SerialListObject`s.
     * @return A MutableMap where keys are list IDs and values are list titles.
     */
    fun getAllListsAsMap(): MutableMap<String, String> {
        return getAllLists().associate { it.id to it.title }.toMutableMap()
    }

    /**
     * Gets the title of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The title of the list, or null if not found.
     */
    fun getListTitle(listId: String): String? {
        return getAllLists().firstOrNull { it.id == listId }?.title
    }

    /**
     * Gets the current display position of a specific list by its ID.
     * @param listId The ID of the list.
     * @return The position of the list, or 0 if not found (or a default).
     */
    fun getListPosition(listId: String): Int {
        return getAllLists().firstOrNull { it.id == listId }?.position ?: 0
    }

    // --- List Modification ---

    /**
     * Creates a new list with the given name and adds it to the collection.
     * @param name The title of the new list.
     * @return The unique ID of the newly created list.
     */
    fun createList(name: String): String {
        val newListId = UUID.randomUUID().toString() // Generate a unique ID
        val currentLists = getAllLists().toMutableList()
        val newPosition = currentLists.size // Add to the end

        currentLists.add(SerialListObject(newListId, name, newPosition))
        saveAllLists(currentLists)

        Timber.d("New list created: ID=$newListId, Name='$name', Position=$newPosition")
        return newListId
    }

    /**
     * Renames an existing list.
     * @param newName The new title for the list.
     * @param listId The ID of the list to rename.
     */
    fun renameList(newName: String, listId: String) {
        val currentLists = getAllLists().toMutableList()
        val index = currentLists.indexOfFirst { it.id == listId }

        if (index != -1) {
            currentLists[index] = currentLists[index].copy(title = newName)
            saveAllLists(currentLists)
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
    fun deleteList(listId: String) {
        val currentLists = getAllLists().toMutableList()
        var removed = false
        val iterator = currentLists.iterator()
        while (iterator.hasNext()) {
            val listObject = iterator.next()
            if (listObject.id == listId) {
                iterator.remove() // Safely remove the element
                removed = true
                break // Assuming list IDs are unique, we can stop after finding it
            }
        }

        if (removed) {
            // Re-assign positions for remaining lists
            currentLists.forEachIndexed { index, listObject ->
                listObject.position = index
            }
            saveAllLists(currentLists)
            Timber.d("List '$listId' successfully deleted.")
        } else {
            Timber.w("Attempted to delete non-existent list with ID: $listId")
        }
        // If the deleted list was the last opened, update that preference
        if (listsPrefs.getString(LAST_OPENED_LIST_KEY, "") == listId) {
            listsPrefs.edit {
                putString(LAST_OPENED_LIST_KEY, currentLists.firstOrNull()?.id ?: "")
            }
        }
    }

    /**
     * Saves the new order (positions) of lists after a drag-and-drop operation.
     * This updates the `position` property for each `SerialListObject` in the stored list.
     * @param lists The reordered list of SerialListObject from the adapter.
     */
    fun saveListPositions(lists: List<SerialListObject>) {
        val currentLists = getAllLists().toMutableList() // Get the actual stored lists

        // Create a map for quick lookup of original list objects by ID
        val currentListsMap = currentLists.associateBy { it.id }.toMutableMap()

        // Update positions based on the provided reordered list
        val updatedLists = lists.mapIndexed { index, reorderedListObject ->
            currentListsMap[reorderedListObject.id]?.copy(position = index)
                ?: reorderedListObject.copy(position = index) // Fallback if somehow not in original
        }.toMutableList()

        saveAllLists(updatedLists)
        Timber.d("List positions saved.")
    }

    /**
     * Internal helper to save the entire list of SerialListObject to SharedPreferences.
     * @param lists The list of SerialListObject to save.
     */
    private fun saveAllLists(lists: List<SerialListObject>) {
        // Always sort by position before saving to maintain a consistent order for retrieval
        val sortedLists = lists.sortedBy { it.position }
        val listsSerialized = Json.encodeToString(sortedLists)
        listsPrefs.edit {
            putString(LISTS_SERIALIZED_KEY, listsSerialized)
        }
    }
}

