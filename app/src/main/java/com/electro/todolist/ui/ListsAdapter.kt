package com.electro.todolist.ui

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates

class ListsAdapter(
    private val mLists: MutableList<ListObject>,
    private val activity: Activity,
    private val caller: BottomSheetDialogFragment
) : RecyclerView.Adapter<ListsAdapter.ViewHolderList>() {

    private var currentListSelected by Delegates.notNull<Int>()

    private val logEnabled = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderList {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val taskView = inflater.inflate(R.layout.list_item, parent, false)

        return ViewHolderList(taskView)
    }

    override fun onBindViewHolder(holder: ViewHolderList, adapterPosition: Int) {
        holder.titleText.text = mLists[holder.bindingAdapterPosition].title


        if (mLists[holder.bindingAdapterPosition].isCurrentSelected) {
            holder.linearLayout.setBackgroundResource(R.drawable.list_item_bg_selected)
            currentListSelected = holder.bindingAdapterPosition
        }

        holder.itemView.setOnClickListener {
            if (activity is TasksActivity) {
                activity.changeList(mLists[holder.bindingAdapterPosition].id)
            } else if (activity is TaskDetailsActivity) {
                // TODO : SWITCH TASK TO SELECTED LIST
                Log.i("Switch","Switching task to selected list : ${mLists[holder.bindingAdapterPosition].title}")
            }
            caller.dismissAllowingStateLoss()
        }
    }

    override fun getItemCount(): Int = mLists.size

    fun moveItem(from: Int, to: Int) {

        if (logEnabled) {
            val titlesBefore = arrayListOf<String>()
            mLists.forEach { titlesBefore.add(it.title) }
            Log.d("Adapter position", "from: $from to: $to")
            Log.i("b4 mTs", Json.encodeToString(titlesBefore))
        }


        // TODO : MOVE LIST
        notifyItemMoved(from, to)

        if (logEnabled) {
            val titlesOnly = arrayListOf<String>()
            mLists.forEach { titlesOnly.add(it.title) }
            Log.i("mTasks", Json.encodeToString(titlesOnly))
        }
    }

    fun deleteItemOnAdapter(position: Int) {
        //tasksActivity.deleteItem(position)

        // TODO : DELETE ITEM
        mLists.removeAt(position)

        //notifyItemRemoved(position)
        //notifyItemRangeChanged(position,mTasks.size)
    }

    /*fun onSwipe(viewHolder: RecyclerView.ViewHolder, side: Int) {

        if (side == ItemTouchHelper.LEFT) { //<<
            Log.i("Swipe","DELETE")
            tasksActivity.deleteItem(viewHolder.bindingAdapterPosition)
            //mTasks.removeAt(viewHolder.bindingAdapterPosition)

            //notifyItemRangeChanged(position,mTasks.size)
        } else if (side == ItemTouchHelper.RIGHT) { // >>

            Log.i("Swipe","Swipe SET DONE, Checked item ${viewHolder.bindingAdapterPosition}")

            val task: ListObject = mLists[viewHolder.bindingAdapterPosition]
            task.done = true

            tasksActivity.setTaskDone(task, true)
        }
    }*/

    inner class ViewHolderList(listItemView: View) : RecyclerView.ViewHolder(listItemView) {

        val linearLayout: LinearLayout = itemView.findViewById(R.id.bottomSheetParent)
        val titleText: TextView = itemView.findViewById(R.id.text)
    }
}