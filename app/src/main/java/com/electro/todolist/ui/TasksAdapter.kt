package com.electro.todolist.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.electro.todolist.data.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class TasksAdapter(
    val mTasks: ArrayList<Task>,
    private val context: Context,
    private val activity : Activity
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    private var mContext: Context = context

    /*fun reorderEach() {
        // Set position to mTasks
        // forEach, take list position and pass it to the last param

        // for item in range length list, mTasks[item].position = item
        mTasks.forEachIndexed { index, task ->
            task.position = index
        }

        // Pass mTasks to main activity
    }*/

    val logEnabled = false

    fun moveItem(from: Int, to: Int) {

        if (logEnabled) {
            val titlesBefore = arrayListOf<String>()
            mTasks.forEach { titlesBefore.add(it.title) }
            Log.d("Adapter position", "from: $from to: $to")
            Log.i("b4 mTs", Json.encodeToString(titlesBefore))
        }

        //Collections.swap(mTasks, from, to)
        (activity as TasksActivity).swapItems(from, to)
        notifyItemMoved(from, to)

        if (logEnabled) {
            val titlesOnly = arrayListOf<String>()
            mTasks.forEach { titlesOnly.add(it.title) }
            Log.i("mTasks", Json.encodeToString(titlesOnly))
        }
    }

    private fun onCheck(position: Int) {

        val taskDone = mTasks[position] // We get the task
        taskDone.done = true
        //val taskToJson = Json.encodeToString(taskDone) // Converted to Json for storage
        //listEdit.putString(taskDone.creationDate.toString(), taskToJson).apply()


        Log.e("Activity", "Remove item at $position")
        Log.e("tasks (activity)", "size after removed : ${mTasks.size}")

        Handler(Looper.getMainLooper()).postDelayed(
            {
                val newPosition = mTasks.size - 1
                mTasks.removeAt(position)
                mTasks.add(mTasks.size, taskDone)
                //notifyItemMoved(position,newPosition)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, newPosition + 1)

                Snackbar.make(
                    (context as TasksActivity).findViewById(R.id.activity),
                    "Tâche terminée",
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(context.findViewById<FloatingActionButton>(R.id.fab))
                    .setAction("Annuler") {
                        Log.e("Action", "Suppression annulée")
                        taskDone.done = false
                        //listEdit.putString(taskDone.creationDate.toString(), Gson().toJson(taskDone)).apply()

                        mTasks.removeAt(newPosition)
                        mTasks.add(position, taskDone)

                        notifyItemMoved(newPosition, position)

                        notifyItemRangeChanged(position, newPosition + 1)
                    }.show()

            }, 100 // value in milliseconds
        )
    }

    fun onSwipe(position: Int) {

        (activity as TasksActivity).deleteItem(position)
        mTasks.removeAt(position)

        notifyItemRemoved(position)
        //notifyItemRangeChanged(position,mTasks.size)
    }

    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {

        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val taskView = inflater.inflate(R.layout.item_task, parent, false)

        return ViewHolder(taskView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val titleTasktextView2 = viewHolder.titleTextView
        val checkbox = viewHolder.checkbox
        val descriptionTaskTextView2 = viewHolder.descriptionTextView

        viewHolder.itemView.setOnClickListener {
            Log.i(
                "Adapter",
                "Clicked item ${viewHolder.bindingAdapterPosition}"
            )
        }

        val task: Task = mTasks[viewHolder.bindingAdapterPosition]
        titleTasktextView2.text = task.title //set title to item
        //descriptionTaskTextView2.text = task.description
        if (task.description.isNullOrBlank()) {
            descriptionTaskTextView2.visibility = View.GONE
            Log.i("Description", "hidden for item ${viewHolder.bindingAdapterPosition}")
        } else {
            descriptionTaskTextView2.visibility = View.VISIBLE
            descriptionTaskTextView2.text = task.description
        }

        checkbox.isChecked = task.done

        /*if (checkbox.isChecked) {
            mTasks.removeAt(position)
            mTasks.add(mTasks.size,task)
            notifyItemMoved(position,mTasks.size-1)
            notifyItemRangeChanged(position, mTasks.size)
        }*/

        checkbox.setOnClickListener {

            task.done = checkbox.isChecked
            Log.i("Checked", "Checked item ${viewHolder.bindingAdapterPosition}")
            if (checkbox.isChecked) {

                onCheck(viewHolder.bindingAdapterPosition)
                //(context as ScrollingActivity).removeItem(viewHolder.adapterPosition) ///////*****
            }
            (context as TasksActivity).setTaskDone(task, checkbox.isChecked)
        }

        // Set onclick listener for the whole item, to show details fragment
        viewHolder.itemView.setOnClickListener {
            val item = mTasks[viewHolder.bindingAdapterPosition]
            //Toast.makeText(context,"Item Cicked with id ${item.uid}",Toast.LENGTH_SHORT).show()

            // Here gather data and encode it to JSON with kotlinx serialization
            // Push it to a bundle
            val intent = Intent(mContext, TaskDetailsActivity::class.java)

            var taskJson: String
            //Json.encodeToString
            taskJson = Json.encodeToString(item)

            Log.i("SERIALIZATION", taskJson)

            intent.putExtra("currentTask", taskJson)
            intent.putExtra("position", viewHolder.bindingAdapterPosition)

            (mContext as Activity).startActivityForResult(intent, 500)
            (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    override fun getItemCount(): Int {
        return mTasks.size
    }

}