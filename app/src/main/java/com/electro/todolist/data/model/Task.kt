/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package com.electro.todolist.data.model

import androidx.annotation.Keep
import com.electro.todolist.R
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Keep
@Serializable
class Priority {
    companion object {

        // TODO : Store the strings as Ints
        val VERY_HIGH = Pair("Très important", R.color.taskTresImportant)
        val HIGH = Pair("Important", R.color.taskImportant)
        val URGENT = Pair("Très urgent !", R.color.taskUrgent)
        val FACULTATIVE = Pair("Facultatif", R.color.taskFacultative) // C8FFCD
        val NOT_URGENT = Pair("Pas urgent", R.color.taskPasUrgent) // B3FF74
        val NONE = Pair("Indéfini", R.color.textContent)
    }
}

@Keep
@Serializable
data class Task(
    var title: String,
    var description: String? = null,
    val creationDate: Long,
    var done: Boolean,
    val uid: String? = null,
    var position: Int = 0,
    var priority: Pair<String, Int> = Priority.NONE,
    var date: Long? = null,
    var duration : Long? = null
    // Timestamp lastEdit or lastSync ?
) {
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
                Task("Ajoutez des tâches", "Cliquez sur le + en bas", generateLongId(), false, generateId(5), 0),
                Task("Ajoutez une description", "Cliquez sur le bouton Description lorsque vous créez une tâche", generateLongId(), false, generateId(5), 1),
                Task("Cochez cette tâche", "<< Cochez la case ici", generateLongId(), false, generateId(5), 3),
                Task("Modifiez cette tâche","Cliquez dessus !", generateLongId(), false, generateId(5), 4),
                Task("Supprimez cette tâche", "<< Faites glisser la tâche vers la gauche <<", generateLongId(), false, generateId(5), 5),
                Task("Démarrez un Flow !", "Cliquez sur la tâche puis sur Démarrer un Flow !", generateLongId(), false, generateId(5), 6),
                Task("Réorganisez des tâches", "Appuyez 1s dessus et faites là glisser !", generateLongId(), false, generateId(5), 7)
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