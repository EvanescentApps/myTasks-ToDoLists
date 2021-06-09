package com.electro.todolist.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electro.todolist.ItemTouchHelperCallback
import com.electro.todolist.R
import com.electro.todolist.data.Task
import com.electro.todolist.data.TasksRepository
import com.electro.todolist.databinding.ActivityTasksBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList


class TasksActivity : AppCompatActivity() {

    private lateinit var tasks: ArrayList<Task>
    // tasks : ArrayList qui sert à sync les données, gérée par l'activité
    // mTasks : ArrayList qui sert à l'affichage de la recyclerView uniquement
    private lateinit var adapter: RecyclerView.Adapter<TasksAdapter.ViewHolder>
    private lateinit var selectedListContent: SharedPreferences
    private lateinit var selectedListEditor: SharedPreferences.Editor
    private lateinit var b: ActivityTasksBinding
    private lateinit var tasksRepository: TasksRepository

    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var itemTouchHelper: ItemTouchHelper
    @Suppress("MemberVisibilityCanBePrivate")
    val allTasksList = ArrayList<Task>()

    @Suppress("MemberVisibilityCanBePrivate")
    val Context.dataStore by preferencesDataStore(name = "settings")

    @Suppress("unused")
    fun updatedTask(task: Task, position: Int) {
        tasks[position] = task
        adapter.notifyItemChanged(position)
    }

    fun addItem(task: Task) {
        tasks.add(0, task) // Ajoute la tâche au début de la liste
        adapter.notifyItemInserted(0)

        scrollToTask(0)
        adapter.notifyItemChanged(0)

        setEmptyState(tasks.isEmpty())
    }

    fun scrollToTask(position: Int) {
        Log.e("Scroll", "Scroll to position")

        b.includeRecycler.tasksRecyclerview.smoothScrollToPosition(position)
    }

    @SuppressLint("ShowToast")
    fun deleteItem(index: Int) { // Called onSwipe
        val taskToDelete = tasks[index] // Get the task to delete

        selectedListEditor.remove(taskToDelete.creationDate.toString()).apply() // Supprimée

        adapter.notifyItemRemoved(index)

        tasks.removeAt(index)

        Log.e("Activity", "Delete item at $index")

        Snackbar.make(b.activity, "Tâche supprimée", Snackbar.LENGTH_LONG)
            .setAnchorView(b.fab)
            .setAction("Annuler") {
                // Suppression annulée : remettre la tâche
                selectedListEditor.putString(
                    taskToDelete.creationDate.toString(),
                    Json.encodeToString(taskToDelete)
                ).apply()

                tasks.add(index, taskToDelete)
                adapter.notifyItemInserted(index)

                scrollToTask(index)
                adapter.notifyItemChanged(index)

                adapter.notifyItemRangeChanged(index, tasks.size)
            }.show()

        setEmptyState(tasks.isEmpty())
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setEmptyState(enabled: Boolean) {
        b.includeRecycler.emptyTasks.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(tasks, fromPosition, toPosition)
        Log.i("Activity position", "from: $fromPosition to: $toPosition")
    }

    fun setTaskDone(task: Task, done: Boolean) {
        task.done = done
        selectedListEditor.putString(task.creationDate.toString(), Json.encodeToString(task)).apply()
    }

    @Suppress("unused")
    private suspend fun save(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        dataStore.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    @Suppress("PropertyName")
    val COUNTER = intPreferencesKey("counter")


    @Suppress("unused")
    suspend fun incrementCounter() {
        this.dataStore.edit { settings ->
            val currentCounterValue = settings[COUNTER] ?: 0
            settings[COUNTER] = currentCounterValue + 1
        }
    }

    fun setSwipeRefreshEnabled(isEnabled : Boolean = true) {
        b.swipeRefresh.isEnabled = isEnabled
    }

    fun updateListName(newName : String) {
        b.toolbarLayout.title = newName
    }

    @SuppressLint("CommitPrefEdits")
    fun changeList(newSelectedList : String) {
        // Notify recyclerView to change dataset

        tasksRepository.currentListName = newSelectedList

        b.toolbarLayout.title = tasksRepository.getListGroup()[newSelectedList].toString()

        Log.e("List changed","new list is ${tasksRepository.currentListName}")
        selectedListContent = getSharedPreferences(tasksRepository.currentListName, MODE_PRIVATE)
        selectedListEditor = selectedListContent.edit()

        val getTasksList = ArrayList<Task>()
        selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let {
                try {
                    getTasksList.add(Json { ignoreUnknownKeys = true}.decodeFromString(Task.serializer(),it) ) //Decode to task & add
                } catch (e: Exception) {
                    Log.e("TASK","Serialization Error...")
                    Toast.makeText(this,"Une erreur est survenue, elle est en cours de résolution en arrière-plan",Toast.LENGTH_LONG).show()
                    e.printStackTrace()

                    Log.e("JSON BUG", it)
                }
            }
        }
        getTasksList.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
        getTasksList.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }

        // ArrayList<Task> created by the Object Class Task
        tasks = getTasksList

        setEmptyState(tasks.isEmpty())

        if (tasks.isNullOrEmpty()) {
            Log.i("TaskList!!","isNull or Empty : $tasks")
            tasks = Task.emptyState()
            setEmptyState(true)
        }

        // Just replace the data
        // Here we create the Adapter for the RecyclerView, with the tasks
        adapter = TasksAdapter(tasks, this, this)
        b.includeRecycler.tasksRecyclerview.swapAdapter(adapter, true)

        itemTouchHelper.attachToRecyclerView(null)
        itemTouchHelperCallback = ItemTouchHelperCallback(adapter as TasksAdapter, this)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(b.includeRecycler.tasksRecyclerview)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        //setTheme(R.style.glow)
        super.onCreate(savedInstanceState)

        b = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(b.root)

        // TODO : create viewmodel instance and pass repository

        // TODO : SYNC ALL TASKS (GET) WITH CLOUD FIRESTORE

        tasksRepository = TasksRepository(this)
        tasksRepository.getDefaultList()

        // Here sending all the lists to the spinner
        val listClean = tasksRepository.getListGroup() //tasksRepository.list.toMutableList()

        if (listClean.containsKey("defaultList")) { listClean.remove("defaultList") }

        //val listOfNames = listClean.keys

        //b.choiceList.adapter = ArrayAdapter(this, R.layout.list_spinner_item, listClean.values.toList())
        //b.choiceList.setSelection(listOfNames.indexOf(tasksRepository.currentListName))

        b.toolbar.title = listClean[tasksRepository.currentListName].toString()

        Log.e("lists", listClean.toString())
        Log.e("Selected List", "${tasksRepository.currentListName} selected")

        // DO THIS WORK ASYNCHRONOUSLY: COROUTINE
        selectedListContent = getSharedPreferences(tasksRepository.currentListName, MODE_PRIVATE)

        selectedListEditor = selectedListContent.edit()

        selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let {
                try  {
                    allTasksList.add(Json { ignoreUnknownKeys = true }.decodeFromString(Task.serializer(),it)) //Decode to task & add
                } catch ( e : Exception) {
                    Log.e("Error decode","deserialization error : ${e.stackTraceToString()}")
                    Toast.makeText(this,"Une erreur est survenue, elle est en cours de résolution en arrière-plan",Toast.LENGTH_LONG).show()

                    // TODO : TRAITER LE JSON POUR REMPLACER LES PROPERTIES
                    val jsonToCorrect = it
                    Log.e("JSON BUG", it)

                    try {
                        val parsedJson = Gson().fromJson(jsonToCorrect, JsonElement::class.java)
                        parsedJson.asString

                        Log.e("Alternate JSON parsing", parsedJson.toString())
                    } catch (e:Exception) {

                    }

                    e.printStackTrace()
                }
            }
        }
        allTasksList.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
        allTasksList.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }

        tasks = allTasksList

        setEmptyState(tasks.isEmpty())

        if (tasks.isNullOrEmpty()) {
            Log.i("TaskList!!","isNull or Empty : $tasks")
            tasks = Task.emptyState()
            setEmptyState(true)
        }

        // Here we create the Adapter for the RecyclerView, with the tasks
        adapter = TasksAdapter(tasks, this, this)

        b.includeRecycler.tasksRecyclerview.adapter = adapter
        b.includeRecycler.tasksRecyclerview.layoutManager = LinearLayoutManager(this)
        //ViewCompat.setNestedScrollingEnabled(b.includeRecycler.tasksRecyclerview, false)
        // This line caused UI bugs onScroll
        itemTouchHelperCallback = ItemTouchHelperCallback(adapter as TasksAdapter, this)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(b.includeRecycler.tasksRecyclerview)

        val bottomDialog = AddTaskFragment()
        bottomDialog.isCancelable = true

        /*val obj = PronoteConnection("https://0141164p.index-education.net/pronote/eleve.html")
        obj.login("COCAIN", "Eclair14")

        val cal = Calendar.getInstance()
        val currentWeekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        Log.e("Week nb","Current week nb is $currentWeekNumber")
        val homework = obj.getHomeworkList(currentWeekNumber)

        Log.e("WORK","homework is $homework")*/

        b.swipeRefresh.isEnabled = false
        b.swipeRefresh.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                b.swipeRefresh.isRefreshing = false
            }, 1100)
        }

        b.fab.setOnClickListener {
            AddTaskFragment.newInstance(tasksRepository.currentListName)
                .show(supportFragmentManager, "dialog")
            // Start an activityForResult instead
        }

        b.includeRecycler.addTask.setOnClickListener {
            AddTaskFragment.newInstance(tasksRepository.currentListName)
                .show(supportFragmentManager, "dialog")
        }

        if (intent.hasExtra("shortcut")) {
            AddTaskFragment.newInstance(tasksRepository.currentListName)
                .show(supportFragmentManager, "dialog")
        }

        findViewById<View>(R.id.contextItem).setOnClickListener {
            //startActivityForResult(Intent(this, TestActivity::class.java), 200)
            BottomFragment.newInstance(tasksRepository.currentListName)
                .show(supportFragmentManager, "dialog")
        }

        var userLists: MutableMap<String, *>
        var userListsSerialized: String

        fun showListsBottomSheet() {
            // THIS IS WHAT IS SHOWN: LIST OF ALL LISTS. NEED TO STAY UP TO DATE
            userLists =  tasksRepository.getListGroup()

            if (userLists.containsKey("defaultList")) { userLists.remove("defaultList") }

            @Suppress("UNCHECKED_CAST")
            userListsSerialized = Json.encodeToString(userLists.toList() as? List<Pair<String,String>>)
            ChangeListFragment.newInstance(tasksRepository.currentListName, userListsSerialized)
                .show(supportFragmentManager, "dialog")
        }

        b.bottomAppBar.setNavigationOnClickListener {
            showListsBottomSheet()
        }

        b.toolbarLayout.setOnClickListener {
            showListsBottomSheet()
        }

        b.appBar.setOnClickListener {
            showListsBottomSheet()
        }
        b.toolbar.setOnClickListener {
            showListsBottomSheet()
        }
    }

    override fun onStop() {
        super.onStop()

        if (tasksRepository.currentListName == tasksRepository.lastOpenedList_Key) {
            Log.i(
                "Lists",
                "No change, current ${tasksRepository.currentListName}, default ${tasksRepository.lastOpenedList_Key}"
            )
        } else { // save the last opened list
            Log.i(
                "Lists",
                "DefaultList changed :  ${tasksRepository.currentListName}, default ${tasksRepository.lastOpenedList_Key}"
            )
            tasksRepository.getListGroupPrefs().edit()
                .putString("defaultList", tasksRepository.currentListName).apply()
        }


        Log.e("AllLists User", tasksRepository.getListGroupPrefs().all.toString())

        // HERE UPDATE POSITION OF EVERY TASK,
        // AND SAVE IT TO PERSISTENT STORAGE

        // TODO : DO THIS WORK ASYNC

        tasks.forEachIndexed { index, task ->
            task.position = index

            val taskToJson = Json.encodeToString(task)
            selectedListEditor.putString(task.creationDate.toString(), taskToJson).apply()
        }
        Log.i("onStop", "Ordered list saved")
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ShowToast")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        Log.e("Activity Result", "We got a result !")

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) { // File created, write it
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                Toast.makeText(this, "the saved uri is $uri", Toast.LENGTH_LONG).show()
                tasksRepository.writeTaskListTofile(tasksRepository.currentListName, uri)
                // Perform operations on the document using its URI.
            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) { // File imported
            resultData?.data?.also { uri ->

                // TODO : CREATE NEW LIST WITH THESE TASKS AND TITLE : LISTE IMPORTEE

                val newList = tasksRepository.readTextContent(uri)
                Log.i("Import", "Imported successfully list $newList")
                // TODO : PARSE CONTENT TO TASKS
                val parsedTasks = Json.decodeFromString<ArrayList<Task>>(newList)

                Log.d("parsed json", parsedTasks.toString())
                // Perform operations on the document using its URI.
            }
        } else if (requestCode == 500 && resultCode == Activity.RESULT_OK && resultData != null) { // Task modified

            val position = resultData.getIntExtra("position", -1)
            val taskJson = resultData.getStringExtra("returnTask")

            if (resultData.hasExtra("delete")) {
                val deleteBool = resultData.getBooleanExtra("delete", false)
                if (deleteBool) {
                    Toast.makeText(this, "Tâche supprimée", Toast.LENGTH_SHORT).show()
                    // SWIPE TO DELETE AT POSITION
                    deleteItem(position)
                }
            }

            if (position != -1 && taskJson != null) {

                if (!taskJson.isNullOrBlank()) {

                    Log.e("Result", taskJson)

                    val task = Json.decodeFromString<Task>(taskJson)

                    // updating result
                    selectedListEditor.putString(
                        task.creationDate.toString(),
                        taskJson
                    ).apply()

                    tasks[position] = task

                    if (task.done) {

                        val newPosition = tasks.size - 1
                        tasks.removeAt(position)
                        tasks.add(tasks.size, task)

                        adapter.notifyItemMoved(position, newPosition)
                        //adapter.notifyItemRangeChanged(position, newPosition + 1)

                        Snackbar.make(
                            findViewById(R.id.activity),
                            "Tâche terminée",
                            Snackbar.LENGTH_LONG
                        )
                            .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
                            .setAction("Annuler") {
                                Log.e("Action", "Validation annulée")
                                task.done = false

                                selectedListEditor.putString(
                                    task.creationDate.toString(),
                                    Json.encodeToString(task)
                                ).apply()
                                tasks.removeAt(newPosition)
                                tasks.add(position, task)

                                adapter.notifyItemMoved(newPosition, position)
                                //adapter.notifyItemRangeChanged(position, newPosition + 1)

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
        } else {
            Log.e("RESULT", "Unknown request code")
        }

        super.onActivityResult(requestCode, resultCode, resultData)
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