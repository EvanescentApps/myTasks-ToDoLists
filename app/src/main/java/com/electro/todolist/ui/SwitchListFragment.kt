package com.electro.todolist.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.electro.todolist.SettingsActivity
import com.electro.todolist.data.TasksRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ListObject(
    val id: String,
    val title : String,
    val isCurrentSelected: Boolean = false
)

class ChangeListFragment : BottomSheetDialogFragment() {

    //private val Context.dataStore by preferencesDataStore(name = "settings")

    private lateinit var listAdapter : ItemAdapter
    private var currentListSelected : Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_lists, container, false)
    }

    fun newListAdded(){
        listAdapter.notifyItemInserted(currentListSelected + 1)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val tasksRepository = TasksRepository.getInstance(requireActivity())
        val recyclerList = view.findViewById<RecyclerView>(R.id.all_lists)

        val currentList = requireArguments().getString("currentList")

        val listOfAllLists = ArrayList<ListObject>()
        var listOfPairs: List<Pair<String, String>>

        requireArguments().getString("list")?.let { string ->
            listOfPairs = Json.decodeFromString(string)

            listOfPairs.forEach { pair ->
                listOfAllLists.add(
                    ListObject(pair.first, pair.second, (pair.first == currentList)
                    )
                )
            }
        }

        if (listOfAllLists.isNullOrEmpty()) {
            dismissAllowingStateLoss()
            Toast.makeText(requireContext(),"Aucune liste créée", Toast.LENGTH_SHORT).show()
        }

        listAdapter = ItemAdapter(listOfAllLists)

        currentList?.let {
            recyclerList?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = listAdapter
            }
        } ?: run {
            Toast.makeText(requireActivity(), "Aucune liste sélectionnée...", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

        val createListButton : LinearLayout = view.findViewById(R.id.createList)
        val settingsButton : LinearLayout = view.findViewById(R.id.settings)

        createListButton.setOnClickListener {

            val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(requireContext(),
                R.style.AlertDialogCustom
            ))
            // I'm using fragment here so I'm using getView() to provide ViewGroup
            // but you can provide here any other instance of ViewGroup from your Fragment / Activity

            val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, getView() as ViewGroup?, false)

            val input = viewInflated.findViewById<EditText>(R.id.input)

            builder.apply {
                setTitle("Créer une liste")
                setView(viewInflated)
                setPositiveButton(
                    R.string.ok
                ) { dialog, _ ->
                    dialog.dismiss()
                    val mText = input.text.toString()
                    tasksRepository.createList(mText)
                    newListAdded()
                    dismissAllowingStateLoss()
                }
                setNegativeButton(
                    R.string.cancel
                ) { dialog, _ -> dialog.cancel() }
                show()
            }
        }

        settingsButton.setOnClickListener {
            requireContext().startActivity(Intent(requireActivity(), SettingsActivity::class.java))
        }
    }

    private inner class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(
        inflater.inflate(R.layout.list_item, parent, false)) {

        val linearLayout: LinearLayout = itemView.findViewById(R.id.bottomSheetParent)
        val text: TextView = itemView.findViewById(R.id.text)
    }

    private inner class ItemAdapter(private val mList: ArrayList<ListObject>) : //private val mItemCount: Int,
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = mList[position].title


            if (mList[position].isCurrentSelected) {
                holder.linearLayout.setBackgroundResource(R.drawable.list_item_bg_selected)
                currentListSelected = position
            }

            holder.itemView.setOnClickListener {
                if (requireActivity() is TasksActivity) {
                    (requireActivity() as TasksActivity).changeList(mList[position].id)
                } else if (requireActivity() is TaskDetailsActivity) {
                    // TODO : SWITCH TASK TO SELECTED LIST
                    Log.i("Switch","Switching task to selected list : ${mList[position].title}")
                }
                dismissAllowingStateLoss()
            }
        }

        override fun getItemCount(): Int = mList.size
    }

    companion object {

        // TODO: Customize parameters
        fun newInstance(currentList: String, listsGroup: String): ChangeListFragment =
            ChangeListFragment().apply {
                arguments = Bundle().apply {
                    putString("currentList", currentList)
                    putString("list", listsGroup)
                }
            }
    }
}