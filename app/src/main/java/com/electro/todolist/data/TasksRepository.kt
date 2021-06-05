@file:Suppress("PropertyName")

package com.electro.todolist.data

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.electro.todolist.ui.TasksActivity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class TasksRepository(private val activity: Activity) {

    private fun String.toSafeCase(): String {
        return java.text.Normalizer.normalize(this.lowercase(), java.text.Normalizer.Form.NFD)
            .filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ", "_")
    }

    // Shared Preferences directory containing all the user's lists as Key Value Pairs (id, name)
    var listsPrefs: SharedPreferences = activity.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)

    // Getting all the lists as a MutableMap (read only)
    var readOnlyUserLists: MutableMap<String, *> = listsPrefs.all

    // the last opened list
    var lastOpenedList_Key = ""

    // list of ids
    var listOfIds = readOnlyUserLists.values.toList() //arrayListOf("List 1","List 2","List 3")
    var userLists : List<Pair<String,String>> = (readOnlyUserLists.toList()) as List<Pair<String,String>>

    //Important, needs to be up to date
    var currentListName = lastOpenedList_Key

    fun updateLists() {
        readOnlyUserLists = getAllListsPref().all
        listOfIds = readOnlyUserLists.values.toList()
    }

    fun parseTimestampToDuration( timestamp : Long) : String {

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

    fun parseTimestampToDate(timestamp : Long): String {
        val cal = Calendar.getInstance()

        cal.timeInMillis = timestamp

        val daysOfWeek = listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
        val monthsOfYear = listOf("Janvier", "Février", "Mars","Avril","Mai","Juin","Juillet", "Août", "Septembre", "Octobre","Novembre","Décembre")

        val currentCal = Calendar.getInstance()
        currentCal.timeInMillis = System.currentTimeMillis()
        var day = ""
        if (cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) + 1) {
            day = "Demain"
        } else if (cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)) {
            day = "Aujourd'hui"
        } else if (cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) -1) {
            day = "Hier"
        } else if (cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) + 2) {
            day = "Après-Demain"
        } else {
            day = "${  daysOfWeek[cal.get(Calendar.DAY_OF_WEEK)-1].take(3)   }. ${cal.get(Calendar.DAY_OF_MONTH)} ${monthsOfYear[cal.get(Calendar.MONTH)]}"
        }

        return "${day} à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)}"
    }

    fun getDefaultList() {

        if(readOnlyUserLists.isNotEmpty()) {

            listsPrefs.getString("defaultList", null)?.let {
                lastOpenedList_Key = it
                Log.e("Default","shared is not null")
            } ?: run {
                lastOpenedList_Key = readOnlyUserLists.map { it.key }[0]
                Log.e("Default","shared is null, taking first list")
            }

            Log.e("defaultListKey","is $lastOpenedList_Key , but ${listsPrefs.getString("defaultList", null)}")
        } else {
            // Creating a list because empty
            Log.e("Empty ListsPrefs","Creating -Mes Tâches- List")
            putNewLists()
            lastOpenedList_Key = "list1"
            updateLists()
        }

        currentListName = lastOpenedList_Key
    }

    fun getAllListsPref() : SharedPreferences {
        return activity.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)
    }

    fun putNewLists() {
        val listsPrefs = getAllListsPref()

        listsPrefs.edit().putString("list1", "Mes tâches").apply()
        listsPrefs.edit().putString("list2", "Aujourd'hui").apply()
        listsPrefs.edit().putString("list3", "Demain").apply()
        listsPrefs.edit().putString("defaultList","list1").apply()
    }

    fun createList(name : String) {
        // create entry in sharedPreferences for a new list

        getAllListsPref().edit().putString(Task.generateId(6), name).commit().let {
            // update list of lists
            updateLists()
        }
    }

    fun renameList(newName: String, listId: String) {
        val listsPrefs = getAllListsPref()
        if (listsPrefs.getString(listId,null) != null) {
            listsPrefs.edit().putString(listId, newName).apply()
            (activity as TasksActivity).updateListName(newName)
        } else {
            Toast.makeText(activity,"Cette liste n'existe pas... Veuillez réessayer.",Toast.LENGTH_LONG).show()
        }

        updateLists()
    }

    fun deleteList(listId: String) {
        val listsPrefs = getAllListsPref()
        try {
            listsPrefs.edit().remove(listId).apply()
            Toast.makeText(activity, "Liste supprimée", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Error removing list", e.stackTrace.toString())
        }
        updateLists()
        (activity as TasksActivity).changeList(listOfIds[0].toString())

    }

    fun deleteAllDoneTasks(list : String) {
        val selectedListContent = activity.getSharedPreferences(list, AppCompatActivity.MODE_PRIVATE)

        val allTasksList = ArrayList<Task>()
        selectedListContent.all.map { it.key }.forEach { str ->
            selectedListContent.getString(str, null)?.let {
                allTasksList.add(Json.decodeFromString(it)) //Decode to task & add
            }
        }

        var isError = false

        allTasksList.forEach { task ->
            if (task.done) {
                try {
                    selectedListContent.edit().remove(task.creationDate.toString()).apply()
                } catch (e:Exception) {
                    Log.e("Error Tasks Removal",e.stackTrace.toString())
                    isError = true
                }

            }
        }

        if (!isError) {
            Toast.makeText(activity, "Tâches terminées supprimées ✔", Toast.LENGTH_SHORT).show()
        }

    }

    fun exportToFile(currentList: String) {

        /* snackbar, which action is SHOW ME */

        val fileName = "${currentList.toSafeCase()}-${getDate()}"

        createFile(fileName)
    }

    fun createFile(fileName: String, pickerInititalUri: Uri? = null) {

        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/txt"
                putExtra(Intent.EXTRA_TITLE, "$fileName.txt")
                //Optionally
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pickerInititalUri?.let {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI,it)
                    }
                }
            }
            Toast.makeText(activity, "Fichier $fileName.txt créé", Toast.LENGTH_LONG).show()
            activity.startActivityForResult(intent,1)
            Log.i("Tasks to file", "Waiting for a result (user choosing directory)")
        } catch (e: Exception) {
            Log.e("Save error","Error thrown")
            e.printStackTrace()
        }
    }

    fun writeTaskListTofile(currentList: String, uri: Uri) {
        Log.e("CURRENT LIST","is $currentList vs list1 normally")
        val selectedList = activity.getSharedPreferences("list1", AppCompatActivity.MODE_PRIVATE)
        val allTasksList = ArrayList<Task>()
        selectedList.all.map { it.key }.forEach { str ->
            selectedList.getString(str, null)?.let {
                allTasksList.add(Json.decodeFromString(it)) //Decode to task & add
            }
        }
        val jsonText = Json.encodeToString(allTasksList)
        Log.e("json FILE","file is $jsonText")

        modifyDoc(uri, jsonText)
    }

    fun openFile(pickerInitialUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/txt"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pickerInitialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri) }
            }
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

    fun modifyDoc(uri: Uri, textContent: String = "Overwritten at ${System.currentTimeMillis()}\n") {
        val contentResolver = activity.applicationContext.contentResolver

        try {

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Check for the freshest data.
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            contentResolver.openFileDescriptor(uri,"rw")?.use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    output.write((textContent).toByteArray()
                    )
                }
            }

            Toast.makeText(activity.baseContext,"File saved successfully !",Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(activity.baseContext,"Error...",Toast.LENGTH_SHORT).show()
        } catch (e : IOException){
            e.printStackTrace()
            Toast.makeText(activity.baseContext,"Error...",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDate(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.DAY_OF_MONTH)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.YEAR)}-${
            c.get(Calendar.HOUR_OF_DAY)
        }h${c.get(Calendar.MINUTE)}"
    }

    companion object {
        var single : TasksRepository? = null

        fun getInstance(activity: Activity): TasksRepository {
            if (single == null)
                single = TasksRepository(activity)
            return single as TasksRepository
        }
    }

}