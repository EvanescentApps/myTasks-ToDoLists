package com.electro.todolist

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.electro.todolist.ScrollingActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        setStyle(DialogFragment.STYLE_NORMAL,R.style.BottomSheetStyle)

    }

    override fun onResume() {
        super.onResume()

        taskTitleEditText.setText("")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        val v = inflater.inflate(R.layout.fragment_add_task, container, false)
        taskTitleEditText = v.findViewById<EditText>(R.id.title_edit_text)
        taskTitleEditText.setText("")
        taskTitleEditText.requestFocus()

        val saveTask = v.findViewById<Button>(R.id.save_task)
        val addDescription = v.findViewById<ImageButton>(R.id.showDescription)
        val description = v.findViewById<EditText>(R.id.description)
        //description.setText("")
        description.text.clear()
        //description.clearComposingText()

        addDescription.setOnClickListener {
            description.visibility = View.VISIBLE
            //description.setText("")
            //description.clearComposingText()
            //description.text.clear()
            description.requestFocus()
        }

        saveTask.setOnClickListener {

            // Get task attributes,
            val title = taskTitleEditText.text.toString()

            // MISSING : DESCRIPTION
            val descriptionText = description.text.toString()

            val list1 = requireActivity().getSharedPreferences("list1", AppCompatActivity.MODE_PRIVATE)

            val timeId = System.currentTimeMillis()
            val taskObject = Task(title,descriptionText,timeId,false)
            val taskToJson = Gson().toJson(taskObject)

            list1.edit().putString(timeId.toString(), taskToJson).apply()

            (activity as ScrollingActivity?)!!.addItem(taskObject)




            // Save task
            // dismiss
            // update recycler view

            dismiss()
        }

        return v
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddTaskFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                AddTaskFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}