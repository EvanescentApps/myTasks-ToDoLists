package com.electro.todolist.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
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
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class TasksActivity : AppCompatActivity() {

    private lateinit var tasks: MutableList<Task>

    // tasks : ArrayList qui sert à sync les données, gérée par l'activité
    // mTasks : ArrayList qui sert à l'affichage de la recyclerView uniquement
    private lateinit var adapter: RecyclerView.Adapter<TasksAdapter.ViewHolder>
    private lateinit var selectedListContent: SharedPreferences
    private lateinit var selectedListEditor: SharedPreferences.Editor
    private lateinit var b: ActivityTasksBinding
    private lateinit var tasksRepository: TasksRepository

    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var scrollListener: RecyclerView.OnScrollListener

    //private var allTasksList = mutableListOf<Task>()

    private val Context.dataStoreSettings by preferencesDataStore(name = "settings",)

    /*private val Context.dataStoreAllLists by preferencesDataStore(
        name = "allLists",
        *//*produceMigrations = { context ->
            listOf(SharedPreferencesMigration(context, "allLists"))
        }*//*
    )*/

    @Suppress("unused")
    private suspend fun save(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        dataStoreSettings.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    @Suppress("PropertyName")
    val COUNTER = intPreferencesKey("counter")

    @Suppress("unused")
    suspend fun incrementCounter() {
        dataStoreSettings.edit { settings ->
            val currentCounterValue = settings[COUNTER] ?: 0
            settings[COUNTER] = currentCounterValue + 1
        }
    }

    private fun getIntFlow(KEY: Preferences.Key<Int>): Flow<Int> {
        /*dataStore.data.map {
            val all = it.asMap().values as MutableList<String>
        }*/
        return dataStoreSettings.data
            .map { preferences ->
                preferences[KEY] ?: 0
            }
    }

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

        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())
    }

    fun getCurrentListName(): String = tasksRepository.currentListName

    @SuppressLint("NotifyDataSetChanged")
    private fun addAllTasks(listToAdd: ArrayList<Task>) {
        tasks.addAll(listToAdd)
        adapter.notifyDataSetChanged()

        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())
    }

    fun scrollToTask(position: Int) {
        b.includeRecycler.tasksRecyclerview.smoothScrollToPosition(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun deleteAllDoneTasks() {
        val tasksToRemove = ArrayList<Task>()
        tasks.forEach { task ->

            if (task.done) {
                selectedListEditor.remove(task.creationDate.toString()) // Supprimée

                tasksToRemove.add(task)
            }
        }

        tasks.removeAll(tasksToRemove)
        adapter.notifyDataSetChanged()
        selectedListEditor.apply()

        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())
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
                selectedListEditor.putString(taskToDelete.creationDate.toString(), Json.encodeToString(taskToDelete)).apply()

                tasks.add(index, taskToDelete)
                adapter.notifyItemInserted(index)
                scrollToTask(index)
            }.show()

        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setEmptyState(enabled: Boolean) {
        b.includeRecycler.emptyTasks.apply {
            visibility = if (enabled) View.VISIBLE else View.GONE
            translationY = 0F
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(tasks, fromPosition, toPosition)
        Log.i("Activity position", "from: $fromPosition to: $toPosition")
    }

    fun setTaskDone(task: Task, done: Boolean = true) {
        task.done = done
        selectedListEditor.putString(task.creationDate.toString(), Json.encodeToString(task)).apply()
    }

    fun setSwipeRefreshEnabled(isEnabled: Boolean = true) {
        b.swipeRefresh.isEnabled = isEnabled
    }

    fun updateListName(newName: String) {
        b.toolbarLayout.title = newName
    }

    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("CommitPrefEdits")
    fun changeList(newSelectedList: String, emptyState: Boolean = true) {

        tasksRepository.currentListName = newSelectedList

        b.toolbarLayout.title = tasksRepository.getListGroup()[newSelectedList].toString()

        Log.i("List", "Changed to ${tasksRepository.currentListName}")
        selectedListContent = getSharedPreferences(tasksRepository.currentListName, MODE_PRIVATE)
        selectedListEditor = selectedListContent.edit()

        /*val testList = mutableListOf<Task>()
        selectedListContent.all.forEach { entry ->
            selectedListContent.getString(entry.key, null)?.let { taskJson ->
                try {
                    testList.add(Json { ignoreUnknownKeys = true }.decodeFromString(Task.serializer(), taskJson)) //Decode to task & add
                } catch (e: Exception) { e.printStackTrace()
                    try {
                        val parsedJson = Gson().fromJson(taskJson, JsonObject::class.java)
                        parsedJson.remove("priority")
                        testList.add(Json { ignoreUnknownKeys = true }.decodeFromString(Task.serializer(), parsedJson.toString()))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }*/
        /*val getTasksList = mutableListOf<Task>()
        selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let { taskJson ->
                try {
                    getTasksList.add(Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString(Task.serializer(), taskJson)) //Decode to task & add
                } catch (e: Exception) {
                    Log.e("JSON", "Deserialization Error with : $taskJson")
                    Toast.makeText(this, "Une erreur est survenue, elle est en cours de résolution en arrière-plan", Toast.LENGTH_LONG).show()
                    e.printStackTrace()

                    try {
                        val parsedJson = Gson().fromJson(taskJson, JsonObject::class.java)
                        //val parsedJson2 = Json.decodeFromString<JsonObject>(taskJson)

                        Log.e("Property Priority", parsedJson["priority"].toString())
                        parsedJson.remove("priority")
                        Log.e("SecuredJson", parsedJson.toString())
                        getTasksList.add(Json { ignoreUnknownKeys = true }.decodeFromString(Task.serializer(), parsedJson.toString()))

                        Toast.makeText(this, "Erreur résolue, tous les fichiers sont corrects", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("Double error", "Error with json handling")
                        e.printStackTrace()
                    }
                }
            }
        }*/

        tasks = selectedListContent.all.map { entry ->
            selectedListContent.getString(entry.key, null)?.let { taskJson ->
                try { json.decodeFromString(Task.serializer(), taskJson) }
                catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        val parsedJson = Gson().fromJson(taskJson, JsonObject::class.java)
                        parsedJson.remove("priority")
                        json.decodeFromString(Task.serializer(), parsedJson.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Task("Erreur de décryptage de la tâche...", creationDate = System.currentTimeMillis(), done = true)
                    }
                }
            } ?: Task("Tâche vide...", creationDate = System.currentTimeMillis(), done = true)
        }.toMutableList()

        tasks.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
        tasks.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }

        // ArrayList<Task> created by the Object Class Task
        //tasks = allTasksList // as ArrayList<Task>
        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())

        if (emptyState) {
            val settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE)
            val firstStart = settingsPrefs.getBoolean("firstStart", true)

            if (tasks.isEmpty() && firstStart) {
                tasks.addAll(Task.emptyState())
                settingsPrefs.edit().putBoolean("firstStart", false).apply()
            }
        }

        adapter = TasksAdapter(tasks, this, this)
        b.includeRecycler.tasksRecyclerview.swapAdapter(adapter, true)

        itemTouchHelper.attachToRecyclerView(null)
        itemTouchHelperCallback = ItemTouchHelperCallback(adapter as TasksAdapter, this)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(b.includeRecycler.tasksRecyclerview)
    }

    @SuppressLint("CommitPrefEdits", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        //setTheme(R.style.glow)
        super.onCreate(savedInstanceState)

        b = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(b.root)

        getIntFlow(COUNTER).asLiveData().observe(this) {
            Log.e("INT", "$it")
            //Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()
        }

        try {
            GlobalScope.launch(Dispatchers.IO) {
                val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val localIP = formatIpAddress(wifiManager.connectionInfo.ipAddress)
                val publicIP = getPublicIPAddress()?.trim()

                Log.i("IP ADDRESS", "Local IP : $localIP - Public IP : $publicIP")

                launch(Dispatchers.Main) {
                    //Toast.makeText(this@TasksActivity, "Ip address : $localIP ip2 : $publicIP", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // TODO : MIGRATE TO DATASTORE FOR PREFERENCES
        // TODO : MIGRATE TO ROOM FOR TASKS STORAGE
        // TODO : create viewmodel instance and pass repository

        // TODO : SYNC ALL TASKS (GET) WITH CLOUD FIRESTORE

        tasksRepository = TasksRepository(this)
        tasksRepository.getDefaultList()

        // Here sending all the lists to the spinner
        val listClean = tasksRepository.getListGroup() //tasksRepository.list.toMutableList()

        if (listClean.containsKey("defaultList")) listClean.remove("defaultList")

        b.toolbar.title = listClean[tasksRepository.currentListName].toString()

        Log.i("Selected List", "${tasksRepository.currentListName} selected among $listClean")

        selectedListContent = getSharedPreferences(tasksRepository.currentListName, MODE_PRIVATE)
        selectedListEditor = selectedListContent.edit()

        /*selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let {
                try {
                    allTasksList.add(Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString(Task.serializer(), it)) //Decode to task & add
                } catch (e: Exception) {
                    Log.e("Error decode", "deserialization error : ${e.stackTraceToString()}")
                    Toast.makeText(
                        this,
                        "Une erreur est survenue, elle est en cours de résolution en arrière-plan",
                        Toast.LENGTH_SHORT
                    ).show()

                    val jsonToCorrect = it
                    Log.e("JSON BUG", jsonToCorrect)

                    try {
                        val parsedJson = Gson().fromJson(jsonToCorrect, JsonObject::class.java)

                        Log.e("Property Priority", parsedJson["priority"].toString())

                        parsedJson.remove("priority")

                        Log.e("SecuredJson", parsedJson.toString())

                        allTasksList.add(Json {
                            ignoreUnknownKeys = true
                        }.decodeFromString(Task.serializer(), parsedJson.toString()))

                        Toast.makeText(
                            this,
                            "Erreur résolue, tous les fichiers sont corrects",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {
                        Log.e("Double error", "Error with json handling")
                        Toast.makeText(this, "L'erreur n'a pas pu être résolue", Toast.LENGTH_SHORT)
                            .show()
                        e.printStackTrace()
                    }

                    e.printStackTrace()
                }
            }
        }*/

        // TODO : DO THIS ASYNC
        // TODO : FIRST CONVERT TO DATASAVED

        tasks = selectedListContent.all.map { entry ->
            selectedListContent.getString(entry.key, null)?.let { taskJson ->
                try {
                    json.decodeFromString(Task.serializer(), taskJson)
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        val parsedJson = Gson().fromJson(taskJson, JsonObject::class.java)
                        parsedJson.remove("priority")
                        json.decodeFromString(Task.serializer(), parsedJson.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Task("Erreur de décryptage de la tâche...", creationDate = System.currentTimeMillis(), done = true)
                    }
                }
            } ?: Task("Tâche vide...", creationDate = System.currentTimeMillis(), done = true)
        }.toMutableList()

        tasks.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
        tasks.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }

        //tasks = allTasksList
        Log.e("tasks",tasks.toString())
        setEmptyState(tasks.isEmpty())

        val settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        val firstStart = settingsPrefs.getBoolean("firstStart", true)

        if (tasks.isEmpty() && firstStart) {
            tasks.addAll(Task.emptyState())

            settingsPrefs.edit().putBoolean("firstStart", false).apply()
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

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                var anim = true

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val view = b.includeRecycler.emptyTasks

                    val initialHeight = view.measuredHeight

                    val layoutParamsInitial = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        initialHeight
                    )
                    view.layoutParams = layoutParamsInitial

                    view.animate()
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setDuration(500L)
                        .translationY(-1000F)

                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                view.visibility = View.GONE
                                view.translationY = 0F
                            }
                        })

                    /*val valueAnimator = ValueAnimator.ofInt(view.measuredHeight, 0)
                    valueAnimator.duration = 500L
                    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
                    valueAnimator.addUpdateListener {
                        val animatedValue = valueAnimator.animatedValue as Int
                        val layoutParams = view.layoutParams
                        layoutParams.height = animatedValue
                        view.layoutParams = layoutParams
                    }

                    valueAnimator.doOnEnd {
                        //view.visibility = View.GONE

                        if (anim) {
                            anim = false
                            valueAnimator.reverse()
                        }

                        view.layoutParams = layoutParamsInitial

                        //view.invalidate()
                    }

                    valueAnimator.start()*/

                }
            }
        }

        b.includeRecycler.tasksRecyclerview.addOnScrollListener(scrollListener)

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

            GlobalScope.launch {
                incrementCounter()
            }
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
            userLists = tasksRepository.getListGroup()

            if (userLists.containsKey("defaultList")) userLists.remove("defaultList")

            @Suppress("UNCHECKED_CAST")
            userListsSerialized = Json.encodeToString(userLists.toList() as? List<Pair<String, String>>)
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

        b.includeRecycler.parentRecyclerV.setOnTouchListener { _, _ ->

            b.bottomAppBar.performShow()
            true
        }
    }

    override fun onStop() {
        super.onStop()

        // save the last opened list
        Log.i("Lists", "DefaultList : ${tasksRepository.currentListName}")
        tasksRepository.getListGroupPrefs().edit().putString("defaultList", tasksRepository.currentListName).apply()

        // TODO : DO THIS WORK ASYNC

        /* Here we save the position of each task */
        tasks.forEachIndexed { index, task ->
            task.position = index
            selectedListEditor.putString(task.creationDate.toString(), Json.encodeToString(task))
        }
        selectedListEditor.apply()
        Log.i("onStop", "Ordered list saved")
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @SuppressLint("ShowToast")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        b.bottomAppBar.performShow()

        Log.e("Activity Result", "We got a result !")

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) { // File created, write it
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                Toast.makeText(this, "the saved uri is $uri", Toast.LENGTH_LONG).show()

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

                tasksRepository.writeTaskListTofile(tasksRepository.currentListName, uri)
                // Perform operations on the document using its URI.
            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) { // File imported
            resultData?.data?.also { uri ->

                val newList = tasksRepository.readTextContent(uri)

                val parsedTasks = Json.decodeFromString<ArrayList<Task>>(newList)
                parsedTasks.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
                parsedTasks.sortWith { o1, o2 -> o1.done.compareTo(o2.done) }

                Log.i("Import", "List imported and read successfully : $parsedTasks")

                tasksRepository.createList("Liste Importée", false) // AUTO SWITCH TO IT OK
                addAllTasks(parsedTasks)

                parsedTasks.forEachIndexed { index, task ->
                    task.position = index
                    selectedListEditor.putString(task.creationDate.toString(), Json.encodeToString(Task.serializer(), task)) // Gson().toJson(task)
                }
                selectedListEditor.apply()

                // create tasksRepository.addAllTasks(listToAdd) ??
                // Perform operations on the document using its URI.
            }
        } else if (requestCode == 500 && resultCode == Activity.RESULT_OK && resultData != null) { // Task modified
            //Toast.makeText(this, "Tâche Modifiée", Toast.LENGTH_SHORT).show()
            val position = resultData.getIntExtra("position", -1)
            val taskJson = resultData.getStringExtra("returnTask")

            val deleteBool = resultData.getBooleanExtra("delete", false)
            if (deleteBool) {
                deleteItem(position)
            } else if (position != -1 && !taskJson.isNullOrBlank()) { // UPDATE TASK
                val task = Json.decodeFromString<Task>(taskJson) // UPDATED TASK

                selectedListEditor.putString(task.creationDate.toString(), taskJson).apply()
                // TASK SAVED ON TASKSDETAILSACTIVITY

                tasks[position] = task

                if (task.done) {

                    val placeEnd = true
                    val newPosition = if (placeEnd) tasks.size - 1 else 0
                    tasks.removeAt(position)

                    val addPlace = if (placeEnd) tasks.size else 0
                    tasks.add(addPlace, task)

                    adapter.notifyItemMoved(position, newPosition)

                    Snackbar.make(findViewById(R.id.activity), "Tâche terminée", Snackbar.LENGTH_LONG)
                        .setAnchorView(findViewById<FloatingActionButton>(R.id.fab))
                        .setAction("Annuler") {
                            task.done = false

                            selectedListEditor.putString(task.creationDate.toString(), Json.encodeToString(task)).apply()
                            tasks.removeAt(newPosition)
                            tasks.add(position, task)

                            adapter.notifyItemMoved(newPosition, position)
                        }.show()
                }
                adapter.notifyItemChanged(position)
            } else Log.e("DATA", "Position or TaskJson is null on result")
        } else Log.e("RESULT", "Unknown request code")

        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun getPublicIPAddress(): String? {
        var value: String? = null
        val es = Executors.newSingleThreadExecutor()
        val result = es.submit<String?> {
            try {
                val url = URL("https://api.my-ip.io/ip.txt")
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                    val r = BufferedReader(InputStreamReader(`in`))
                    val total = StringBuilder()
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        total.append(line).append('\n')
                    }
                    urlConnection.disconnect()
                    return@submit total.toString()
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: IOException) {
                Log.e("Public IP: ", e.message!!)
            }
            null
        }
        try {
            value = result.get()
        } catch (e: java.lang.Exception) {
            // failed
            e.printStackTrace()
        }
        es.shutdown()
        return value
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