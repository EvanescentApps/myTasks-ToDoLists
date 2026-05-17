package com.evanescent.mytasks.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evanescent.mytasks.R
import com.evanescent.mytasks.data.model.SerialListObject
import com.evanescent.mytasks.data.model.Task
import com.evanescent.mytasks.data.model.TaskList
import com.evanescent.mytasks.data.repository.TasksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    application: Application,
    private val tasksRepository: TasksRepository
) : AndroidViewModel(application) {

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }

    // --- Flows for Tasks and UI State ---
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // Exposer le compteur à l'Activity via le ViewModel
    val counter: StateFlow<Int> = tasksRepository.counterFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // L'action est déclenchée depuis le ViewModel
    fun onAddTaskClicked() { // Renommez selon l'action
        viewModelScope.launch {
            tasksRepository.incrementCounter()
        }
    }


    private val _isEmptyStateEnabled = MutableStateFlow(false)
    val isEmptyStateEnabled: StateFlow<Boolean> = _isEmptyStateEnabled.asStateFlow()

    private val _currentListName = MutableStateFlow("")
    val currentListName: StateFlow<String> = _currentListName.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // --- Flows for ALL Lists ---
    private val _allLists = MutableStateFlow<List<SerialListObject>>(emptyList())
    val allLists: StateFlow<List<SerialListObject>> = _allLists.asStateFlow()

    private val _currentListId = MutableStateFlow<String?>(null)
    val currentListId: StateFlow<String?> = _currentListId.asStateFlow() // Expose the ID

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
            val listTitle = tasksRepository.getListTitle(currentListId) ?: "Mes tâches"

            withContext(Dispatchers.Main) {
                _tasks.value = loadedTasks
                _isEmptyStateEnabled.value = loadedTasks.isEmpty()
                // Update currentListName based on the repository's current list title
                _currentListName.value = listTitle
                _currentListId.value = currentListId

            }
        }
    }

    fun updateTasksOrder(newOrderedList: List<Task>) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.saveTaskPositions(newOrderedList)
        }
    }

    fun updateTask(originalTask: Task, updatedTask: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.updateTask(originalTask, updatedTask)
            loadTasksForCurrentList() // Reload tasks to reflect the update in the UI
/*
            _snackbarEvent.emit(getString(R.string.task_updated))
*/
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.addTaskAtTop(task)
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.task_added))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.deleteTask(task)
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.task_deleted))
        }
    }

    fun setTaskDone(task: Task, done: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.setTaskDone(task, done)
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.task_updated))
        }
    }

    fun restoreItem(task: Task, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.addTaskAtTop(task)
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.task_restored))
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = _tasks.value.toMutableList()
            if (fromPosition < 0 || fromPosition >= currentTasks.size ||
                toPosition < 0 || toPosition >= currentTasks.size) {
                return@launch // Avoid out of bounds
            }

            Collections.swap(currentTasks, fromPosition, toPosition)
            tasksRepository.saveTaskPositions(currentTasks) // Persist the new order

            withContext(Dispatchers.Main) {
                _tasks.value = currentTasks // Update StateFlow immediately
            }
        }
    }

    fun deleteAllDoneTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.deleteAllDoneTasksFromCurrentList()
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.done_tasks_deleted))
        }
    }

    // --- File Export/Import Operations (Existing) ---

    fun exportTaskListToFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure tasksRepository.getTasksForList returns the list of tasks, not just the name
                val tasksToExport = tasksRepository.getTasksForList(tasksRepository.getCurrentListName())
                tasksRepository.writeTaskListTofile(tasksToExport, uri)
                _snackbarEvent.emit(getString(R.string.file_saved_success))
            } catch (e: Exception) {
                Timber.e(e, "Error exporting task list to file.")
                _snackbarEvent.emit(getString(R.string.error_export_file))
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

                _snackbarEvent.emit(getString(R.string.file_imported_success))
            } catch (e: Exception) {
                Timber.e(e, "Error importing task list from file.")
                _snackbarEvent.emit(getString(R.string.error_import_file))
            }
        }
    }

    // --- List Management Operations (NEW/Modified) ---

    /**
     * Loads all user-defined lists from the repository and updates _allLists Flow.
     */
    fun loadAllUserLists() {
        viewModelScope.launch(Dispatchers.IO) {
            val allLists = tasksRepository.getAllListsObjects()
            val currentListId = tasksRepository.getCurrentListName()

            val serialListObjects = allLists.map { list ->
                SerialListObject(
                    id = list.id,
                    title = list.title,
                    position = list.position,
                    isCurrentSelected = (list.id == currentListId)
                )
            }.toMutableList()

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
            _snackbarEvent.emit(getString(R.string.list_created, listTitle))
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
                    _currentListName.value = newName
                }
                _snackbarEvent.emit(getString(R.string.list_renamed, newName))
            } catch (e: Exception) {
                Timber.e(e, "Error renaming list: $listId to $newName")
                _snackbarEvent.emit(getString(R.string.error_rename_list))
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
            val currentListsMutable = _allLists.value.toMutableList()
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
                _allLists.value = currentListsMutable // Update StateFlow immediately
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
                _snackbarEvent.emit(getString(R.string.list_deleted))
            } catch (e: Exception) {
                Timber.e(e, "Error deleting list: $listId")
                _snackbarEvent.emit(getString(R.string.error_delete_list))
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

    /**
     * Starts the demonstration mode by adding sample tasks to the current list.
     */
    fun startDemoMode() {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepository.createDemoData()
            loadTasksForCurrentList()
            _snackbarEvent.emit(getString(R.string.demo_mode_activated))
        }
    }

    // --- Helper Methods (Potentially redundant or to be moved) ---

    // getCurrentListNameForUI() and getAllUserListsForUI() might become redundant
    // if you primarily use Flows. You might keep them for very specific
    // cases where you need a synchronous value, but generally, observe Flows.
    fun getCurrentListNameForUI(): String {
        return _currentListName.value
    }
}

