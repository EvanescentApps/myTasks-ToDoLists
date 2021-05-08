package com.electro.todolist

import kotlin.collections.ArrayList
//import java.io.Serializable
import kotlinx.serialization.Serializable

@Serializable
class Task(
    var title: String,
    var description: String? = null,
    val creationDate: Long,
    var done: Boolean,
    val uid: String? = null,
    var position: Int = 0
    // Timestamp lastEdit or lastSync ?
) {

    companion object {

        fun createTasksList(listSaved: ArrayList<Task>? = null) :ArrayList<Task> {
            val tasks = ArrayList<Task>()

            listSaved?.forEach {
                tasks.add(it)
            }

           /* tasks.forEach {
                if (it.done) {
                    tasks.remove(it)
                    tasks.add(tasks.size-1, it)
                }
            }*/

            if(listSaved?.size==0) {
                tasks.add(Task("Ajoutez des tâches", "Cliquez sur le + en bas", System.currentTimeMillis(), false))
                tasks.add(Task("Ajoutez une description", "Cliquez sur le bouton en bas à gauche lorsque vous créez une tâche", System.currentTimeMillis(), false))
                tasks.add(Task("Cochez cette tâche", "<< Cliquez sur la checkbox ici", System.currentTimeMillis(), false))
                tasks.add(Task("Supprimez cette tâche", ">> Faites glisser la tâche vers la droite >>", System.currentTimeMillis(), false))
                tasks.add(Task("Réorganisez des tâches", "Appuyez 1s dessus et faites là glisser !", System.currentTimeMillis(), false))
            }

            return tasks // Returned to tasks var in ScrollingActivity
        }

    }

}