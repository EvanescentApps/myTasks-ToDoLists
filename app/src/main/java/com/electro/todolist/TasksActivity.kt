package com.electro.todolist

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList


class TasksActivity : AppCompatActivity() {

    private lateinit var tasks: ArrayList<Task>
    private lateinit var adapter: RecyclerView.Adapter<TasksAdapter.ViewHolder>
    private lateinit var list1: SharedPreferences
    private lateinit var listEditor: SharedPreferences.Editor
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var allLists: SharedPreferences
    private lateinit var currentList: String
    private lateinit var defaultListKey: String
    //private lateinit var bottomSheet: BottomSheetBehavior<View>

    // mTasks : ArrayList qui sert à l'affichage
    // tasks : ArrayList qui sert à sync les données, gérée par l'activité

    private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

    //val snakeRegex = "_[a-zA-Z]".toRegex()

    fun String.toSafeCase(): String {

        return java.text.Normalizer.normalize(this.toLowerCase(Locale.getDefault()), java.text.Normalizer.Form.NFD).filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ","_")
        /*camelRegex.replace(this) {
            "_${it.value}"
        }*/
    }

    fun updatedTask(task: Task, position: Int) {
        tasks[position] = task
        adapter.notifyItemChanged(position)
    }

    fun exportToJson() {

        /* get list name
         * to lower snake case
         * get date + hour
         * append to fileName
         * get Json
         * save it to a file
         * tell the user where the file is
         * with a snackbar, which action is SHOW ME
         * */
        val c = Calendar.getInstance()
        val date = "${c.get(Calendar.DAY_OF_MONTH)}_${c.get(Calendar.MONTH)}_${c.get(Calendar.YEAR)}_${c.get(Calendar.HOUR_OF_DAY)}h${c.get(Calendar.MINUTE)}"
        val fileName = "${currentList.toSafeCase()}_$date"
        Toast.makeText(this,"Nom du fichier $fileName", Toast.LENGTH_LONG).show()


    }

    fun addItem(task: Task) {
        tasks.add(0, task) // Ajoute la tâche au début de la liste
        adapter.notifyItemInserted(0)
        scrollToTask(0)
        //adapter.notifyItemRangeChanged(0, tasks.size)
    }

    private fun scrollToTask(position: Int) {
        Log.e("Scroll", "Scroll to position")
        /*(tasksRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
            position,
            50
        )*/
        tasksRecyclerView.smoothScrollToPosition(position)
        //tasksRecyclerView.scrollToPosition(position)
    }

    fun deleteItem(index: Int) { // Called onSwipe
        val taskToDelete = tasks[index] // Get the task to delete

        //tasks.removeAt(index) // Supprimée de la liste de sync

        listEditor.remove(taskToDelete.creationDate.toString()).apply() // Supprimée

        Log.e("Activity", "Delete item at $index")

        Snackbar.make(findViewById(R.id.activity), "Tâche supprimée", Snackbar.LENGTH_LONG)
            .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
            .setAction("Annuler") {
                // Suppression annulée : remettre la tâche
                listEditor.putString(
                    taskToDelete.creationDate.toString(),
                    Json.encodeToString(taskToDelete)
                ).apply()

                tasks.add(index, taskToDelete)
                adapter.notifyItemInserted(index)
                adapter.notifyItemRangeChanged(index, tasks.size)
            }.show()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(tasks, fromPosition, toPosition)

        //val titlesOnly = arrayListOf<String>()
        //tasks.forEach { titlesOnly.add(it.title) }
        //Log.i("tasks ", Gson().toJson(titlesOnly))

        Log.i("Activity position", "from: $fromPosition to: $toPosition")

    }

    fun removeItem(index: Int) {
        val taskDone = tasks[index] // We get the task
        taskDone.done = true
        val taskToJson = Json.encodeToString(taskDone) // Converted to Json for storage
        listEditor.putString(taskDone.creationDate.toString(), taskToJson).apply()

        //tasks.removeAt(index)

        Log.i("Activity", "Remove item at $index")
        Log.i("tasks (activity)", "size after removed : ${tasks.size}")

    }

    fun setTaskDone(task: Task, done: Boolean) {
        task.done = done
        listEditor.putString(task.creationDate.toString(), Json.encodeToString(task)).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //application.setTheme(R.style.Theme_TodolistViolet)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)
        //setSupportActionBar(findViewById(R.id.toolbar)) to show top right menu (:points)


        val fab = findViewById<FloatingActionButton>(R.id.fab)
        //val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        //mSwipeRefreshLayout.isEnabled = false

        /*mSwipeRefreshLayout.setOnRefreshListener {
            // HERE what to do onRefresh

            Handler(Looper.getMainLooper()).postDelayed({
                //Toast.makeText(this,"Refresh cancelled",Toast.LENGTH_SHORT).show()

                mSwipeRefreshLayout.isRefreshing = false

            }, 1500)
        }*/

        // SYNC ALL TASKS (GET) WITH CLOUD FIRESTORE

        tasksRecyclerView = findViewById(R.id.tasks_recyclerview)

        //tasksRecyclerView.isNestedScrollingEnabled = false

        //val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
        //nestedScrollView.isNestedScrollingEnabled = false
       // nestedScrollView.requestDisallowInterceptTouchEvent(true)
        
        allLists = getSharedPreferences("allLists", MODE_PRIVATE)

        allLists.edit().putString("list1","Mes tâches").apply()
        allLists.edit().putString("list2","Aujourd'hui").apply()
        allLists.edit().putString("list3","Demain").apply()

        //allLists.edit().putString("defaultList","list2").apply()

        val listSpinner = findViewById<AppCompatSpinner>(R.id.choiceList)

        val lists = allLists.all

        val list = lists.values.toList() //arrayListOf("List 1","List 2","List 3")
        val listsAdapter = ArrayAdapter(this, R.layout.list_spinner_item,list)
        //listsAdapter.addAll("List 1","List 2","List 3")
        listSpinner.adapter = listsAdapter

        defaultListKey = lists.map { it.key }[0]

        lists.forEach {
            Log.e("List",  "${it.key} > ${it.value}")
            if (it.key == "defaultList") {

                if (allLists.getString(it.value.toString(),defaultListKey) != null) {
                        defaultListKey = allLists.getString(it.value.toString(),defaultListKey).toString()
                        currentList = defaultListKey
                }
            }
        }

        list1 = getSharedPreferences("list1", MODE_PRIVATE)

        //val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        //collapsingToolbar.title = defaultListKey
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "" //defaultListKey

        listEditor = list1.edit()

        val listOfAllTasks = ArrayList<Task>()
        val allEntries = list1.all.map { it.key }

        // PUT ALL ENTRIES TO JSON AND SAVE IT

        Log.i("All Entries", allEntries.toString())
        Log.i("JSON LIST1", Gson().toJson(list1.all.map { it }))
        allEntries.forEach { str ->
            val jsonFromData = list1.getString(str, "Empty")
            jsonFromData?.let { Log.i("KEYS", it) } // Log the key

            if (jsonFromData != null) {
                //Log.i("JSON", jsonFromData)
                val objectTraduit = Json.decodeFromString<Task>(jsonFromData) //Convert to task
                listOfAllTasks.add(objectTraduit)
            }
        }
        Log.i("JSON Entries", Gson().toJson(listOfAllTasks))
        // Here tasks are sorted by date

        //Sort by position instead
        //listOfAllTasks.sortWith { o1, o2 -> o2.creationDate.compareTo(o1.creationDate) }
        listOfAllTasks.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
        listOfAllTasks.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }
        // ArrayList<Task> created by the Object Class Task
        tasks = Task.createTasksList(listOfAllTasks)
        // This part can be better..

        // Here we create the Adapter for the RecyclerView, with the tasks
        adapter = TasksAdapter(tasks, this)

        tasksRecyclerView.adapter = adapter // Adapter assigned to the recyclerview
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        ViewCompat.setNestedScrollingEnabled(tasksRecyclerView,false)

        val helperCallback = ItemTouchHelperCallback(adapter as TasksAdapter, this)
        val helper = ItemTouchHelper(helperCallback)
        helper.attachToRecyclerView(tasksRecyclerView)

        //itemTouchHelper.attachToRecyclerView(tasksRecyclerView)

        /*bottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottomDialogParent))
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.isHideable = true*/

        val bottomDialog = AddTaskFragment()
        bottomDialog.isCancelable = true

        fab.setOnClickListener { view ->
            bottomDialog.show(supportFragmentManager, "dialog")

            // Start an activityForResult instead
        }

        if(intent.hasExtra("shortcut")) {
            bottomDialog.show(supportFragmentManager, "dialog")
        }

        findViewById<View>(R.id.context).setOnClickListener {
            //startActivityForResult(Intent(this, TestActivity::class.java), 200)
            BottomFragment.newInstance(6).show(supportFragmentManager, "dialog")
            //scrollToTask(12)
        }
    }


    override fun onStop() {
        super.onStop()

        //currentList = "list1"

        if (currentList == defaultListKey ) {
            Log.i("Lists","No change, list is $currentList")
        } else {
            allLists.edit().putString("defaultList",currentList).apply()
        }



        // HERE UPDATE POSITION OF EVERY TASK,
        // AND SAVE IT TO PERSISTENT STORAGE
        if (true){
            //val orderCheck= arrayListOf<Int>()

            tasks.forEachIndexed { index, task ->
                task.position = index

                val taskToJson = Json.encodeToString(task)
                listEditor.putString(task.creationDate.toString(), taskToJson).apply()

                //orderCheck.add(task.position)

            }
            //Log.i("Order after", Json.encodeToString(orderCheck))
            Log.i("Order","Saved data with position")
            //  VISUALIZE DATA TO CHECK IF CORRECT
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Log.e("Activity Result", "We got a result !")

        if (requestCode == 500) {
            if (resultCode == Activity.RESULT_OK && data != null) {

                val position = data.getIntExtra("position", -1)
                val taskJson = data.getStringExtra("returnTask")

                if(data.hasExtra("delete")) {
                    val deleteBool = data.getBooleanExtra("delete",false)
                    if (deleteBool){
                        Toast.makeText(this,"Delete",Toast.LENGTH_SHORT).show()
                        // SWIPE TO DELETE AT POSITION

                    }
                }

                if (position != -1 && taskJson != null) {

                    if (!taskJson.isNullOrBlank()) {

                        /*Toast.makeText(
                            this,
                            "Resultat reçu ! Processing data update",
                            Toast.LENGTH_SHORT
                        ).show()*/
                        Log.e("Result", taskJson)

                        val task = Json.decodeFromString<Task>(taskJson)

                        listEditor.putString(
                            task.creationDate.toString(),
                            Json.encodeToString(task)
                        ).apply()

                        tasks[position] = task

                        if (task.done) {

                            val newPosition = tasks.size - 1
                            tasks.removeAt(position)
                            tasks.add(tasks.size, task)
                            adapter.notifyItemMoved(position, newPosition)
                            adapter.notifyItemRangeChanged(position, newPosition + 1)

                            Snackbar.make(
                                findViewById(R.id.activity),
                                "Tâche terminée",
                                Snackbar.LENGTH_LONG
                            )
                                .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
                                .setAction("Annuler") {
                                    Log.e("Action", "Validation annulée")
                                    task.done = false

                                    listEditor.putString(
                                        task.creationDate.toString(),
                                        Json.encodeToString(task)
                                    ).apply()
                                    tasks.removeAt(newPosition)
                                    tasks.add(position, task)

                                    adapter.notifyItemMoved(newPosition, position)
                                    adapter.notifyItemRangeChanged(position, newPosition + 1)

                                }.show()
                        }

                        adapter.notifyItemChanged(position)

                        // AND WE NEED TO SAVE THESE CHANGES :)

                    } else {
                        Log.e("DATA", "TaskJson is Null or Blank")
                    }
                } else {
                    Log.e("DATA", "Position or TaskJson is null on result")
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // DO SMTH
                Log.e("RESULT ERR", "Canceled")
            } else {
                Log.e("RESULT ERR", "Unknown")
            }
        } else {
            Log.e("RESULT", "Unknown request code")
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                //scrollToTask(5)
                //startActivity(Intent(this, TestActivity::class.java))

            }
        }
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}