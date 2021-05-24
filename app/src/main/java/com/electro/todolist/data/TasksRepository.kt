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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*

class TasksRepository(private val dataStore: DataStore<Preferences>,val activity: Activity) {

    private fun String.toSafeCase(): String {

        return java.text.Normalizer.normalize(this.lowercase(), java.text.Normalizer.Form.NFD)
            .filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ", "_")
            /*camelRegex.replace(this) {
            "_${it.value}"
            }*/
    }

    val listsPrefs: SharedPreferences = activity.getSharedPreferences("allLists", AppCompatActivity.MODE_PRIVATE)
    val tasks = "e"

    var readOnlyUserLists: MutableMap<String, *> = listsPrefs.all

    var defaultListKey = ""

    var list = readOnlyUserLists.values.toList() //arrayListOf("List 1","List 2","List 3")

    var currentListName = defaultListKey

    fun getDefaultList() {

        if(readOnlyUserLists.isNotEmpty()) {
            Log.e("readOnlyUserLists",readOnlyUserLists.toString())

            listsPrefs.getString("defaultList", null)?.let {
                defaultListKey = it
                Log.e("Default","shared is not null")
            } ?: run {
                defaultListKey = readOnlyUserLists.map { it.key }[0]
                Log.e("Default","shared is null, taking first list")
            }

            Log.e("defaultListKey","is $defaultListKey , but ${listsPrefs.getString("defaultList", null)}")
        } else {
            Log.e("Empty ListsPrefs","Creating -Mes Tâches- List")
            listsPrefs.edit().putString("list1","Mes tâches").apply()
            listsPrefs.edit().putString("defaultList","list1").apply()
            defaultListKey = "list1"
            readOnlyUserLists = listsPrefs.all
            list = readOnlyUserLists.values.toList()
        }

        currentListName = defaultListKey
    }

    fun putNewLists() {
        listsPrefs.edit().putString("list1", "Mes tâches").apply()
        listsPrefs.edit().putString("list2", "Aujourd'hui").apply()
        listsPrefs.edit().putString("list3", "Demain").apply()
    }

    fun createList(name : String) {
        // create entry in sharedPreferences for a new list
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

    fun removeAllTasksDone() {
        /* tasks.forEach {
             if (it.done) {
                 tasks.remove(it)
             }
         }*/
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

}