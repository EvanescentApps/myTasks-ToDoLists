package com.electro.todolist.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.electro.todolist.data.Priority
import com.electro.todolist.data.Task
import com.electro.todolist.data.TasksRepository
import com.google.android.material.chip.Chip
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

    private val tasksActivity = activity as TasksActivity

    val logEnabled = false

    fun moveItem(from: Int, to: Int) {

        if (logEnabled) {
            val titlesBefore = arrayListOf<String>()
            mTasks.forEach { titlesBefore.add(it.title) }
            Log.d("Adapter position", "from: $from to: $to")
            Log.i("b4 mTs", Json.encodeToString(titlesBefore))
        }

        tasksActivity.swapItems(from, to)
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

        Log.e("Activity", "Remove item at $position, size after removed : ${mTasks.size}")

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

                        tasksActivity.scrollToTask(position)


                        notifyItemRangeChanged(position, newPosition + 1)
                    }.show()

            }, 100 // value in milliseconds
        )
    }

    fun deleteItemOnAdapter(position: Int) {
        tasksActivity.deleteItem(position)
        mTasks.removeAt(position)

        //notifyItemRemoved(position)
        //notifyItemRangeChanged(position,mTasks.size)
    }

    fun onSwipe(viewHolder: RecyclerView.ViewHolder, side: Int) {

        if (side == ItemTouchHelper.LEFT) { //<<
            Log.i("Swipe","DELETE")

            tasksActivity.deleteItem(viewHolder.bindingAdapterPosition)
            //mTasks.removeAt(viewHolder.bindingAdapterPosition)

            //notifyItemRangeChanged(position,mTasks.size)
        } else if (side == ItemTouchHelper.RIGHT) { // >>

            Log.i("Swipe","Swipe SET DONE, Checked item ${viewHolder.bindingAdapterPosition}")

            val task: Task = mTasks[viewHolder.bindingAdapterPosition]
            task.done = true

            onCheck(viewHolder.bindingAdapterPosition)
            tasksActivity.setTaskDone(task, true)
        }
    }

    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {

        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val dateChip : Chip = itemView.findViewById(R.id.date_chip)
        val priorityChip : Chip = itemView.findViewById(R.id.priority_chip)
        val durationChip : Chip = itemView.findViewById(R.id.duration_chip)
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

        val task: Task = mTasks[viewHolder.bindingAdapterPosition]
        titleTasktextView2.text = task.title //set title to item
        //descriptionTaskTextView2.text = task.description
        descriptionTaskTextView2.visibility = View.GONE
        if (task.description.isNullOrBlank()) {
            descriptionTaskTextView2.visibility = View.GONE
        } else {
            descriptionTaskTextView2.text = task.description
            descriptionTaskTextView2.visibility = View.VISIBLE
        }

        checkbox.isChecked = task.done

        val timestamp = task.date
        if (timestamp != null) {
            viewHolder.dateChip.visibility = View.VISIBLE

            val dateText = TasksRepository.getInstance(tasksActivity).timestampToDate(timestamp)

            if ( !dateText.isNullOrBlank())
                viewHolder.dateChip.text = dateText
            else
                viewHolder.dateChip.visibility = View.GONE

        } else viewHolder.dateChip.visibility = View.GONE

        val priority = task.priority
        if (priority != Priority.NONE) {
            viewHolder.priorityChip.apply {
                visibility = View.VISIBLE
                text = priority.first
                setTextColor(resources.getColor(priority.second))
            }
        } else viewHolder.priorityChip.visibility = View.GONE

        val durationTimestamp = task.duration
        if (durationTimestamp != null) {
            viewHolder.durationChip.visibility = View.VISIBLE

            val durationText = TasksRepository.getInstance(tasksActivity).timestampToDuration(durationTimestamp)

            if ( !durationText.isNullOrBlank()) viewHolder.durationChip.text = durationText
            else viewHolder.durationChip.visibility = View.GONE

        } else viewHolder.durationChip.visibility = View.GONE

        checkbox.setOnClickListener {

            task.done = checkbox.isChecked
            Log.i("task", "Checked item ${viewHolder.bindingAdapterPosition}")
            if (checkbox.isChecked) {

                onCheck(viewHolder.bindingAdapterPosition)
                //(context as ScrollingActivity).removeItem(viewHolder.adapterPosition) ///////*****
            }
            tasksActivity.setTaskDone(task, checkbox.isChecked)
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

            Log.i("Item clicked", taskJson)

            intent.putExtra("currentTask", taskJson)
            intent.putExtra("position", viewHolder.bindingAdapterPosition)

            tasksActivity.startActivityForResult(intent, 500)
            tasksActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    override fun getItemCount(): Int {
        return mTasks.size
    }

    fun setSwipeRefresh(b: Boolean = true) {
        tasksActivity.setSwipeRefreshEnabled(b)
    }

}