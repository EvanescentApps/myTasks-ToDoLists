/*
 * Copyright (c) 2026. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.data.db

import androidx.room.*
import com.evanescent.mytasks.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY done ASC, position ASC, id ASC")
    fun getTasksForList(listId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY done ASC, position ASC, id ASC")
    suspend fun getTasksForListOnce(listId: String): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Transaction
    suspend fun updateTasksOrder(tasks: List<Task>) {
        tasks.forEach { updateTask(it) }
    }

    @Query("UPDATE tasks SET position = position + 1 WHERE listId = :listId")
    suspend fun shiftPositions(listId: String)

    @Transaction
    suspend fun insertTaskAtTop(task: Task) {
        shiftPositions(task.listId)
        insertTask(task.copy(position = 0))
    }

    @Query("DELETE FROM tasks WHERE listId = :listId AND done = 1")
    suspend fun deleteAllDoneTasks(listId: String)
}


