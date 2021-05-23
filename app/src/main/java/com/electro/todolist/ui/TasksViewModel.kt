package com.electro.todolist.ui

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import com.electro.todolist.data.TasksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class TasksViewModel(repository: TasksRepository,
                     private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // USE FLOW AS LIVE DATA

   /* val tasksLiveFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e("TAG", "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->

            // Get our show completed value, defaulting to false if not set:
            //val showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED] ?: false
            //Preferences(showCompleted, sortOrder)
        }*/

}