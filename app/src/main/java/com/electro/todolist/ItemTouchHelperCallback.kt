package com.electro.todolist

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ui.home.ListsAdapter
import com.electro.todolist.ui.home.TasksAdapter
import androidx.core.graphics.toColorInt
import timber.log.Timber

class ItemTouchHelperCallback(adapter: TasksAdapter, context: Context) : ItemTouchHelper.Callback() {
    private val mAdapter: TasksAdapter = adapter
    private var mContext: Context = context

    private var initialFromPosition: Int = -1 // Track the initial position of the dragged item

    // ... (rest of your existing properties and init block) ...
    private var mClearPaint: Paint
    var mBackground: ColorDrawable = ColorDrawable()
    private var backgroundColor = 0
    private var deleteDrawable: Drawable
    private var doneDrawable: Drawable
    private var inWidth = 0
    private var inHeight = 0
    private var reset = false


    init {
        backgroundColor = "#b80f0a".toColorInt()
        mClearPaint = Paint()

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


    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val layoutManager = recyclerView.layoutManager
        return if (layoutManager is GridLayoutManager) {
            val dragFlag =
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            makeMovementFlags(dragFlag, swipeFlag)
        } else if (layoutManager is LinearLayoutManager) {
            val orientation = layoutManager.orientation
            var dragFlag = 0
            var swipeFlag = 0
            if (orientation == LinearLayoutManager.HORIZONTAL) {
                swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            }
            if (orientation == LinearLayoutManager.VERTICAL) {
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
            }
            makeMovementFlags(dragFlag, swipeFlag)
        } else {
            0
        }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            initialFromPosition = viewHolder?.bindingAdapterPosition ?: -1
            Timber.d("TasksItemTouchHelperCallback: Drag started at position $initialFromPosition")
        }
        // mAdapter.setSwipeRefresh(!dragging) is not needed here
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.20f
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        mAdapter.onItemMove(from, to) // Call the adapter's visual move function
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        mAdapter.onSwipe(viewHolder, i) // Delegate to the adapter's onSwipe method
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

        if (dX > 0) { // SWIPE RIGHT
            mBackground.color = "#4F5CA4".toColorInt()
            mBackground.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom
            )
            mBackground.draw(canvas)

            doneDrawable.setTint("#FFFFFF".toColorInt())

            val doneIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val doneIconMargin: Int = (itemHeight - inHeight) / 2
            val doneIconLeft: Int = itemView.left + doneIconMargin
            val doneIconRight: Int = itemView.left + doneIconMargin + inWidth
            val doneIconBottom: Int = doneIconTop + inHeight
            doneDrawable.setBounds(doneIconLeft, doneIconTop, doneIconRight, doneIconBottom)
            doneDrawable.draw(canvas)

        } else if (dX < 0) { // SWIPE LEFT
            mBackground.color = backgroundColor
            mBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            mBackground.draw(canvas)

            deleteDrawable.setTint("#FFFFFF".toColorInt())

            val deleteIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val deleteIconMargin: Int = (itemHeight - inHeight) / 2
            val deleteIconLeft: Int = itemView.right - deleteIconMargin - inWidth
            val deleteIconRight: Int = itemView.right - deleteIconMargin
            val deleteIconBottom: Int = deleteIconTop + inHeight
            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
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
        // This method is called when the interaction (swipe or drag) is over.
        // It's the ideal place to notify the ViewModel about the final position.
        val currentPosition = viewHolder.bindingAdapterPosition
        if (initialFromPosition != -1 && currentPosition != -1 && initialFromPosition != currentPosition) {
            mAdapter.onDropCompleted(initialFromPosition, currentPosition)
        }
        initialFromPosition = -1 // Reset for next drag operation
        reset = true // Unused property, consider removing if not performing a specific reset
    }
}

// ListsItemTouchCallback remains the same structure for now, as it needs to be updated
// with the new adapter callback approach similar to TasksAdapter.
// I've kept it as-is for now, but be aware it will need similar refactoring.

class ListsItemTouchCallback(adapter: ListsAdapter, context: Context) : ItemTouchHelper.Callback() {
    private val mAdapter = adapter
    private var mContext: Context = context

    // ... (rest of your existing properties and init block) ...
    private var mClearPaint: Paint
    var mBackground: ColorDrawable = ColorDrawable()
    private var backgroundColor = 0
    private var deleteDrawable: Drawable
    private var doneDrawable: Drawable
    private var inWidth = 0
    private var inHeight = 0
    private var reset = false

    init {
        backgroundColor = "#b80f0a".toColorInt()
        mClearPaint = Paint()

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

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val layoutManager = recyclerView.layoutManager
        return if (layoutManager is GridLayoutManager) {
            val dragFlag =
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            makeMovementFlags(dragFlag, swipeFlag)
        } else if (layoutManager is LinearLayoutManager) {
            val orientation = layoutManager.orientation
            var dragFlag = 0
            var swipeFlag = 0
            if (orientation == LinearLayoutManager.HORIZONTAL) {
                swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            }
            if (orientation == LinearLayoutManager.VERTICAL) {
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
            }
            makeMovementFlags(dragFlag, swipeFlag)
        } else {
            0
        }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        // mAdapter.setSwipeRefresh(!dragging) is not needed here
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.20f
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        mAdapter.moveItem(from, to) // Assuming ListsAdapter has a moveItem for visual swap
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        // TODO: This needs to be updated to use ListsAdapter callbacks for swipe-to-delete
        // mAdapter.onSwipe(viewHolder, i)
        Timber.w("ListsItemTouchCallback: onSwiped called, but adapter callback is TODO.")
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            mAdapter.onSwipe(viewHolder, i) // Call the onSwipe function that passes to the callback
        }
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

        if (dX > 0) { // SWIPE RIGHT
            mBackground.color = "#4F5CA4".toColorInt()
            mBackground.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom
            )
            mBackground.draw(canvas)
            doneDrawable.setTint("#FFFFFF".toColorInt())
            val doneIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val doneIconMargin: Int = (itemHeight - inHeight) / 2
            val doneIconLeft: Int = itemView.left + doneIconMargin
            val doneIconRight: Int = itemView.left + doneIconMargin + inWidth
            val doneIconBottom: Int = doneIconTop + inHeight
            doneDrawable.setBounds(doneIconLeft, doneIconTop, doneIconRight, doneIconBottom)
            doneDrawable.draw(canvas)
        } else if (dX < 0) { // SWIPE LEFT
            mBackground.color = backgroundColor
            mBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            mBackground.draw(canvas)
            deleteDrawable.setTint("#FFFFFF".toColorInt())
            val deleteIconTop: Int = itemView.top + (itemHeight - inHeight) / 2
            val deleteIconMargin: Int = (itemHeight - inHeight) / 2
            val deleteIconLeft: Int = itemView.right - deleteIconMargin - inWidth
            val deleteIconRight: Int = itemView.right - deleteIconMargin
            val deleteIconBottom: Int = deleteIconTop + inHeight
            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
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
        // ListsItemTouchCallback doesn't currently have a `onDropCompleted` equivalent.
        // If you need to persist list reordering, you'll need to add it to ListsAdapter
        // and trigger it here, similar to TasksItemTouchHelperCallback.
        reset = true
    }
}