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
import com.electro.todolist.ui.ListsAdapter
import com.electro.todolist.ui.TasksAdapter
import kotlin.math.roundToInt

class ItemTouchHelperCallback//ContextCompat.getDrawable(mContext, R.drawable.ic_delete)!!
    (adapter: TasksAdapter, context: Context) : ItemTouchHelper.Callback() {
    private val mAdapter: TasksAdapter = adapter

    private var longPressDragEnabled = false
    private var itemViewSwipeEnabled = false
    var mContext: Context = context
    private var mClearPaint: Paint
    var mBackground: ColorDrawable = ColorDrawable()
    private var backgroundColor = 0
    private var deleteDrawable: Drawable
    private var doneDrawable: Drawable
    private var inWidth = 0
    private var inHeight = 0
    private var cardPicked = true
    private var reset = false

    init {
        backgroundColor = Color.parseColor("#b80f0a")
        mClearPaint = Paint()

        // KNOW WHAT IS THAT
        mClearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        deleteDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_delete_outline_24
        )!!

        inWidth = deleteDrawable.intrinsicWidth
        inHeight = deleteDrawable.intrinsicHeight

        doneDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_task_alt_24)!!
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
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {// GridLayoutManager
            // flag If the value is 0, it is equivalent to this function being turned off.
            val dragFlag =
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            // create make
            return makeMovementFlags(dragFlag, swipeFlag)
        } else if (layoutManager is LinearLayoutManager) {// linearLayoutManager
            val orientation = layoutManager.orientation
            var dragFlag = 0
            var swipeFlag = 0
            // For the sake of easy understanding, it is equivalent to a horizontal ListView and a vertical ListView.
            if (orientation == LinearLayoutManager.HORIZONTAL)
                run {// If it is a horizontal layout
                    swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                }
            if (orientation == LinearLayoutManager.VERTICAL) {// If it is a vertical layout, equivalent to ListView
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
            }
            return makeMovementFlags(dragFlag, swipeFlag)
        }
        return 0
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        val dragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG || actionState == ItemTouchHelper.ACTION_STATE_SWIPE

        if (false) {
            mAdapter.setSwipeRefresh(!dragging)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.20f
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
    ): Boolean {

        val adapter = recyclerView.adapter as TasksAdapter
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        adapter.moveItem(from, to)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {

        if (i == ItemTouchHelper.LEFT) { //<<
            Log.e("Swipe","Swipe LEFT")
        } else if (i == ItemTouchHelper.RIGHT) { // >>
            Log.e("Swipe","Swipe RIGHT")
        }

        mAdapter.onSwipe(viewHolder, i)
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

        //Log.e("dX","is $dX")

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val animator = viewHolder.itemView.animate()

            if (isCurrentlyActive) {
                animator.translationZ(16f)
                animator.withEndAction {
                    viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.backgroundSelected))
                }
                viewHolder.itemView.translationZ = 16f
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.backgroundSelected))
            } else {

                animator.translationZ(0f)

                animator.withEndAction {
                    viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background))
                }
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background))
            }

            animator.duration = 100
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        if (isCancelled) {

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

        if (dX > 0) { // SWIPE RIGHT

            mBackground.color = Color.parseColor("#4F5CA4")
            mBackground.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom
            )
            mBackground.draw(canvas)

            val slidePercent = ((dX / itemView.width) * 100).roundToInt()

            if ((slidePercent % 20) == 0 && slidePercent >= 30) {
                Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")
            }

            doneDrawable.setTint(Color.parseColor("#FFFFFF"))

            // ZoomIn and Out bubble anim

            // REMOVE THESE RISKY "!!"
            doneDrawable = if (slidePercent >= 10) {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_task_alt_24)!!
            } else {
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_baseline_task_alt_24
                )!!
            }

            val doneIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val doneIconMargin: Int = (itemHeight - inHeight) / 2
            val doneIconLeft: Int = itemView.left + doneIconMargin
            val doneIconRight: Int = itemView.left + doneIconMargin + inWidth
            val doneIconBottom: Int = doneIconTop + inHeight
            doneDrawable.setBounds(
                doneIconLeft,
                doneIconTop,
                doneIconRight,
                doneIconBottom
            )

            doneDrawable.draw(canvas)
        } else if (dX < 0) { // SWIPE <<

            mBackground.color = backgroundColor
            mBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            mBackground.draw(canvas)

            val slidePercent = ((-dX / itemView.width) * 100).roundToInt()

            Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")

            if ((slidePercent % 20) == 0 && slidePercent >= 30) {
                Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")
            }

            deleteDrawable.setTint(Color.parseColor("#FFFFFF"))

            deleteDrawable = if (slidePercent >= 10) {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_delete_24)!!
            } else {
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_baseline_delete_outline_24_white
                )!!
            }

            val deleteIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val deleteIconMargin: Int = (itemHeight - inHeight) / 2
            val deleteIconLeft: Int = itemView.right - deleteIconMargin - inWidth
            val deleteIconRight: Int = itemView.right - deleteIconMargin
            val deleteIconBottom: Int = deleteIconTop + inHeight
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




class ListsItemTouchCallback//ContextCompat.getDrawable(mContext, R.drawable.ic_delete)!!
    (adapter: ListsAdapter, context: Context) : ItemTouchHelper.Callback() {
    private val mAdapter = adapter

    private var longPressDragEnabled = false
    private var itemViewSwipeEnabled = false
    private var mContext: Context = context
    private var mClearPaint: Paint
    var mBackground: ColorDrawable = ColorDrawable()
    private var backgroundColor = 0
    private var deleteDrawable: Drawable
    private var doneDrawable: Drawable
    private var inWidth = 0
    private var inHeight = 0
    private var cardPicked = true
    private var reset = false

    init {
        backgroundColor = Color.parseColor("#b80f0a")
        mClearPaint = Paint()

        // KNOW WHAT IS THAT
        mClearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        deleteDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_delete_outline_24
        )!!

        inWidth = deleteDrawable.intrinsicWidth
        inHeight = deleteDrawable.intrinsicHeight

        doneDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_task_alt_24)!!
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
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {// GridLayoutManager
            // flag If the value is 0, it is equivalent to this function being turned off.
            val dragFlag =
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            // create make
            return makeMovementFlags(dragFlag, swipeFlag)
        } else if (layoutManager is LinearLayoutManager) {// linearLayoutManager
            val orientation = layoutManager.orientation
            var dragFlag = 0
            var swipeFlag = 0
            // For the sake of easy understanding, it is equivalent to a horizontal ListView and a vertical ListView.
            if (orientation == LinearLayoutManager.HORIZONTAL)
                run {// If it is a horizontal layout
                    swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                }
            if (orientation == LinearLayoutManager.VERTICAL) {// If it is a vertical layout, equivalent to ListView
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
            }
            return makeMovementFlags(dragFlag, swipeFlag)
        }
        return 0
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        val dragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG || actionState == ItemTouchHelper.ACTION_STATE_SWIPE

        /*if (false) {
            mAdapter.setSwipeRefresh(!dragging)
        }*/
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.20f
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
    ): Boolean {

        val adapter = recyclerView.adapter as ListsAdapter
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        adapter.moveItem(from, to)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {

        if (i == ItemTouchHelper.LEFT) { //<<
            Log.e("Swipe","Swipe LEFT")
        } else if (i == ItemTouchHelper.RIGHT) { // >>
            Log.e("Swipe","Swipe RIGHT")
        }

        // * TODO
        //mAdapter.onSwipe(viewHolder, i)
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

        //Log.e("dX","is $dX")

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val animator = viewHolder.itemView.animate()

            if (isCurrentlyActive) {
                animator.translationZ(16f)
                animator.withEndAction {
                    viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.backgroundSelected))
                }
                viewHolder.itemView.translationZ = 16f
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.backgroundSelected))
            } else {

                animator.translationZ(0f)

                animator.withEndAction {
                    viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background))
                }
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background))
            }

            animator.duration = 100
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        if (isCancelled) {

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

        if (dX > 0) { // SWIPE RIGHT

            mBackground.color = Color.parseColor("#4F5CA4")
            mBackground.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom
            )
            mBackground.draw(canvas)

            val slidePercent = ((dX / itemView.width) * 100).roundToInt()

            if ((slidePercent % 20) == 0 && slidePercent >= 30) {
                Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")
            }

            doneDrawable.setTint(Color.parseColor("#FFFFFF"))

            // ZoomIn and Out bubble anim

            // REMOVE THESE RISKY "!!"
            doneDrawable = if (slidePercent >= 10) {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_task_alt_24)!!
            } else {
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_baseline_task_alt_24
                )!!
            }

            val doneIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val doneIconMargin: Int = (itemHeight - inHeight) / 2
            val doneIconLeft: Int = itemView.left + doneIconMargin
            val doneIconRight: Int = itemView.left + doneIconMargin + inWidth
            val doneIconBottom: Int = doneIconTop + inHeight
            doneDrawable.setBounds(
                doneIconLeft,
                doneIconTop,
                doneIconRight,
                doneIconBottom
            )

            doneDrawable.draw(canvas)
        } else if (dX < 0) { // SWIPE <<

            mBackground.color = backgroundColor
            mBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            mBackground.draw(canvas)

            val slidePercent = ((-dX / itemView.width) * 100).roundToInt()

            Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")

            if ((slidePercent % 20) == 0 && slidePercent >= 30) {
                Log.e("Slide", "${slidePercent}% ${dX.roundToInt()} ${itemView.width}")
            }

            deleteDrawable.setTint(Color.parseColor("#FFFFFF"))

            deleteDrawable = if (slidePercent >= 10) {
                ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_delete_24)!!
            } else {
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_baseline_delete_outline_24_white
                )!!
            }

            val deleteIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val deleteIconMargin: Int = (itemHeight - inHeight) / 2
            val deleteIconLeft: Int = itemView.right - deleteIconMargin - inWidth
            val deleteIconRight: Int = itemView.right - deleteIconMargin
            val deleteIconBottom: Int = deleteIconTop + inHeight
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