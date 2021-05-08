package com.electro.todolist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var titreEditText: EditText
    private lateinit var descriptionEditText: AppCompatEditText
    private lateinit var task: Task
    private lateinit var jsonTask: String
    private lateinit var bottomAppBar: BottomAppBar
    private var delete: Boolean = false
    private var position: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val setDoneFAB = findViewById<ExtendedFloatingActionButton>(R.id.setDoneFab)

        bottomAppBar = findViewById(R.id.bottomAppBar)
        delete = false
        
        // GET BUNDLE AND EXTRACT TASK
        jsonTask = intent.getStringExtra("currentTask").toString()

        task = Json.decodeFromString(jsonTask)

        titreEditText = findViewById(R.id.title)
        descriptionEditText = findViewById(R.id.description)



        titreEditText.setText(task.title)


        //titreEditText.movementMethod = LinkMovementMethod.getInstance()

        descriptionEditText.setText(task.description)
        //descriptionEditText.movementMethod = LinkMovementMethod.getInstance()
        descriptionEditText.linksClickable = true
        descriptionEditText.autoLinkMask = Linkify.WEB_URLS
        Linkify.addLinks(descriptionEditText,Linkify.WEB_URLS)

        descriptionEditText.addTextChangedListener(
            afterTextChanged = {
                if (it != null) {
                    Linkify.addLinks(it, Linkify.WEB_URLS)
                }
            }
        )

        // HANDLE THE CASE WHEN TASK IS DONE
        // SHOW : SET UNDONE

        setDoneFAB.setOnClickListener {
            task.done = true
            finish()
            // DON'T FORGET TO TOAST !
        }

        bottomAppBar.setOnMenuItemClickListener {
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

        val editedTitle = titreEditText.text.toString()
        val editedDescription = descriptionEditText.text.toString()

        task.title = editedTitle
        task.description = editedDescription

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