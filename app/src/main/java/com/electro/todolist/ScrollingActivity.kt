package com.electro.todolist

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList


class ScrollingActivity : AppCompatActivity() {

    private lateinit var tasks: ArrayList<Task>
    private lateinit var adapter: RecyclerView.Adapter<TasksAdapter.ViewHolder>
    private lateinit var list1: SharedPreferences
    private lateinit var listEdit : SharedPreferences.Editor
    private lateinit var tasksRecyclerView:RecyclerView
    //private lateinit var bottomSheet: BottomSheetBehavior<View>

    // mTasks : ArrayList qui sert à l'affichage
    // tasks : ArrayList qui sert à sync les données, gérée par l'activité

    fun addItem(task: Task) {
        tasks.add(0, task) // Ajoute la tâche au début de la liste
        adapter.notifyItemInserted(0)
        //adapter.notifyItemRangeChanged(0, tasks.size)
    }

    fun deleteItem(index: Int) { // Called onSwipe
        val taskToDelete = tasks[index] // Get the task to delete

        //tasks.removeAt(index) // Supprimée de la liste de sync

        listEdit.remove(taskToDelete.creationDate.toString()).apply() // Supprimée

        Log.e("Activity", "Delete item at $index")
        //Log.e("Etat tasks", "array to json : ${Gson().toJson(tasks)}")

        Snackbar.make(findViewById(R.id.activity), "Tâche supprimée", Snackbar.LENGTH_LONG)
            .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
            .setAction("Annuler") { // Suppression annulée : remettre la tâche

                listEdit.putString(taskToDelete.creationDate.toString(), Gson().toJson(taskToDelete)).apply()

                tasks.add(index,taskToDelete)
                adapter.notifyItemInserted(index)
                adapter.notifyItemRangeChanged(index,tasks.size)
            }.show()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {


        Collections.swap(tasks, fromPosition, toPosition)

        val titlesOnly= arrayListOf<String>()
        tasks.forEach { titlesOnly.add(it.title) }
        Log.i("tasks ", Gson().toJson(titlesOnly))

        Log.d("Activity position","from: $fromPosition to: $toPosition")

    }

    fun removeItem(index: Int) {

        val taskDone = tasks[index] // We get the task
        taskDone.done = true
        val taskToJson = Gson().toJson(taskDone) // Converted to Json for storage
        listEdit.putString(taskDone.creationDate.toString(), taskToJson).apply()

        //tasks.removeAt(index)

        Log.e("Activity", "Remove item at $index")
        Log.e("tasks (activity)", "size after removed : ${tasks.size}")

    }

    fun setTaskDone(task: Task, done: Boolean) {
        task.done = done
        listEdit.putString(task.creationDate.toString(), Gson().toJson(task)).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        //setSupportActionBar(findViewById(R.id.toolbar)) to show top right menu (:points)
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = "Mes tâches"
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        // SYNC ALL TASKS (GET) WITH CLOUD FIRESTORE

        tasksRecyclerView = findViewById<View>(R.id.tasks_recyclerview) as RecyclerView

        list1 = getSharedPreferences("list1", MODE_PRIVATE)
        listEdit = list1.edit()

        val listOfAllTasks = ArrayList<Task>()
        val allEntries  = list1.all.map { it.key }

        Log.e("All Entries", allEntries.toString())

        allEntries.forEach { str ->
            val jsonFromData = list1.getString(str, "Empty")
            jsonFromData?.let { Log.e("KEYS", it) } // Log the key

            Log.e("JSON", Gson().toJson(jsonFromData))

            val objectTraduit = Gson().fromJson(jsonFromData, Task::class.java) //Convert to task
            listOfAllTasks.add(objectTraduit)
        }

        // Here tasks are sorted by date
        listOfAllTasks.sortWith { o1, o2 -> o2.creationDate.compareTo(o1.creationDate) }

        // ArrayList<Task> created by the Object Class Task
        tasks = Task.createTasksList(listOfAllTasks)
        // This part can be better..

        // Here we create the Adapter for the RecyclerView, with the tasks
        adapter = TasksAdapter(tasks, this)

        tasksRecyclerView.adapter = adapter // Adapter assigned to the recyclerview
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        // TODO : Bug pour le Drag to Reorder : La position n'est pas MàJ...

        val helperCallback = ItemTouchHelperCallback(adapter as TasksAdapter)
        val helper = ItemTouchHelper(helperCallback)
        helper.attachToRecyclerView(tasksRecyclerView)

        //itemTouchHelper.attachToRecyclerView(tasksRecyclerView)

        /*bottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottomDialogParent))
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.isHideable = true*/

        val bottomDialog = AddTaskFragment()
        bottomDialog.isCancelable = true

        fab.setOnClickListener { view -> bottomDialog.show(supportFragmentManager, "dialog") }

        findViewById<View>(R.id.context).setOnClickListener {
            bottomDialog.show(supportFragmentManager, "dialog")
        }



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}