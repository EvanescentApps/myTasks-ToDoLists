package com.electro.todolist

import kotlin.collections.ArrayList

class Task(
    val title: String,
    val description: String? = null,
    val creationDate: Long,
    var done: Boolean
) {

    companion object {

        fun createTasksList(listSaved: ArrayList<Task>? = null) :ArrayList<Task> {
            val tasks = ArrayList<Task>()

            listSaved?.forEach {
                tasks.add(it)
            }

            if(listSaved?.size==0) {
                tasks.add(Task("Ajouter des tâches", "Cliquez sur le + en bas", System.currentTimeMillis(), false))
            }

            return tasks // Returned to tasks var in ScrollingActivity
        }

    }

}