package com.electro.todolist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.electro.todolist.databinding.ActivityFlowBinding
import kotlin.random.Random

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FlowActivity : AppCompatActivity() {

    private lateinit var b: ActivityFlowBinding
    private lateinit var fullscreenContent: LinearLayout
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.getMainLooper())
    private lateinit var mCountDownTimer: CountDownTimer
    private var i: Int = 0
    private var taskDurationMillis: Int = 0

    private var finished = false

    private lateinit var baseColor : IntArray

    private var isPaused = false
    private var timeElapsedStop = 0

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    /*private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }*/

    private var timeLeft: Long = 0

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityFlowBinding.inflate(layoutInflater)
        setContentView(b.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        try {
            val attrib = window.attributes
            attrib.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = b.fullscreenContent
        //fullscreenContent.setOnClickListener { toggle() }

        fullscreenContentControls = b.fullscreenContentControls

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //binding.dummyButton.setOnTouchListener(delayHideTouchListener)

        val string1 = "Reste motivé(e), c'est bientôt fini !"
        val string2 = "C'est parti ! Hâte de voir le résultat ?"

        val motivation = listOf(string1, string2)

        val taskDurationSec =
            intent.getIntExtra("taskDuration", 900) // Task & Flow duration in seconds

        taskDurationMillis = taskDurationSec * 1000

        val taskTitle = intent.getStringExtra("taskTitle")

        b.taskTitle.text = taskTitle
        b.motivText.text = motivation[Random.nextInt(motivation.size)]

        timeLeft = taskDurationMillis.toLong()
        // TODO : pass as an argument in intent bundle

        updateTimer(timeLeft)

        setTimer(taskDurationSec)

    }

    fun updateTimer(timeLeftMillis: Long) {
        timeLeft = timeLeftMillis

        val timeLeftMillisUp = timeLeftMillis + (1 * 1000)

        val minutes = timeLeftMillisUp / 60000
        val seconds = (timeLeftMillisUp % 60000 / 1000)
        var textTimeLeft = ""
        if (minutes < 10) {
            textTimeLeft += "0"
        }
        textTimeLeft += "${minutes}:"
        if (seconds < 10) {
            textTimeLeft += "0"
        }
        textTimeLeft += "$seconds"

        b.countdownTextView.text = textTimeLeft
    }

    fun setCountdownProgress(p: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            b.progressTime.setProgress(p, true)
        } else {
            b.progressTime.progress = p
        }
    }

    private fun startCountdownTimer(timeLeftMillis: Long, timeElapsed: Int = 0) {

        i = timeElapsed

        mCountDownTimer = object : CountDownTimer(timeLeftMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                i++
                setCountdownProgress(i)
                updateTimer(millisUntilFinished)
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                i += 2
                setCountdownProgress(taskDurationMillis / 100)

                b.countdownTextView.text = "00:00"

                finished = true

                Handler(Looper.getMainLooper()).postDelayed({

                    //b.leaveFlow.visibility = View.VISIBLE

                    b.leaveFlow.fadeTo(true, 1000)

                    try {
                        val notification: Uri =
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r: Ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                        r.play()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    b.progressTime.apply {
                        trackColor = Color.parseColor("#00CD00")
                        setIndicatorColor(Color.parseColor("#00CD00"))
                    }

                    b.leaveFlow.setOnClickListener {
                        val returnIntent = Intent()
                        returnIntent.putExtra("done", true)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                    b.leaveFlow.visibility = View.VISIBLE
                }, 1000)
            }
        }
        mCountDownTimer.start()
    }

    fun modifyTimer(variation: Int = 0) {

    }
    
    @SuppressLint("SetTextI18n")
    private fun setTimer(taskDurationSec: Int) {

        i = 0

        taskDurationMillis = (taskDurationSec) * 1000

        b.progressTime.apply {
            isIndeterminate = false
            max = taskDurationMillis / 100
            progress = 0
        }

        startCountdownTimer(taskDurationMillis.toLong())

        val minutes = taskDurationMillis / 60000
        val seconds = (taskDurationMillis % 60000 / 1000)
        var textTimeTotal = ""
        if (minutes < 10) {
            textTimeTotal += "0"
        }
        textTimeTotal += "${minutes}:"
        if (seconds < 10) {
            textTimeTotal += "0"
        }
        textTimeTotal += "$seconds"

        b.totalTime.text = "Total $textTimeTotal"

        baseColor = b.progressTime.indicatorColor

        b.playPause.setOnClickListener {
            if (isPaused) play() else pause()
        }
        b.modifyTimer.setOnClickListener {

            // TODO : ADD OR MINUS TIME ON TIMER


        }
        b.backLeave.setOnClickListener {
            if (!finished) {

                pause()

                val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
                builder.apply {
                    setTitle("Quitter le Flow ?")
                    setMessage("Dommage de s'arrêter si près du but... Vraiment sûr(e) de vouloir quitter le Flow ?")
                    setPositiveButton(
                        R.string.ok
                    ) { _, _ -> finish() }
                    setNegativeButton(
                        R.string.cancel
                    ) { dialog, _ ->
                        dialog.dismiss()
                        play()
                    }
                    show()
                }
            } else {

                val returnIntent = Intent()
                returnIntent.putExtra("done", true)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }

    }

    private fun play() {
        isPaused = false
        b.playPause.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pause_black_24dp))
        startCountdownTimer(timeLeft, timeElapsedStop)

        b.progressTime.setIndicatorColor(baseColor[0])
    }

    private fun pause() {
        isPaused = true

        b.progressTime.setIndicatorColor(Color.parseColor("#E65100"))

        b.playPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.play_arrow_black_24dp))
        mCountDownTimer.cancel()

        timeElapsedStop = i
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.post(hideRunnable)
    }

    //var doubleBack = 0
    override fun onBackPressed() {

        if (!finished) {
            /*if (doubleBack < 1) doubleBack++
            else  super.onBackPressed() */

            pause()

            val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
            builder.apply {
                setTitle("Quitter le Flow ?")
                setMessage("Dommage de s'arrêter si près du but... Vraiment sûr(e) de vouloir quitter le Flow ?")
                setPositiveButton(
                    R.string.ok
                ) { _, _ -> finish() }
                setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                    play()
                }
                show()
            }
        } else {

            val returnIntent = Intent()
            returnIntent.putExtra("done", true)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    /*private fun toggle() {
        if (isFullscreen) hide() else show()
    }*/

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /*private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }*/

    /**
     * Schedules a call to hide() in delayMillis, canceling any
     * previously scheduled calls.
     */
    /*private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }*/

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}