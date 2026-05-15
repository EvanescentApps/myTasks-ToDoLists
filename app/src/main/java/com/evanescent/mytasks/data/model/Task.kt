/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package com.evanescent.mytasks.data.model

import androidx.annotation.Keep
import com.evanescent.mytasks.R
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Keep
@Serializable
class Priority {
    companion object {
        val VERY_HIGH = Pair("Très important", R.color.priority_very_important)
        val HIGH = Pair("Important", R.color.priority_important)
        val URGENT = Pair("Très urgent !", R.color.priority_urgent)
        val FACULTATIVE = Pair("Facultatif", R.color.priority_optional)
        val NOT_URGENT = Pair("Pas urgent", R.color.priority_not_urgent)
        val NONE = Pair("Indéfini", R.color.textContent)
    }
}

@Keep
@Serializable
@Entity(tableName = "tasks")
data class Task(
    var title: String,
    var description: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    var done: Boolean = false,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: String = "list1",
    var position: Int = 0,
    var priorityName: String = Priority.NONE.first,
    var priorityColor: Int = Priority.NONE.second,
    var date: Long? = null,
    var duration : Long? = null
) {
    // Helper to get priority as a Pair (for legacy support in UI)
    val priority: Pair<String, Int>
        get() = Pair(priorityName, priorityColor)

    fun setPriority(pair: Pair<String, Int>) {
        priorityName = pair.first
        priorityColor = pair.second
    }
    companion object {
        private const val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        @Deprecated("This method leads to issues (tasks not saved)")
        fun createTasksList(listSaved: ArrayList<Task>? = null) :ArrayList<Task> {

            var tasks = ArrayList<Task>()

            listSaved?.let { tasks = it }

            /*if(listSaved.isNullOrEmpty()) {
                tasks.add(Task("Ajoutez des tâches", "Cliquez sur le + en bas", System.currentTimeMillis(), false, "1", 0))
                tasks.add(Task("Ajoutez une description", "Cliquez sur le bouton en bas à gauche lorsque vous créez une tâche", System.currentTimeMillis(), false, "2", 1))
                tasks.add(Task("Cochez cette tâche", "<< Cliquez sur la checkbox ici", System.currentTimeMillis(), false, "3", 3))
                tasks.add(Task("Supprimez cette tâche", ">> Faites glisser la tâche vers la droite >>", System.currentTimeMillis(), false, "4", 4))
                tasks.add(Task("Réorganisez des tâches", "Appuyez 1s dessus et faites là glisser !", System.currentTimeMillis(), false, "5", 5))
            }*/
            return tasks // Returned to tasks var in ScrollingActivity
        }

        fun emptyState(): ArrayList<Task> {

            return arrayListOf(
                Task(title = "Ajoutez des tâches", description = "Cliquez sur le + en bas", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 0),
                Task(title = "Ajoutez une description", description = "Cliquez sur le bouton Description lorsque vous créez une tâche", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 1),
                Task(title = "Cochez cette tâche", description = "<< Cochez la case ici", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 3),
                Task(title = "Modifiez cette tâche", description = "Cliquez dessus !", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 4),
                Task(title = "Supprimez cette tâche", description = "<< Faites glisser la tâche vers la gauche <<", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 5),
                Task(title = "Démarrez un Flow !", description = "Cliquez sur la tâche puis sur Démarrer un Flow !", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 6),
                Task(title = "Réorganisez des tâches", description = "Appuyez 1s dessus et faites là glisser !", creationDate = generateLongId(), done = false, id = 0, listId = generateId(5), position = 7)
            )
        }

        fun generateId(nbChars: Int = 5): String {
            var id = ""

            for (i in 1..nbChars) {
                id+= alphabet[Random.nextInt(0, alphabet.length - 1)]
            }
            return id
        }

        fun generateLongId() : Long {
            return Random.nextLong(1000000,1000000000)
        }
    }
}

