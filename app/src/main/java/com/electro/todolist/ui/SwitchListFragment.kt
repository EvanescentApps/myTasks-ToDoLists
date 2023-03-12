package com.electro.todolist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ListsItemTouchCallback
import com.electro.todolist.R
import com.electro.todolist.data.TasksRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ListObject(
    val id: String,
    val title : String,
    val isCurrentSelected: Boolean = false,
    val position : Int
)

@Keep
@Serializable
data class SerialListObject(
    val id: String,
    val title : String,
    val position : Int
)

class ChangeListFragment : BottomSheetDialogFragment() {

    //private val Context.dataStore by preferencesDataStore(name = "settings")

    private lateinit var listAdapter : ListsAdapter
    private var currentListSelected : Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.bottom_sheet_lists, container, false)
    }

    private fun newListAdded(){
        listAdapter.notifyItemInserted(currentListSelected + 1)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val d = dialog as BottomSheetDialog
        val bottomSheetInternal =
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetInternal as View)

        val coordinatorLayout = bottomSheetInternal.parent as CoordinatorLayout

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        //bottomSheetBehavior.peekHeight = bottomSheetInternal.height
        coordinatorLayout.parent.requestLayout()

        val tasksRepository = TasksRepository.getInstance(requireActivity())
        val recyclerList = view.findViewById<RecyclerView>(R.id.all_lists)
        val currentList = requireArguments().getString("currentList")

        val listOfAllLists = ArrayList<ListObject>()
        var listOfPairs: List<Pair<String, String>>

        requireArguments().getString("list")?.let { string ->
            listOfPairs = Json.decodeFromString(string)

            listOfPairs.forEachIndexed { index,  pair ->
                listOfAllLists.add(
                    ListObject(pair.first, pair.second, (pair.first == currentList), index
                    )
                )
            }
        }

        if (listOfAllLists.isEmpty()) {
            dismissAllowingStateLoss()
            Toast.makeText(requireContext(),"Aucune liste créée", Toast.LENGTH_SHORT).show()
        }

        listAdapter = ListsAdapter(listOfAllLists, requireActivity(), this)

        currentList?.let {
            recyclerList?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = listAdapter


                //itemTouchHelper.attachToRecyclerView(null)
                val itemTouchHelperCallback = ListsItemTouchCallback(listAdapter, this.context)
                val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                itemTouchHelper.attachToRecyclerView(recyclerList)
            }
        } ?: run {
            Toast.makeText(requireActivity(), "Aucune liste sélectionnée...", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

        val createListButton : LinearLayout = view.findViewById(R.id.createList)
        val settingsButton : LinearLayout = view.findViewById(R.id.settings)

        createListButton.setOnClickListener {

            val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
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
                    tasksRepository.createList(mText, true)
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
            //requireContext().startActivity(Intent(requireActivity(), SettingsActivity::class.java))

            val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)

            builder.apply {
                setTitle("Fonctionnalité en cours de développement")
                setMessage("Les paramètres ne sont pas disponibles pour l'instant, ce sera pour une prochaine mise à jour.")
                setPositiveButton(
                    "D'accord"
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                show()
            }
        }
    }

    private inner class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(
        inflater.inflate(R.layout.list_item, parent, false)) {

        val linearLayout: LinearLayout = itemView.findViewById(R.id.bottomSheetParent)
        val text: TextView = itemView.findViewById(R.id.text)
    }

    /*private inner class ItemAdapter(private val mList: ArrayList<ListObject>) : //private val mItemCount: Int,
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, adapterPosition: Int) {
            holder.text.text = mList[holder.bindingAdapterPosition].title


            if (mList[holder.bindingAdapterPosition].isCurrentSelected) {
                holder.linearLayout.setBackgroundResource(R.drawable.list_item_bg_selected)
                currentListSelected = holder.bindingAdapterPosition
            }

            holder.itemView.setOnClickListener {
                if (requireActivity() is TasksActivity) {
                    (requireActivity() as TasksActivity).changeList(mList[holder.bindingAdapterPosition].id)
                } else if (requireActivity() is TaskDetailsActivity) {
                    // TODO : SWITCH TASK TO SELECTED LIST
                    Log.i("Switch","Switching task to selected list : ${mList[holder.bindingAdapterPosition].title}")
                }
                dismissAllowingStateLoss()
            }
        }

        override fun getItemCount(): Int = mList.size
    }*/

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