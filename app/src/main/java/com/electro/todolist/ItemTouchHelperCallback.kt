package com.electro.todolist

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ui.TasksAdapter


//import androidx.test.runner.intent.IntentStubberRegistry.reset


interface ItemTouchHelperAdapter {
    /**
     * @param fromPosition starting position
     * @param toPosition The location of the move
     */
    fun onMove(fromPosition: Int, toPosition: Int)
    fun onSwipe(position: Int)
}

class ItemTouchHelperCallback: ItemTouchHelper.Callback {
    private val mAdapter: TasksAdapter

    private var longPressDragEnabled = false
    private var itemViewSwipeEnabled = false
    lateinit var mContext: Context
    private lateinit var mClearPaint: Paint
    lateinit var mBackground: ColorDrawable
    private var backgroundColor = 0
    private lateinit var deleteDrawable: Drawable
    private var intrinsicWidth = 0
    private var intrinsicHeight = 0
    private var cardPicked = true
    private var reset = false

    constructor(adapter: TasksAdapter, context: Context) {
        mAdapter = adapter
        mContext = context
        mBackground = ColorDrawable()
        backgroundColor = Color.parseColor("#b80f0a")
        mClearPaint = Paint()
        mClearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        deleteDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_delete_outline_24
        )!! //ContextCompat.getDrawable(mContext, R.drawable.ic_delete)!!
        intrinsicWidth = deleteDrawable.intrinsicWidth
        intrinsicHeight = deleteDrawable.intrinsicHeight
    }
    constructor(adapter: TasksAdapter, canDrag: Boolean, canSwipe: Boolean) {
        mAdapter = adapter
        this.longPressDragEnabled = canDrag
        this.itemViewSwipeEnabled = canSwipe
    }
    /**
     * Can the settings be dragged and dropped?
     *
     * @param canDrag is true, no false
     */
    fun setDragEnable(canDrag: Boolean) {
        longPressDragEnabled = canDrag
    }
    /**
     * Set whether it can be swiped
     *
     * @param canSwipe is true, no false
     */
    fun setSwipeEnable(canSwipe: Boolean) {
        itemViewSwipeEnabled = canSwipe
    }
    /**
     * When the user drags or slides the Item, we need to tell the system to slide or drag the direction
     * @param recyclerView
     * @param viewHolder
     * @return
     */
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder):Int {
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

    override fun onMoved(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        fromPos: Int,
        target: RecyclerView.ViewHolder,
        toPos: Int,
        x: Int,
        y: Int
    ) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
        Log.i("MOVED", "Item moved and released")

    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ):Boolean {
        //mAdapter.onMove(viewHolder.adapterPosition, target.adapterPosition)

        val adapter = recyclerView.adapter as TasksAdapter
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        adapter.moveItem(from, to)

        //adapter.notifyItemMoved(from, to)

        return true
    }
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        mAdapter.onSwipe(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView: View = viewHolder.itemView
        val itemHeight: Int = itemView.height
        val isCancelled = dX == 0f && !isCurrentlyActive

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            if(actionState == ItemTouchHelper.ACTION_STATE_DRAG ) {
                val animator = viewHolder.itemView.animate()
                if (isCurrentlyActive) {
                    animator.translationZ(8f)
                    //animator.duration = 200
                    //animator.interpolator = AccelerateInterpolator()
                    //cardPicked = true
                } else {
                    //val animator = viewHolder.itemView.animate()
                    animator.translationZ(0f)
                    //animator.start()
                    //cardPicked = false
                    //reset = false
                }

                animator.duration = 200
                animator.interpolator = AccelerateInterpolator()
                animator.start()

            }

        }


        if (isCancelled) {
            //Log.e("End","Swipe finished or cancelled")
            clearCanvas(
                canvas,
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left + dX,
                itemView.bottom.toFloat()
            )
            super.onChildDraw(
                canvas,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
            return
        }

        //backgroundColor = Color.parseColor("#E65100")

        mBackground.color = backgroundColor
        mBackground.setBounds(
            itemView.left,
            itemView.top,
            itemView.left + dX.toInt(),
            itemView.bottom
        )
        mBackground.draw(canvas)

        if (dX>0) {

            val slidePercent = (dX / itemView.width) *100
            Log.e("Slide", "$slidePercent% $dX ${itemView.width}")

            deleteDrawable = if (dX>200) {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_delete_24)!!
            } else {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_delete_outline_24_white)!!
            }

            val deleteIconTop: Int = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin: Int = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft: Int = itemView.left + deleteIconMargin
            val deleteIconRight: Int = itemView.left + deleteIconMargin + intrinsicWidth
            val deleteIconBottom: Int = deleteIconTop + intrinsicHeight
            deleteDrawable.setBounds(
                deleteIconLeft,
                deleteIconTop,
                deleteIconRight,
                deleteIconBottom
            )

            deleteDrawable.draw(canvas)
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, mClearPaint)
        reset = true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // interaction is over, time to reset our elevation
        reset = true
    }

}