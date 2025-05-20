/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder


const val ARG_CURRENT_LIST = "currentList"

// Define an interface for actions that the BottomFragment can request
interface BottomFragmentActions {
    fun onRenameList(listId: String, newName: String)
    fun onDeleteList(listId: String)
    fun onDeleteAllDoneTasks()
    fun onExportList(listId: String)
    fun onImportList()
    fun onCreateNewList(listTitle: String)
}

data class ListAction(
    val title : String,
    val iconResource : Int? = null,
    val action: () -> Unit
)

class BottomFragment : BottomSheetDialogFragment() {

    // A reference to the activity that implements BottomFragmentActions
    private var actionsListener: BottomFragmentActions? = null

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (context is BottomFragmentActions) {
            actionsListener = context
        } else {
            throw RuntimeException("$context must implement BottomFragmentActions")
        }
    }

    override fun onDetach() {
        super.onDetach()
        actionsListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.context_menu_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Call super.onViewCreated

        val d = dialog as BottomSheetDialog
        val bottomSheetInternal =
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetInternal as View)

        val coordinatorLayout = bottomSheetInternal.parent as CoordinatorLayout

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        coordinatorLayout.parent.requestLayout()

        val recyclerList = view.findViewById<RecyclerView>(R.id.list)
        // Get the current list ID from arguments
        val currentListId = arguments?.getString(ARG_CURRENT_LIST)

        if(currentListId != null){
            recyclerList?.layoutManager = LinearLayoutManager(context)
            recyclerList?.adapter = ItemAdapter(arrayListOf(

                ListAction("Renommer la liste", R.drawable.outline_drive_file_rename_outline_24) {
                    showRenameListDialog(currentListId)
                },
                ListAction("Supprimer la liste", R.drawable.ic_baseline_delete_outline_24) {
                    showDeleteListDialog(currentListId)
                },
                ListAction("Supprimer toutes les tâches terminées", R.drawable.outline_delete_sweep_24) {
                    showDeleteAllDoneTasksDialog()
                },
                ListAction("Exporter la liste", R.drawable.outline_file_upload_24) {
                    actionsListener?.onExportList(currentListId)
                    Toast.makeText(requireActivity(), "Export de la liste au format texte (JSON)", Toast.LENGTH_SHORT).show()
                    dismissAllowingStateLoss()
                },
                ListAction("Importer une liste", R.drawable.outline_file_download_24) {
                    actionsListener?.onImportList()
                    Toast.makeText(requireActivity(), "Importez une liste au format texte (JSON)", Toast.LENGTH_SHORT).show()
                    dismissAllowingStateLoss()
                },
                ListAction("Créer une nouvelle liste", R.drawable.ic_add_black_48dp) {
                    showCreateNewListDialog()
                }
            ))
        } else {
            Toast.makeText(requireActivity(), "Aucune liste sélectionnée...", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }
    }

    private fun showRenameListDialog(listId: String) {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, view as ViewGroup?, false)
        val input = viewInflated.findViewById<EditText>(R.id.input)

        builder.apply {
            setTitle("Renommer une liste")
            setView(viewInflated)
            setPositiveButton("Renommer") { dialog, _ ->
                dialog.dismiss()
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    actionsListener?.onRenameList(listId, newName)
                } else {
                    Toast.makeText(context,"Veuillez définir un nom pour la liste",Toast.LENGTH_LONG).show()
                }
                dismissAllowingStateLoss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    private fun showDeleteListDialog(listId: String) {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
        builder.apply {
            setTitle("Supprimer la liste ?")
            setMessage("Toutes les tâches de cette liste seront définitivement supprimées, continuer ?")
            setPositiveButton("Supprimer") { _, _ ->
                actionsListener?.onDeleteList(listId)
                dismissAllowingStateLoss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun showDeleteAllDoneTasksDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
        builder.apply {
            setTitle("Supprimer toutes les tâches terminées ?")
            setMessage("Toutes les tâches terminées seront définitivement supprimées, continuer ?")
            setPositiveButton("Supprimer") { _, _ ->
                actionsListener?.onDeleteAllDoneTasks()
                dismissAllowingStateLoss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun showCreateNewListDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.list_name_edit, view as ViewGroup?, false)
        val input = viewInflated.findViewById<EditText>(R.id.input)

        builder.apply {
            setView(viewInflated)
            setTitle("Créer une liste")
            setPositiveButton("Créer") { dialog, _ ->
                dialog.dismiss()
                val mText = input.text.toString()
                if (mText.isNotEmpty()) {
                    actionsListener?.onCreateNewList(mText)
                } else {
                    Toast.makeText(context, "Veuillez définir un nom pour la liste", Toast.LENGTH_LONG).show()
                }
                dismissAllowingStateLoss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
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

    private inner class ItemAdapter(private val mList: ArrayList<ListAction>) :
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
                holder.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),res))
            }
        }

        override fun getItemCount(): Int = mList.size
    }

    companion object {
        fun newInstance(currentList: String): BottomFragment =
            BottomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_LIST, currentList)
                }
            }
    }
}