/*
 * Copyright (c) 2026. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.data.db

import androidx.room.*
import com.evanescent.mytasks.data.model.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY position ASC")
    fun getAllListsFlow(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_lists ORDER BY position ASC")
    suspend fun getAllLists(): List<TaskList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<TaskList>)

    @Update
    suspend fun updateList(list: TaskList)

    @Delete
    suspend fun deleteList(list: TaskList)

    @Query("SELECT * FROM task_lists WHERE id = :listId")
    suspend fun getListById(listId: String): TaskList?

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)
}
