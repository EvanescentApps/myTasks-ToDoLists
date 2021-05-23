package com.electro.todolist.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.electro.todolist.R
import com.electro.todolist.data.Task
import com.electro.todolist.databinding.ActivityTaskDetailsBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class TaskDetailsActivity : AppCompatActivity() {

    //private lateinit var titreEditText: EditText
    //private lateinit var descriptionEditText: AppCompatEditText
    private lateinit var task: Task
    private lateinit var jsonTask: String
    //private lateinit var bottomAppBar: BottomAppBar
    private var delete: Boolean = false
    private var position: Int = -1
    private lateinit var b : ActivityTaskDetailsBinding

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

        delete = false
        jsonTask = intent.getStringExtra("currentTask").toString()

        task = Json.decodeFromString(jsonTask)

        //b.title.movementMethod = LinkMovementMethod.getInstance()
        //b.description.movementMethod = LinkMovementMethod.getInstance()

        b.title.setText(task.title)
        b.description.setText(task.description)
        b.description.linksClickable = true
        b.description.autoLinkMask = Linkify.WEB_URLS
        Linkify.addLinks(b.description,Linkify.WEB_URLS)

        b.description.addTextChangedListener(
            afterTextChanged = {
                if (it != null) {
                    Linkify.addLinks(it, Linkify.WEB_URLS)
                }
            }
        )

        // HANDLE THE CASE WHEN TASK IS DONE
        // SHOW : SET UNDONE

        b.setDoneFab.setOnClickListener {
            task.done = true
            finish()
            // DON'T FORGET TO TOAST !
        }

        b.bottomAppBar.setOnMenuItemClickListener {
            when(it.itemId) {
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

        val returnTaskJson = Json.encodeToString(task)
        Log.e("DetailsAct", returnTaskJson)

        val returnIntent = Intent()
        returnIntent.putExtra("returnTask", returnTaskJson)
        returnIntent.putExtra("position", position)
        returnIntent.putExtra("delete", delete)

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