package com.electro.todolist.ui

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.addTextChangedListener
import com.electro.todolist.ChangeListFragment
import com.electro.todolist.FlowActivity
import com.electro.todolist.R
import com.electro.todolist.data.Priority
import com.electro.todolist.data.Task
import com.electro.todolist.data.TasksRepository
import com.electro.todolist.databinding.ActivityTaskDetailsBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*


class TaskDetailsActivity : AppCompatActivity() {

    //private lateinit var titreEditText: EditText
    //private lateinit var descriptionEditText: AppCompatEditText
    private lateinit var task: Task
    private lateinit var jsonTask: String

    //private lateinit var bottomAppBar: BottomAppBar
    private var delete: Boolean = false
    private var isNewDone: Boolean = false
    private var position: Int = -1
    private lateinit var b: ActivityTaskDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // New : View Binding ! To get rid of the boilerplate code findViewById !
        b = ActivityTaskDetailsBinding.inflate(layoutInflater)

        setContentView(b.root) // r.layout.activity_task_details

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        //val setDoneFAB = findViewById<ExtendedFloatingActionButton>(R.id.setDoneFab)
        //titreEditText = findViewById(R.id.title)
        //descriptionEditText = findViewById(R.id.description)
        //bottomAppBar = findViewById(R.id.bottomAppBar)

        val tasksRepository = TasksRepository.getInstance(this)

        tasksRepository.getDefaultList()



        delete = false
        isNewDone = false
        jsonTask = intent.getStringExtra("currentTask").toString()

        task = try {
            Json.decodeFromString(Task.serializer(), jsonTask)
        } catch (e: Exception) {
            Task(
                "Erreur de décodage",
                "Erreur de décodage de la tâche : ${e.cause} ${e.message} ${e.stackTraceToString()}",
                System.currentTimeMillis(),
                false
            )
        }

        //b.title.movementMethod = LinkMovementMethod.getInstance()
        //b.description.movementMethod = LinkMovementMethod.getInstance()



        b.title.setText(task.title)
        b.description.setText(task.description)

        /*val duration = task.duration
        if(duration != null) {
            b.setDuration.text = TasksRepository.getInstance(this).parseTimestampToDuration(duration)
        }

        val dateTimestamp = task.date
        if(dateTimestamp != null) {
            b.setTime.text = TasksRepository.getInstance(this).parseTimestampToDate(dateTimestamp)
        }*/

        task.date?.let {
            val dateText = tasksRepository.parseTimestampToDate(it)
            if (dateText.isNotBlank()) b.setTime.text = dateText
            else b.setTime.text = "Ajouter date/heure"
        }

        task.duration?.let {
            val durationText = tasksRepository.parseTimestampToDuration(it)
            if (durationText.isNotBlank()) b.setDuration.text = durationText
            else b.setDuration.text = "Définir la durée"
        }

        try {
            b.description.linksClickable = true
            b.description.autoLinkMask = Linkify.WEB_URLS
            Linkify.addLinks(b.description, Linkify.WEB_URLS)

            b.description.addTextChangedListener(
                afterTextChanged = {
                    if (it != null) {
                        Linkify.addLinks(it, Linkify.WEB_URLS)
                    }
                }
            )

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
                val intent = result.data

                if (intent != null) {
                    if (intent.hasExtra("done")) {
                        if (intent.getBooleanExtra("done", false)) {
                            task.done = true
                            finish()
                        }
                    }
                }
            }
        }

        b.launchFlow.setOnClickListener {

            val taskDuration: Int = task.duration?.div(1000)?.toInt() ?: 900

            val flowIntent = Intent(this, FlowActivity::class.java)
            flowIntent.putExtra("taskDuration", taskDuration)
            flowIntent.putExtra("taskTitle", task.title)

            startFlowForResult.launch(flowIntent)
        }

        b.setTime.setOnClickListener {

            val cal = Calendar.getInstance()

            val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
               /* Toast.makeText(
                    this,
                    "Time selected : ${
                        SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                        ).format(cal.time)
                    }",
                    Toast.LENGTH_LONG
                ).show()
*/
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
                val timeStamp = cal.timeInMillis
                task.date = timeStamp
                b.setTime.text ="${day} à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)}"
            }

            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    /*Toast.makeText(
                        this,
                        "Date selected : ${cal.get(Calendar.DAY_OF_MONTH)} ${cal.get(Calendar.MONTH)} ${
                            cal.get(Calendar.YEAR)
                        }",
                        Toast.LENGTH_LONG
                    ).show()*/
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


        b.setPriority.setOnClickListener {
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))

            // I'm using fragment here so I'm using getView() to provide ViewGroup
            // but you can provide here any other instance of ViewGroup from your Fragment / Activity

            val viewInflated: View = LayoutInflater.from(this)
                .inflate(R.layout.choose_priority, window.decorView.rootView as ViewGroup?, false)

            //val input = viewInflated.findViewById<EditText>(R.id.input)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            builder.setView(viewInflated)

            builder.setTitle("Définir la Priorité")

            builder.setCancelable(false)

            builder.setPositiveButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.dismiss()

                b.setPriority.text = Priority.HIGH.first
            }
            builder.setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.cancel() }

            val alertDialogDuration = builder.create()
            alertDialogDuration.show()

        }

        b.setDuration.setOnClickListener {

            val builder: AlertDialog.Builder =
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))

            // I'm using fragment here so I'm using getView() to provide ViewGroup
            // but you can provide here any other instance of ViewGroup from your Fragment / Activity

            val viewInflated: View = LayoutInflater.from(this)
                .inflate(R.layout.choose_duration, window.decorView.rootView as ViewGroup?, false)

            //val input = viewInflated.findViewById<EditText>(R.id.input)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            builder.setView(viewInflated)

            builder.setTitle("Définir la Durée")

            builder.setCancelable(false)

            val hoursPicker = viewInflated.findViewById<NumberPicker>(R.id.hours)
            val minutesPicker = viewInflated.findViewById<NumberPicker>(R.id.minutes)
            val secondsPicker = viewInflated.findViewById<NumberPicker>(R.id.seconds)

            builder.setPositiveButton(
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
                    var space = ""
                    if (durationText.isNotEmpty()) space = " "


                    durationText += "${space}${minutes} min"
                }
                if (seconds != 0) {
                    var space = ""
                    if (durationText.isNotEmpty()) space = " "

                    durationText += "${space}${seconds}s"
                }

                val timeDurationSeconds = hours*3600 + minutes * 60 + seconds
                val timeDurationMillis = timeDurationSeconds * 1000

                task.duration = timeDurationMillis.toLong()

                if (!durationText.isNullOrBlank()) {
                    b.setDuration.text = durationText
                }

            }
            builder.setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.cancel() }

            val alertDialogDuration = builder.create()
            alertDialogDuration.show()

            val taskDurationMillis = task.duration

            var taskDurationSec = 0

            var hoursDuration = 0
            var minutesDuration = 0
            var secondsDuration = 0

            if (taskDurationMillis != null) {
                /*taskDurationSec = (taskDurationMillis/1000).toInt()
                hoursDuration = taskDurationSec % 3600
                taskDurationSec -= hoursDuration * 3600
                minutesDuration = taskDurationSec % 60
                taskDurationSec -= minutesDuration*60
                secondsDuration = taskDurationSec*/

                taskDurationSec = (taskDurationMillis/1000).toInt()
                //Toast.makeText(this, " $taskDurationSec", Toast.LENGTH_LONG).show()
                secondsDuration = taskDurationSec % 60
                minutesDuration = (taskDurationSec / 60) % 60
                hoursDuration = (taskDurationSec/ 3600 ) % 24
                //Toast.makeText(this, " $hoursDuration $minutesDuration $secondsDuration", Toast.LENGTH_LONG).show()
            }

            hoursPicker.minValue = 0
            hoursPicker.maxValue = 100
            hoursPicker.value = hoursDuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hoursPicker.textSize = 64f
                //hoursPicker.setPadding(0, 18, 0, 18)
            }

            minutesPicker.minValue = 0
            minutesPicker.maxValue = 59
            minutesPicker.value = minutesDuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                minutesPicker.textSize = 64f
                //minutesPicker.setPadding(0, 32, 0, 32)
            }

            secondsPicker.minValue = 0
            secondsPicker.maxValue = 59
            secondsPicker.value = secondsDuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                secondsPicker.textSize = 64f
                //secondsPicker.setPadding(0, 18, 0, 18)
            }

        }

        b.bottomAppBar.setNavigationOnClickListener {
            val userLists = tasksRepository.readOnlyUserLists.toList()
            val userListsSerialized = Json.encodeToString(userLists as? List<Pair<String, String>>)
            ChangeListFragment.newInstance(tasksRepository.currentListName, userListsSerialized)
                .show(supportFragmentManager, "dialog")
        }

        b.bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete -> {
                    Log.e("Delete", "pressed")
                    delete = true
                    finish()
                    true
                }
                R.id.favorite -> {
                    Log.e("Favorite", "pressed")
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

        // CHECK IF EXISTS !!!!
        position = intent.getIntExtra("position", -1)
        // Parse JSON to Task here
        // And then set text to title and description fields

        // Handle Changes for fields onStop
    }

    override fun onStop() {
        super.onStop()
        // SAVE CHANGES AND TELL THE MAIN ACTIVITY TO UPDATE THIS ITEM
        // SO HERE CALL MainActivity's function to update
        // with updated task, and position given
        // IF POSITION == -1 then error raised, do nothing & show snackbar error occured : Pos error
    }

    private fun generateResult(): Intent {

        task.title = b.title.text.toString()
        task.description = b.description.text.toString()

        val returnTaskJson = Json.encodeToString(Task.serializer(), task)
        Log.e("DetailsAct", returnTaskJson)

        val returnIntent = Intent()
        returnIntent.putExtra("returnTask", returnTaskJson)
        returnIntent.putExtra("position", position)
        returnIntent.putExtra("delete", delete)
        //returnIntent.putExtra("done", isNewDone)

        return returnIntent
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

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        super.onBackPressed()
    }
}