package com.electro.todolist.ui.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.electro.todolist.data.model.SerialListObject
import com.electro.todolist.data.model.Task
import com.electro.todolist.data.repository.TasksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class TasksViewModel(private val tasksRepository: TasksRepository) : ViewModel() {

    // --- LiveData for Tasks and UI State (Existing) ---
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks



    // Exposer le compteur à l'Activity via le ViewModel
    val counter: LiveData<Int> = tasksRepository.counterFlow.asLiveData()

    // L'action est déclenchée depuis le ViewModel
    fun onAddTaskClicked() { // Renommez selon l'action
        viewModelScope.launch {
            tasksRepository.incrementCounter()
        }
    }


    private val _isEmptyStateEnabled = MutableLiveData<Boolean>()
    val isEmptyStateEnabled: LiveData<Boolean> = _isEmptyStateEnabled

    private val _currentListName = MutableLiveData<String>()
    val currentListName: LiveData<String> = _currentListName

    private val _snackbarEvent = MutableLiveData<String>()
    val snackbarEvent: LiveData<String> = _snackbarEvent

    // --- NEW: LiveData for ALL Lists ---
    private val _allLists = MutableLiveData<List<SerialListObject>>()
    val allLists: LiveData<List<SerialListObject>> = _allLists

    private val _currentListId = MutableLiveData<String>()
    val currentListId: LiveData<String> = _currentListId // Expose the ID

    init {
        // Load initial data when the ViewModel is created
        loadTasksForCurrentList()
        // NEW: Load all lists as well
        loadAllUserLists()
    }

    // --- Task-related Operations (Modified/Existing) ---

    private fun loadTasksForCurrentList() {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.getDefaultList() // Ensure default list is set up
            val currentListId = tasksRepository.getCurrentListName()

            val loadedTasks = tasksRepository.getTasksForList(currentListId)

            withContext(Dispatchers.Main) {
                _tasks.value = loadedTasks
                _isEmptyStateEnabled.value = loadedTasks.isEmpty()
                // Update currentListName based on the repository's current list title
                _currentListName.value = tasksRepository.getListTitle(currentListId)
                _currentListId.value = currentListId

            }
        }
    }

    fun updateTask(originalTask: Task, updatedTask: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.updateTask(originalTask, updatedTask)
            loadTasksForCurrentList() // Reload tasks to reflect the update in the UI
            _snackbarEvent.postValue("Tâche mise à jour")
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.addTask(task)
            loadTasksForCurrentList()
            _snackbarEvent.postValue("Tâche ajoutée")
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.deleteTask(task)
            loadTasksForCurrentList()
            _snackbarEvent.postValue("Tâche supprimée")
        }
    }

    fun setTaskDone(task: Task, done: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.setTaskDone(task, done)
            loadTasksForCurrentList()
            _snackbarEvent.postValue("Tâche mise à jour")
        }
    }

    fun restoreItem(task: Task, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.addTask(task)
            loadTasksForCurrentList()
            _snackbarEvent.postValue("Tâche restaurée")
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = _tasks.value?.toMutableList() ?: return@launch
            if (fromPosition < 0 || fromPosition >= currentTasks.size ||
                toPosition < 0 || toPosition >= currentTasks.size) {
                return@launch // Avoid out of bounds
            }

            Collections.swap(currentTasks, fromPosition, toPosition)
            tasksRepository.saveTaskPositions(currentTasks) // Persist the new order

            withContext(Dispatchers.Main) {
                _tasks.value = currentTasks // Update LiveData immediately
            }
        }
    }

    fun deleteAllDoneTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.deleteAllDoneTasksFromCurrentList()
            loadTasksForCurrentList()
            _snackbarEvent.postValue("Tâches terminées supprimées ✔")
        }
    }

    // --- File Export/Import Operations (Existing) ---

    fun exportTaskListToFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure tasksRepository.getTasksForList returns the list of tasks, not just the name
                val tasksToExport = tasksRepository.getTasksForList(tasksRepository.getCurrentListName())
                tasksRepository.writeTaskListTofile(tasksToExport, uri)
                _snackbarEvent.postValue("Fichier sauvegardé avec succès !")
            } catch (e: Exception) {
                Timber.e(e, "Error exporting task list to file.")
                _snackbarEvent.postValue("Erreur lors de l'exportation du fichier.")
            }
        }
    }

    fun importTaskListFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonContent = tasksRepository.readTextContent(uri)
                val parsedTasks = Json.decodeFromString<ArrayList<Task>>(jsonContent)
                val newListName = "Importée (${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())})"

                // Use the new method to create and switch to the list
                createAndSwitchToList(newListName, parsedTasks)

                _snackbarEvent.postValue("Liste importée avec succès")
            } catch (e: Exception) {
                Timber.e(e, "Error importing task list from file.")
                _snackbarEvent.postValue("Erreur lors de l'importation du fichier.")
            }
        }
    }

    // --- List Management Operations (NEW/Modified) ---

    /**
     * Loads all user-defined lists from the repository and updates _allLists LiveData.
     */
    fun loadAllUserLists() {
        viewModelScope.launch(Dispatchers.IO) {
            val listsMap = tasksRepository.getAllLists() // This returns MutableMap<String, String> (ID to Title)

            // Convert map to a list of SerialListObject
            val currentListId = tasksRepository.getCurrentListName() // Get current list ID

            val serialListObjects = listsMap.map { entry ->
                SerialListObject(
                    id = entry.key,
                    title = entry.value,
                    position = tasksRepository.getListPosition(entry.key), // Get position from repository
                    isCurrentSelected = (entry.key == currentListId) // Set selection status
                )
            }.sortedBy { it.position }.toMutableList() // Sort by position to maintain order

            withContext(Dispatchers.Main) {
                _allLists.value = serialListObjects
            }
        }
    }

    /**
     * Changes the currently active list.
     * @param newSelectedListId The ID of the list to switch to.
     */
    fun changeList(newSelectedListId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.setCurrentListName(newSelectedListId) // Update current list in repo
            loadTasksForCurrentList() // Load tasks for the new list
            loadAllUserLists() // Reload all lists to update selection state in UI
        }
    }

    /**
     * Creates a new list and optionally adds tasks to it, then switches to it.
     * @param listTitle The title of the new list.
     * @param tasksToAdd Optional list of tasks to add to the new list.
     * @param selectAfterCreation True if the app should switch to this new list after creation.
     */
    fun createNewList(listTitle: String, tasksToAdd: List<Task> = emptyList(), selectAfterCreation: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val newListId = tasksRepository.createList(listTitle) // This should return the ID of the new list
            if (tasksToAdd.isNotEmpty()) {
                tasksRepository.addTasksToList(newListId, tasksToAdd)
            }
            if (selectAfterCreation) {
                tasksRepository.setCurrentListName(newListId)
                loadTasksForCurrentList() // Load tasks for the new list
            }
            loadAllUserLists() // Always refresh all lists to show the new one
            _snackbarEvent.postValue("Liste '$listTitle' créée.")
        }
    }

    /**
     * Renames an existing list.
     * @param listId The ID of the list to rename.
     * @param newName The new title for the list.
     */
    fun renameList(listId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tasksRepository.renameList(newName, listId) // Delegate to TasksRepository
                loadAllUserLists() // Refresh all lists to show the updated name
                // If the current list was renamed, update its displayed name
                if (tasksRepository.getCurrentListName() == listId) {
                    withContext(Dispatchers.Main) {
                        _currentListName.value = newName
                    }
                }
                _snackbarEvent.postValue("Liste renommée en '$newName'.")
            } catch (e: Exception) {
                Timber.e(e, "Error renaming list: $listId to $newName")
                _snackbarEvent.postValue("Erreur lors du renommage de la liste.")
            }
        }
    }

    /**
     * Handles the reordering of lists in the UI (drag-and-drop).
     * @param fromPosition The original position of the moved list.
     * @param toPosition The new position of the moved list.
     */
    fun reorderLists(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentListsMutable = _allLists.value?.toMutableList() ?: return@launch
            if (fromPosition < 0 || fromPosition >= currentListsMutable.size ||
                toPosition < 0 || toPosition >= currentListsMutable.size) {
                return@launch // Avoid out of bounds
            }

            Collections.swap(currentListsMutable, fromPosition, toPosition)

            // Update the positions in the repository for persistence
            // This method needs to be added to TasksRepository.
            // It will iterate through the reordered list and save each list's new position.
            tasksRepository.saveListPositions(currentListsMutable)

            withContext(Dispatchers.Main) {
                _allLists.value = currentListsMutable // Update LiveData immediately
                // No Snackbar needed for just a drag-and-drop.
            }
        }
    }

    /**
     * Deletes a list.
     * @param listId The ID of the list to delete.
     */
    fun deleteList(listId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tasksRepository.deleteList(listId) // Add this method to TasksRepository
                // If the deleted list was the current one, switch to default or another list
                if (tasksRepository.getCurrentListName() == listId) {
                    tasksRepository.setCurrentListName("defaultList") // Or a better fallback
                }
                loadTasksForCurrentList() // Reload tasks for the potentially new current list
                loadAllUserLists() // Refresh all lists to remove the deleted one from UI
                _snackbarEvent.postValue("Liste supprimée.")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting list: $listId")
                _snackbarEvent.postValue("Erreur lors de la suppression de la liste.")
            }
        }
    }

    /**
     * Utility function to create a list and switch to it. Used by import.
     */
    private suspend fun createAndSwitchToList(listTitle: String, tasksToAdd: List<Task>) {
        val newListId = tasksRepository.createList(listTitle)
        tasksRepository.addTasksToList(newListId, tasksToAdd)
        tasksRepository.setCurrentListName(newListId)
        loadTasksForCurrentList()
        loadAllUserLists() // Update all lists to reflect new selection and addition
    }

    // --- Helper Methods (Potentially redundant or to be moved) ---

    // getCurrentListNameForUI() and getAllUserListsForUI() might become redundant
    // if you primarily use LiveData. You might keep them for very specific
    // cases where you need a synchronous value, but generally, observe LiveData.
    fun getCurrentListNameForUI(): String {
        return tasksRepository.getListTitle(tasksRepository.getCurrentListName())
            ?: "Current List"
    }
}