package com.electro.todolist

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.preferences.preferencesDataStore
import com.electro.todolist.data.TasksRepository

// TODO: Customize parameter argument names
const val ARG_CURRENT_LIST = "currentList"

data class ListAction(
    val title : String,
    val action: () -> Unit
)

class BottomFragment : BottomSheetDialogFragment() {

    val Context.dataStore by preferencesDataStore(name = "settings")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val tasksRepository = TasksRepository(requireContext().dataStore, requireActivity())
        val recyclerList = view.findViewById<RecyclerView>(R.id.list)

        val currentList = requireArguments().getString(ARG_CURRENT_LIST)

        if(currentList != null){
            recyclerList?.layoutManager = LinearLayoutManager(context)
            recyclerList?.adapter = ItemAdapter(arrayListOf( //"Paramètres","Mes tâches","Aujourd'hui","Demain","Projets","Un Jour"

                ListAction("Renommer la liste") {
                    Toast.makeText(requireActivity(), "Renommer la liste", Toast.LENGTH_SHORT).show()
                },
                ListAction("Supprimer la liste") {
                    Toast.makeText(requireActivity(), "Supprimer la liste", Toast.LENGTH_SHORT).show()
                },
                ListAction("Supprimer toutes les tâches terminées") {
                    Toast.makeText(requireActivity(), "Supprimer toutes les tâches terminées", Toast.LENGTH_SHORT).show()
                },
                ListAction("Exporter la liste") {
                    /*Le code s'exécute dans une fonction de l'activité,
                    * qui possède les droits d'accès aux données requises, et le contexte*/

                    tasksRepository.exportToFile(currentList)
                },
                ListAction("Importer une liste") {
                    //(requireActivity() as TasksActivity?)!!.exportToJson()
                    tasksRepository.openFile()
                },
                ListAction("Créer une nouvelle liste") {
                    //(requireActivity() as TasksActivity?)!!.exportToJson()
                    tasksRepository.openFile()
                },
                ListAction("Paramètres") {
                    Toast.makeText(requireActivity(), "Paramètres", Toast.LENGTH_SHORT).show()
                },
                ListAction("Changer le thème") {
                    Toast.makeText(requireActivity(), "Changer le thème", Toast.LENGTH_SHORT).show()
                }
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
            R.layout.fragment_bottom_list_dialog_item,
            parent,
            false
        )
    ) {

        val text: TextView = itemView.findViewById(R.id.text)
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