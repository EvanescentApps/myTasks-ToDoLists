package com.electro.todolist.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.RadioGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.electro.todolist.R
import com.electro.todolist.data.Priority
import com.electro.todolist.data.Task
import com.electro.todolist.data.TasksRepository
import com.electro.todolist.databinding.ActivityTaskDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class TaskDetailsActivity : AppCompatActivity() {

    //private lateinit var titreEditText: EditText
    //private lateinit var descriptionEditText: AppCompatEditText
    private lateinit var task: Task
    private lateinit var jsonTask: String
    private lateinit var currentList : String

    //private lateinit var bottomAppBar: BottomAppBar
    private var delete: Boolean = false
    private var isNewDone: Boolean = false
    private var position: Int = -1

    private lateinit var b: ActivityTaskDetailsBinding

    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // New : View Binding ! To get rid of the boilerplate code findViewById !
        b = ActivityTaskDetailsBinding.inflate(layoutInflater)

        setContentView(b.root) // r.layout.activity_task_details

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val tasksRepository = TasksRepository.getInstance(this)
        tasksRepository.getDefaultList()

        delete = false
        isNewDone = false
        jsonTask = intent.getStringExtra("currentTask").toString()
        currentList = intent.getStringExtra("currentList").toString()

        task = try {
            json.decodeFromString(Task.serializer(), jsonTask)
        } catch (e: Exception) {
            Task(
                "Erreur de décodage",
                "Erreur de décodage de la tâche : ${e.cause} ${e.message} ${e.stackTraceToString()}",
                System.currentTimeMillis(),
                false
            )
        }

        b.title.setText(task.title)
        b.description.setText(task.description)

        task.date?.let {
            val dateText = tasksRepository.timestampToDate(it)
            if (dateText.isNotBlank()) b.setTime.text = dateText
            else b.setTime.text = "Ajouter date/heure"
        }

        task.duration?.let {
            val durationText = tasksRepository.timestampToDuration(it)
            if (durationText.isNotBlank()) b.setDuration.text = durationText
            else b.setDuration.text = "Définir la durée"
        }

        if (task.priority != Priority.NONE) {
            b.setPriority.text = task.priority.first
            b.setPriority.setTextColor(ContextCompat.getColor(this, task.priority.second))
        } else b.setPriority.text = "Définir la priorité"

        try {
            b.description.apply {
                linksClickable = true
                autoLinkMask = Linkify.WEB_URLS
                Linkify.addLinks(this, Linkify.WEB_URLS)
                addTextChangedListener(
                    afterTextChanged = {
                        it?.let{
                            Linkify.addLinks(it, Linkify.WEB_URLS)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("Clickable links", "Error : ${e.stackTrace}")
        }

        // HANDLE THE CASE WHEN TASK IS DONE
        // SHOW : SET UNDONE

        b.setDoneFab.setOnClickListener {
            task.done = true
            finish()
        }

        val startFlowForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result: ActivityResult ->

            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    if (it.hasExtra("done")) {
                        if (it.getBooleanExtra("done", false)) {
                            task.done = true
                            finish()
                        }
                    }
                }
            }
        }

        fun setDurationDialog(){
            val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)

            val viewInflated: View = LayoutInflater.from(this)
                .inflate(R.layout.choose_duration, window.decorView.rootView as ViewGroup?, false)

            val hoursPicker = viewInflated.findViewById<NumberPicker>(R.id.hours)
            val minutesPicker = viewInflated.findViewById<NumberPicker>(R.id.minutes)
            val secondsPicker = viewInflated.findViewById<NumberPicker>(R.id.seconds)

            builder.apply {
                setView(viewInflated)
                setTitle("Définir la Durée")
                setMessage("Définissez une durée pour cette tâche")
                setCancelable(false)

                setPositiveButton(
                    R.string.ok
                ) { dialog, _ ->
                    dialog.dismiss()

                    val hours = hoursPicker.value
                    val minutes = minutesPicker.value
                    val seconds = secondsPicker.value

                    var durationText = ""
                    if (hours != 0) {
                        durationText += "${hours}h"
                    }
                    if (minutes != 0) {
                        val space = if (durationText.isNotEmpty()) " " else ""
                        durationText += "${space}${minutes} min"
                    }
                    if (seconds != 0) {
                        val space =  if (durationText.isNotEmpty()) " " else ""
                        durationText += "${space}${seconds}s"
                    }

                    val timeDurationSeconds = hours*3600 + minutes * 60 + seconds
                    val timeDurationMillis = timeDurationSeconds * 1000

                    task.duration = timeDurationMillis.toLong()

                    if (durationText.isNotBlank()) b.setDuration.text = durationText
                }

                setNegativeButton(
                    R.string.cancel
                ) { dialog, _ -> dialog.cancel() }

                show()

                val taskDurationMillis = task.duration

                val taskDurationSec: Int

                var hoursDuration = 0
                var minutesDuration = 0
                var secondsDuration = 0

                if (taskDurationMillis != null) {
                    taskDurationSec = (taskDurationMillis/1000).toInt()

                    secondsDuration = taskDurationSec % 60
                    minutesDuration = (taskDurationSec / 60) % 60
                    hoursDuration = (taskDurationSec/ 3600 ) % 24
                }

                hoursPicker.apply {
                    minValue = 0
                    maxValue = 48
                    value = hoursDuration

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        textSize = 100f
                        textColor = ContextCompat.getColor(this.context, R.color.textContent)
                    }
                }

                //hoursPicker2.maxValue = 48
                //hoursPicker2.minValue = 0

                minutesPicker.apply {
                    minValue = 0
                    maxValue = 59
                    value = minutesDuration

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        textSize = 100f
                        textColor = ContextCompat.getColor(this.context, R.color.textContent)
                    }
                }

                secondsPicker.apply {
                    minValue = 0
                    maxValue = 59
                    value = secondsDuration

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        textSize = 100f
                        textColor = ContextCompat.getColor(this.context, R.color.textContent)
                    }
                }
            }
        }

        b.launchFlow.setOnClickListener {

            // TODO : CHECK IF DURATION : ELSE  SHOW DIALOG THEN LAUNCH FLOW

            val taskDuration: Int = task.duration?.div(1000)?.toInt() ?: 900

            task.duration?.let{
                val flowIntent = Intent(this, FlowActivity::class.java).apply {
                    putExtra("taskDuration", taskDuration)
                    putExtra("taskTitle", task.title)
                }

                startFlowForResult.launch(flowIntent)
            } ?: run { setDurationDialog() }
        }

        fun showPriorityDialog() {
            val viewInflated: View = LayoutInflater.from(this).inflate(R.layout.choose_priority, window.decorView.rootView as ViewGroup?, false)
            val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            builder.apply {
                setView(viewInflated)
                setTitle("Définir la Priorité")
                setCancelable(false)
                setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    val priorityGroup = viewInflated.findViewById<RadioGroup>(R.id.priorityGroup)

                    val priorityVal = when(priorityGroup.checkedRadioButtonId){
                        R.id.tres_important -> Priority.VERY_HIGH
                        R.id.important -> Priority.HIGH
                        R.id.tres_urgent -> Priority.URGENT
                        R.id.pas_urgent -> Priority.NOT_URGENT
                        R.id.facultatif -> Priority.FACULTATIVE
                        else -> Priority.NONE
                    }

                    if (priorityVal != Priority.NONE)  {
                        b.setPriority.text = priorityVal.first
                        b.setPriority.setTextColor(ContextCompat.getColor(this.context, priorityVal.second))
                        task.priority = priorityVal
                    }
                }
                setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                show()
            }
        }

        fun setTimeDialog() {
            val cal = Calendar.getInstance()

            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)

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

                val timeStamp = cal.timeInMillis
                task.date = timeStamp
                b.setTime.text ="$day à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)}"
            }

            val dateSetListener =
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        this,
                        R.style.MyDialogTheme,
                        timeSetListener,
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show()
                }

            DatePickerDialog(
                this,
                R.style.MyDialogTheme,
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun showModifyOrDeleteDialog(modifyDialog: () -> Unit, deleteAction: () -> Unit) {
            // TODO : PASS TWO ARGS : A RUNNABLE FOR MODIFY AND A RUNNABLE FOR DELETE
            // FOR NOW ONLY WORKING FOR PRIORITY, BUT NEXT SHOULD BE REUSABLE FOR ALL

            val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)

            builder.apply {
                setTitle("Modifier ou Supprimer ?")

                setPositiveButton("MODIFIER") { dialog, _ ->
                    dialog.dismiss()
                    modifyDialog()
                }
                setNegativeButton("SUPPRIMER") { dialog, _ ->
                    dialog.dismiss()
                    deleteAction()
                }
                show()
            }
        }

        b.setDuration.setOnClickListener {
            if (task.duration != null) {
                showModifyOrDeleteDialog(
                    { setDurationDialog() },
                    {
                        task.duration = null
                        b.setDuration.text = "Définir la durée"
                        b.setDuration.setTextColor(ContextCompat.getColor(this, R.color.textContent))
                    }
                )
            } else setDurationDialog()
        }

        b.setTime.setOnClickListener {
            if (task.date != null) {
                showModifyOrDeleteDialog(
                    { setTimeDialog() },
                    {
                        task.date = null
                        b.setTime.text = "Ajouter date/heure"
                        b.setTime.setTextColor(ContextCompat.getColor(this, R.color.textContent))
                    }
                )
            } else setTimeDialog()
        }

        b.setPriority.setOnClickListener {
            if (task.priority != Priority.NONE) {
                showModifyOrDeleteDialog(
                    { showPriorityDialog() },
                    {
                        task.priority = Priority.NONE
                        b.setPriority.text = "Définir la priorité"
                        b.setPriority.setTextColor(ContextCompat.getColor(this, R.color.textContent))
                    }
                )
            } else showPriorityDialog()
        }

        b.bottomAppBar.setNavigationOnClickListener {
            Timber.tag("Navigation").e("pressed")
            val userLists = tasksRepository.getListGroup()

            Timber.e(userLists.toString())

            //val goodList = userLists?.filter { it.id!= "defaultList" }
            if (userLists.containsKey("defaultList")) { userLists.remove("defaultList") }

            val newList = userLists.map { Pair(it.key, it.value) }//toMutableList()
            //Timber.e(goodList.toString())
            // TODO : SET "CHANGE LIST" MODE WITH DIFFERENT INTERFACE
            // texte "Déplacer vers :



            val userListsSerialized = json.encodeToString(newList as List<Pair<String, String>>)
            ChangeListFragment.newInstance(tasksRepository.currentListName, userListsSerialized)
                .show(supportFragmentManager, "dialog")
        }

        b.bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete -> {
                    Timber.tag("Delete").e("pressed")
                    delete = true
                    finish()
                    true
                }
                R.id.moreOptions -> {
                    val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)

                    builder.apply {
                        setTitle("Fonctionnalité en cours de développement")
                        setMessage("Le menu contextuel pas disponible pour l'instant, ce sera pour une prochaine mise à jour.")
                        setPositiveButton(
                            "D'accord"
                        ) { dialog, _ ->
                            dialog.dismiss()
                        }
                        show()
                    }
                    true
                }
                R.id.done -> {
                    Log.e("Done", "pressed")
                    task.done = true
                    isNewDone = true
                    finish()
                    true
                }
                else -> false
            }
        }

        position = intent.getIntExtra("position", -1)
    }

    private fun updatedTaskJson(): String {
        task.title = b.title.text.toString()
        task.description = b.description.text.toString()

        return json.encodeToString(Task.serializer(), task)
    }

    private fun generateResult(): Intent {
        val returnTaskJson = updatedTaskJson()

        return Intent().apply {
            putExtra("returnTask", returnTaskJson)
            putExtra("position", position)
            putExtra("delete", delete)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        return super.onSupportNavigateUp()
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, generateResult())

        super.finish()
    }

    @SuppressLint("ApplySharedPref")
    override fun onStop() {

        Thread {
            Log.e("OnStop", "Stopping")
            Log.e("TASK", updatedTaskJson())

            if (!delete) {
                getSharedPreferences(currentList, MODE_PRIVATE).edit()
                    .putString(task.creationDate.toString(), updatedTaskJson()).apply()
            }

        }.start()
        super.onStop()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        super.onBackPressed()
    }
}