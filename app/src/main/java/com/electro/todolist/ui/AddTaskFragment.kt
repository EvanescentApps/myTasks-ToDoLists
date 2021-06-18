package com.electro.todolist.ui

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import com.electro.todolist.R
import com.electro.todolist.data.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import kotlin.random.Random.Default.nextInt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddTaskFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class AddTaskFragment : BottomSheetDialogFragment() {

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var taskTitleEditText: EditText
    private lateinit var description: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle)

    }

    private fun saveTask(currentList: String = "list1") {
        val title = taskTitleEditText.text.toString()
        val descriptionText = description.text.toString()

        taskTitleEditText.clearFocus()
        description.clearFocus()
        // Hide keyboard

        if (title.isBlank()){
            return
        }

        val id = Task.generateId(5)
        //Toast.makeText(requireContext(),"ID : $id", Toast.LENGTH_SHORT).show()
        Log.e("ID", id)

        val timeId = System.currentTimeMillis()
        val taskObject = Task(title,descriptionText,timeId,false, id)
        val taskToJson = Gson().toJson(taskObject)

        val list = requireActivity().getSharedPreferences(currentList, AppCompatActivity.MODE_PRIVATE)
        list.edit().putString(timeId.toString(), taskToJson).apply()


        if (requireActivity() is TasksActivity) {
            (requireActivity() as TasksActivity).addItem(taskObject)
        }

        description.text.clear()
        taskTitleEditText.text.clear()

        return
    }

    override fun onDismiss(dialog: DialogInterface) {
        //Toast.makeText(requireContext(),"BottomSheet dismissed",Toast.LENGTH_SHORT).show()
        var currentList = "list1"
        requireArguments().getString("currentList")?.let {
            currentList = it
        }
        saveTask(currentList)

        //HIDE KEYBOARD BEFORE
        super.onDismiss(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val d = dialog as BottomSheetDialog
        val bottomSheetInternal =
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetInternal as View)

        val coordinatorLayout = bottomSheetInternal.parent as CoordinatorLayout

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheetBehavior.peekHeight = bottomSheetInternal.height
        coordinatorLayout.parent.requestLayout();


        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_add_task, container, false)

        taskTitleEditText = v.findViewById(R.id.title_edit_text)
        taskTitleEditText.text.clear()
        taskTitleEditText.requestFocus()

        description = v.findViewById(R.id.description)
        description.text.clear()

        val saveTask = v.findViewById<Button>(R.id.save_task)
        val addDescription = v.findViewById<ImageButton>(R.id.showDescription)

        addDescription.setOnClickListener {
            description.visibility = View.VISIBLE
            description.requestFocus()
        }

        saveTask.setOnClickListener {
            dismiss() // Task saved here
        }

        return v
    }

    companion object { // Params, unused for now
        @JvmStatic
        fun newInstance(currentList: String) =
                AddTaskFragment().apply {
                    arguments = Bundle().apply {
                        putString("currentList", currentList)
                    }
                }
    }
}