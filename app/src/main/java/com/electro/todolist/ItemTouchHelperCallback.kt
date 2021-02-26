package com.electro.todolist

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

interface ItemTouchHelperAdapter {
    /**
     * @param fromPosition starting position
     * @param toPosition The location of the move
     */
    fun onMove(fromPosition:Int, toPosition:Int)
    fun onSwipe(position:Int)
}

class ItemTouchHelperCallback: ItemTouchHelper.Callback {
    private val mAdapter:TasksAdapter

    private var longPressDragEnabled = false
    private var itemViewSwipeEnabled = false


    constructor(adapter:TasksAdapter) {
        mAdapter = adapter
    }
    constructor(adapter:TasksAdapter, canDrag:Boolean, canSwipe:Boolean) {
        mAdapter = adapter
        this.longPressDragEnabled = canDrag
        this.itemViewSwipeEnabled = canSwipe
    }
    /**
     * Can the settings be dragged and dropped?
     *
     * @param canDrag is true, no false
     */
    fun setDragEnable(canDrag:Boolean) {
        longPressDragEnabled = canDrag
    }
    /**
     * Set whether it can be swiped
     *
     * @param canSwipe is true, no false
     */
    fun setSwipeEnable(canSwipe:Boolean) {
        itemViewSwipeEnabled = canSwipe
    }
    /**
     * When the user drags or slides the Item, we need to tell the system to slide or drag the direction
     * @param recyclerView
     * @param viewHolder
     * @return
     */
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder:RecyclerView.ViewHolder):Int {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager)
        {// GridLayoutManager
            // flag If the value is 0, it is equivalent to this function being turned off.
            val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            // create make
            return makeMovementFlags(dragFlag, swipeFlag)
        }
        else if (layoutManager is LinearLayoutManager) {// linearLayoutManager
            val orientation = layoutManager.orientation
            var dragFlag = 0
            var swipeFlag = 0
            // For the sake of easy understanding, it is equivalent to a horizontal ListView and a vertical ListView.
            if(orientation == LinearLayoutManager.HORIZONTAL)
            run {// If it is a horizontal layout
                swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            }
            if (orientation == LinearLayoutManager.VERTICAL)
            {// If it is a vertical layout, equivalent to ListView
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.RIGHT // or ItemTouchHelper.LEFT
            }
            return makeMovementFlags(dragFlag, swipeFlag)
        }
        return 0
    }

    override fun onMove(recyclerView:RecyclerView, viewHolder:RecyclerView.ViewHolder, target:RecyclerView.ViewHolder):Boolean {
        //mAdapter.onMove(viewHolder.adapterPosition, target.adapterPosition)

        val adapter = recyclerView.adapter as TasksAdapter
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        adapter.moveItem(from, to)

        //adapter.notifyItemMoved(from, to)

        return true
    }
    override fun onSwiped(viewHolder:RecyclerView.ViewHolder, i:Int) {
        mAdapter.onSwipe(viewHolder.adapterPosition)
    }
}