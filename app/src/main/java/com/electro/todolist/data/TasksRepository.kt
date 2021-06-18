@file:Suppress("PropertyName")

package com.electro.todolist.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.electro.todolist.ui.TasksActivity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TasksRepository(private val activity: Activity) {

    @Suppress("MemberVisibilityCanBePrivate")
    fun String.toSafeCase(): String = Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
            .filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ", "_")

    // Shared Preferences directory containing all the user's lists as Key Value Pairs (id, name)
    private var listsPrefs: SharedPreferences = activity.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)
    var lastOpenedList_Key = ""
    var currentListName = ""

    //var currentListName = lastOpenedList_Key // Important, needs to be up to date

    // Getting all the lists as a MutableMap (read only)
    private var userListGroup: MutableMap<String, *> = listsPrefs.all
    //var listOfIds = userListGroup.values.toList() //arrayListOf("List 1","List 2","List 3")
    //var userLists : List<Pair<String,String>> = (userListGroup.toList()) as List<Pair<String,String>>

    fun getListGroup(): MutableMap<String, *> = getListGroupPrefs().all

    fun timestampToDuration(timestamp : Long): String {

        val taskDurationSec = (timestamp/1000).toInt()

        val hours = (taskDurationSec/ 3600 ) % 24
        val minutes = (taskDurationSec / 60) % 60
        val seconds = taskDurationSec % 60

        var durationText = ""
        if (hours != 0) {
            durationText += "${hours}h"
        }
        if (minutes != 0) {
            var space = ""
            if (durationText.isNotEmpty()) space = " "

            durationText += "${space}${minutes} min"
        }
        if (seconds != 0) {
            var space = ""
            if (durationText.isNotEmpty()) space = " "

            durationText += "${space}${seconds}s"
        }
        return durationText
    }

    fun timestampToDate(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val daysOfWeek = listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
        val monthsOfYear = listOf("Janvier", "Février", "Mars","Avril","Mai","Juin","Juillet", "Août", "Septembre", "Octobre","Novembre","Décembre")

        val currentCal = Calendar.getInstance()
        currentCal.timeInMillis = System.currentTimeMillis()

        val today = currentCal.get(Calendar.DAY_OF_YEAR)
        val day = when(cal.get(Calendar.DAY_OF_YEAR)) {
            today - 2 -> "Avant-Hier"
            today -1 -> "Hier"
            today -> "Aujourd'hui"
            today + 1 -> "Demain"
            today + 2 -> "Après-Demain"
            else -> "${daysOfWeek[cal.get(Calendar.DAY_OF_WEEK)-1].take(3)}. ${cal.get(Calendar.DAY_OF_MONTH)} ${monthsOfYear[cal.get(Calendar.MONTH)]}"
        }

        return "$day à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)}"
    }

    fun getDefaultList() {

        userListGroup = getListGroup()

        if(userListGroup.isNotEmpty()) {

            listsPrefs.getString("defaultList", null)?.let {
                lastOpenedList_Key = it
            } ?: run { lastOpenedList_Key = userListGroup.map { it.key }[0] }

            Log.e("defaultListKey","is $lastOpenedList_Key , but ${listsPrefs.getString("defaultList", null)}")
        } else {
            // Creating a list because empty
            Log.e("Empty ListsPrefs","Creating -Mes Tâches- List")
            putNewLists()
            lastOpenedList_Key = "list1"
            //getUpdatedLists()
        }

        currentListName = lastOpenedList_Key

        //Toast.makeText(activity,"CurrentList is $currentListName",Toast.LENGTH_SHORT).show()
    }

    fun getListGroupPrefs() : SharedPreferences = activity.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)

    @Suppress("MemberVisibilityCanBePrivate")
    fun putNewLists() {
        getListGroupPrefs().edit().apply {
            putString("list1", "Mes tâches")
            putString("list2", "Aujourd'hui")
            putString("list3", "Demain")
            putString("defaultList","list1")
        }.apply()
    }

    fun createList(name : String, emptyState: Boolean = true) {
        val newListId = Task.generateId(6)
        getListGroupPrefs().edit().putString(newListId, name).apply()
        if (activity is TasksActivity) activity.changeList(newListId, emptyState)
    }

    fun renameList(newName: String, listId: String) {
        val listsPrefs = getListGroupPrefs()
        if (listsPrefs.getString(listId,null) != null) {
            listsPrefs.edit().putString(listId, newName).apply()
            if (activity is TasksActivity) activity.updateListName(newName)
        } else {
            Toast.makeText(activity,"Cette liste n'existe pas... Veuillez réessayer.",Toast.LENGTH_LONG).show()
        }
    }

    fun deleteList(listId: String) {

        try {
            getListGroupPrefs().edit().remove(listId).apply()
        } catch (e: Exception) {
            Log.e("Error removing list", e.stackTrace.toString())
        }

        val listOfIds = getListGroup().toList()

        if (activity is TasksActivity) activity.changeList(listOfIds[0].first)
        currentListName = listOfIds[0].first

        //Toast.makeText(activity, "Liste supprimée, liste par défaut : ${listOfIds[0].first}", Toast.LENGTH_LONG).show()
    }

    fun deleteAllDoneTasks() {

        if (activity is TasksActivity) activity.deleteAllDoneTasks()

        /*val allTasksList = ArrayList<Task>()
        selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let {
                allTasksList.add(Json.decodeFromString(it)) //Decode to task & add
            }
        }

        // TODO : GET THE RIGHT LIST...

        var isError = false

        allTasksList.forEachIndexed { index, task ->
                if (task.done) {

                    // TODO : MAKE THIS WORK

                   *//* try {
                        selectedListContent.edit().remove(task.creationDate.toString()).apply()
                    } catch (e:Exception) {
                        Log.e("Error Tasks Removal",e.stackTrace.toString())
                        isError = true
                    }*//*
                }
        }*/

        Toast.makeText(activity, "Tâches terminées supprimées ✔", Toast.LENGTH_SHORT).show()
    }

    fun exportToFile(currentList: String) {
        /* snackbar, which action is SHOW ME */
        createFile("${currentList.toSafeCase()}-${getDate()}")
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createFile(fileName: String, pickerInititalUri: Uri? = null) {
        try {
            val intentCreateDoc = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                val mimetypes = arrayOf("application/txt", "text/*")
                putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                putExtra(Intent.EXTRA_TITLE, "$fileName.txt")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pickerInititalUri?.let {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI,it)
                    }
                }
            }
            Toast.makeText(activity, "Fichier $fileName.txt créé", Toast.LENGTH_LONG).show()
            activity.startActivityForResult(intentCreateDoc,1)
            Log.i("Tasks to file", "Waiting for a result (user choosing directory)")
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun writeTaskListTofile(currentList: String, uri: Uri) {
        Log.e("CURRENT LIST","is $currentList")
        val selectedList = activity.getSharedPreferences(currentList, AppCompatActivity.MODE_PRIVATE)
        // "currentList" était entre 2 ptn de guillemets et ça a tout fait buger
        /*val allTasksList = ArrayList<Task>()
        selectedList.all.map { it.key }.forEach { str ->
            selectedList.getString(str, null)?.let {
                allTasksList.add(Json.decodeFromString(it)) //Decode to task & add
            }
        }*/

        val testList = selectedList.all.map { entry ->
            selectedList.getString(entry.key, null)?.let {
                Json.decodeFromString(Task.serializer(), it) //Decode to task & add
            } ?: Task("Tâche vide...", creationDate = System.currentTimeMillis(), done = true)
        }.toMutableList()

        val jsonText = Json.encodeToString(testList)
        modifyDoc(uri, jsonText)
    }

    fun openFile(pickerInitialUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            val mimetypes = arrayOf("application/txt", "text/*")
            putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            // Optionally, specify a URI for the file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                pickerInitialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri) }
        }

        activity.startActivityForResult(intent, 2)
    }

    fun readTextContent(uri: Uri): String {
        val stringBuilder = StringBuilder()
        val contentResolver = activity.applicationContext.contentResolver

        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }

        return stringBuilder.toString()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun modifyDoc(uri: Uri, textContent: String = "Overwritten at ${System.currentTimeMillis()}\n") {
        val contentResolver = activity.applicationContext.contentResolver
        try {
            contentResolver.apply {
                takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                openFileDescriptor(uri,"w")?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).use { output ->
                        output.write((textContent).toByteArray())
                    }
                }
            }

            Toast.makeText(activity.baseContext,"Fichier sauvegardé avec succès !",Toast.LENGTH_SHORT).show()
        } catch (e : Exception){
            e.printStackTrace()
            Toast.makeText(activity.baseContext,"Erreur...",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDate(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.DAY_OF_MONTH)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.YEAR)}-${
            c.get(Calendar.HOUR_OF_DAY)
        }h${c.get(Calendar.MINUTE)}"
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var single : TasksRepository? = null

        fun getInstance(activity: Activity): TasksRepository {
            if (single == null) single = TasksRepository(activity)
            return single as TasksRepository
        }

        @Suppress("unused")
        val Number.dp get() = toFloat() * (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

}