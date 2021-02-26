package com.electro.todolist

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.util.*

class TasksAdapter(private val mTasks: ArrayList<Task>, private val context: Context) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    private lateinit var mContext: Context

    fun moveItem(from: Int, to: Int) {

        val titlesOnlyBefore= arrayListOf<String>()

        mTasks.forEach { titlesOnlyBefore.add(it.title) }


        Log.d("Adapter position","from: $from to: $to")
        Log.i("b4 mTs", Gson().toJson(titlesOnlyBefore))
        //-----------------------------------------------

        //Collections.swap(mTasks, from, to)
        (context as ScrollingActivity).swapItems(from, to)
        notifyItemMoved(from, to)

        //-----------------------------------------------
        val titlesOnly= arrayListOf<String>()
        mTasks.forEach { titlesOnly.add(it.title) }

        Log.i("mTasks", Gson().toJson(titlesOnly))

        //notifyItemRangeChanged(from,mTasks.size)
    }

    private fun onCheck(position: Int) {

        val taskDone = mTasks[position] // We get the task
        taskDone.done = true
        val taskToJson = Gson().toJson(taskDone) // Converted to Json for storage
        //listEdit.putString(taskDone.creationDate.toString(), taskToJson).apply()

        Log.e("Activity", "Remove item at $position")
        Log.e("tasks (activity)", "size after removed : ${mTasks.size}")

        Handler(Looper.getMainLooper()).postDelayed({
            val newPosition = mTasks.size-1
            mTasks.removeAt(position)
            mTasks.add(mTasks.size,taskDone)
            notifyItemMoved(position,newPosition)
            notifyItemRangeChanged(position, newPosition+1)

            Snackbar.make((context as ScrollingActivity).findViewById(R.id.activity), "Tâche terminée", Snackbar.LENGTH_LONG)
                    .setAnchorView(context.findViewById<FloatingActionButton>(R.id.fab))
                    .setAction("Annuler") {
                        Log.e("Action", "Suppression annulée")
                        taskDone.done = false
                        //listEdit.putString(taskDone.creationDate.toString(), Gson().toJson(taskDone)).apply()

                        mTasks.removeAt(newPosition)
                        mTasks.add(position,taskDone)

                        notifyItemMoved(newPosition,position)

                        notifyItemRangeChanged(position,newPosition+1)
                    }.show()

        }, 200 // value in milliseconds
        )
    }

    fun onSwipe(position: Int) {

        (context as ScrollingActivity).deleteItem(position)
        mTasks.removeAt(position)

        notifyItemRemoved(position)
        notifyItemRangeChanged(position,mTasks.size)
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

    override fun onBindViewHolder(viewHolder: TasksAdapter.ViewHolder, position: Int) {

        val titleTasktextView2 = viewHolder.titleTextView
        val checkbox = viewHolder.checkbox
        val descriptionTaskTextView2 = viewHolder.descriptionTextView

        viewHolder.itemView.setOnClickListener { Log.e("Adapter","Clicked item ${viewHolder.adapterPosition}") }

        val task : Task = mTasks[viewHolder.adapterPosition]
        titleTasktextView2.text = task.title //set title to item
        //descriptionTaskTextView2.text = task.description
        if( !(task.description.isNullOrBlank()) ) { descriptionTaskTextView2.text = task.description }
        else {
            descriptionTaskTextView2.visibility = View.GONE
            Log.e("VISIBILITY","description hidden for item ${viewHolder.adapterPosition}")
        }


        checkbox.isChecked = task.done

        checkbox.setOnClickListener {

            task.done = checkbox.isChecked
            Log.e("Checked", "Checked item ${viewHolder.adapterPosition}")
            if (checkbox.isChecked){

                onCheck(viewHolder.adapterPosition)
                //(context as ScrollingActivity).removeItem(viewHolder.adapterPosition) ///////*****
            }
            (context as ScrollingActivity).setTaskDone(task, checkbox.isChecked)
        }
    }

    override fun getItemCount(): Int {
        return mTasks.size
    }

}