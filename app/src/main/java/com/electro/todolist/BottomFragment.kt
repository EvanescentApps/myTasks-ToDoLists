package com.electro.todolist

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.preferences.preferencesDataStore
import com.electro.todolist.data.TasksRepository

// TODO: Customize parameter argument names
const val ARG_CURRENT_LIST = "currentList"

data class ListAction(
    val title : String,
    val iconResource : Int? = null,
    val action: () -> Unit
)

class BottomFragment : BottomSheetDialogFragment() {

    private val Context.dataStore by preferencesDataStore(name = "settings")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.context_menu_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val tasksRepository = TasksRepository(requireActivity())
        val recyclerList = view.findViewById<RecyclerView>(R.id.list)

        val currentListId = requireArguments().getString(ARG_CURRENT_LIST)

        if(currentListId != null){
            recyclerList?.layoutManager = LinearLayoutManager(context)
            recyclerList?.adapter = ItemAdapter(arrayListOf( //"Paramètres","Mes tâches","Aujourd'hui","Demain","Projets","Un Jour"

                ListAction("Renommer la liste", R.drawable.outline_drive_file_rename_outline_24) {
                    //Toast.makeText(requireActivity(), "Renommer la liste", Toast.LENGTH_SHORT).show()
                    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    // I'm using fragment here so I'm using getView() to provide ViewGroup
                    // but you can provide here any other instance of ViewGroup from your Fragment / Activity

                    val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, getView() as ViewGroup?, false)

                    val input = viewInflated.findViewById<EditText>(R.id.input)
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(viewInflated)

                    builder.setTitle("Renommer une liste")

                    builder.setPositiveButton(
                        R.string.ok
                    ) { dialog, _ ->
                        dialog.dismiss()
                        val newName = input.text.toString()

                        if (newName.isNotEmpty()) {
                            tasksRepository.renameList(newName, currentListId)
                        } else {
                            Toast.makeText(context,"Veuillez définir un nom pour la liste",Toast.LENGTH_LONG).show()
                        }

                        dismissAllowingStateLoss()
                    }
                    builder.setNegativeButton(
                        R.string.cancel
                    ) { dialog, _ -> dialog.cancel() }

                    builder.show()

                    // TODO : RENAME THE LIST IN THE CHANGELISTBS
                    // Launch popup to rename list
                },
                ListAction("Supprimer la liste", R.drawable.ic_baseline_delete_outline_24) {
                    //Toast.makeText(requireActivity(), "Suppression de la liste...", Toast.LENGTH_SHORT).show()
                    tasksRepository.deleteList(currentListId)
                    dismissAllowingStateLoss()
                },
                ListAction("Supprimer toutes les tâches terminées", R.drawable.outline_delete_sweep_24) {
                    //Toast.makeText(requireActivity(), "Suppression des tâches terminées...", Toast.LENGTH_SHORT).show()
                    tasksRepository.deleteAllDoneTasks(currentListId)
                    dismissAllowingStateLoss()
                },
                ListAction("Exporter la liste", R.drawable.outline_file_upload_24) {
                    /*Le code s'exécute dans une fonction du repository,
                    * qui possède les droits d'accès aux données requises, et le contexte*/
                    Toast.makeText(requireActivity(), "Export de la liste au format texte (JSON)", Toast.LENGTH_SHORT).show()
                    tasksRepository.exportToFile(currentListId)
                    dismissAllowingStateLoss()
                },
                ListAction("Importer une liste", R.drawable.outline_file_download_24) {
                    //(requireActivity() as TasksActivity?)!!.exportToJson()
                    Toast.makeText(requireActivity(), "Importez une liste au format texte (JSON)", Toast.LENGTH_SHORT).show()
                    tasksRepository.openFile()
                    dismissAllowingStateLoss()
                },
                ListAction("Créer une nouvelle liste", R.drawable.ic_add_black_48dp) {
                    Toast.makeText(requireActivity(), "Création d'une liste", Toast.LENGTH_SHORT).show()
                    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    // I'm using fragment here so I'm using getView() to provide ViewGroup
                    // but you can provide here any other instance of ViewGroup from your Fragment / Activity

                    val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, getView() as ViewGroup?, false)

                    val input = viewInflated.findViewById<EditText>(R.id.input)
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(viewInflated)

                    builder.setTitle("Créer une liste")

                    builder.setPositiveButton(
                        R.string.ok
                    ) { dialog, _ ->
                        dialog.dismiss()
                        val mText = input.text.toString()
                        tasksRepository.createList(mText)
                        dismissAllowingStateLoss()
                    }
                    builder.setNegativeButton(
                        R.string.cancel
                    ) { dialog, _ -> dialog.cancel() }

                    builder.show()
                    // Launch popup or bottomsheet for the name
                },
                /*ListAction("Changer le thème") {
                    Toast.makeText(requireActivity(), "Changer le thème", Toast.LENGTH_SHORT).show()
                }*/
            ))
        } else {
            Toast.makeText(requireActivity(), "Aucune liste sélectionnée...", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

         //arguments?.getInt(ARG_ITEM_COUNT2)?.let {  }
    }

    private inner class ViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        inflater.inflate(
            R.layout.context_menu_list_item,
            parent,
            false
        )
    ) {

        val text: TextView = itemView.findViewById(R.id.text)
        val icon: ImageView = itemView.findViewById(R.id.icon)
    }

    private inner class ItemAdapter(private val mList: ArrayList<ListAction>) : //private val mItemCount: Int,
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = mList[position].title
            holder.itemView.setOnClickListener {
                mList[position].action()
            }
            mList[position].iconResource?.let { res ->
                holder.icon.setImageDrawable(resources.getDrawable(res))
            }
        }

        override fun getItemCount(): Int {
            return mList.size
        }
    }

    companion object {

        // TODO: Customize parameters
        fun newInstance(currentList: String): BottomFragment =
            BottomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_LIST, currentList)
                }
            }

    }
}