package com.electro.todolist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.electro.todolist.databinding.ActivityFlowBinding
import kotlin.random.Random

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlowBinding
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
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    private var timeLeft: Long = 0

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        fullscreenContent = binding.fullscreenContent
        //fullscreenContent.setOnClickListener { toggle() }

        fullscreenContentControls = binding.fullscreenContentControls

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

        binding.taskTitle.text = taskTitle
        binding.motivText.text = motivation[Random.nextInt(motivation.size)]

        timeLeft = taskDurationMillis.toLong()
        // TODO : pass as an argument in intent bundle

        updateTimer(timeLeft)

        setTimer(taskDurationSec)

        /*Handler(Looper.getMainLooper()).postDelayed({

        },800)*/
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

        binding.countdownTextView.text = textTimeLeft
    }

    fun setCountdownProgress(p: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.progressTime.setProgress(p, true)
        } else {
            binding.progressTime.progress = p
        }
    }

    fun startCountdownTimer(timeLeftMillis: Long, timeElapsed: Int = 0) {

        i = timeElapsed

        mCountDownTimer = object : CountDownTimer(timeLeftMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                i++
                setCountdownProgress(i)
                updateTimer(millisUntilFinished)
            }

            override fun onFinish() {
                i += 2
                setCountdownProgress(taskDurationMillis / 100)

                binding.countdownTextView.text = "00:00"

                finished = true

                Handler(Looper.getMainLooper()).postDelayed({

                    binding.leaveFlow.visibility = View.VISIBLE

                    try {
                        val notification: Uri =
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r: Ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                        r.play()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    binding.progressTime.trackColor = Color.parseColor("#00CD00")
                    binding.progressTime.setIndicatorColor(Color.parseColor("#00CD00"))

                    binding.leaveFlow.setOnClickListener {
                        val returnIntent = Intent()
                        returnIntent.putExtra("done", true)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                    binding.leaveFlow.visibility = View.VISIBLE
                }, 1000)
            }
        }
        mCountDownTimer.start()
    }

    fun setTimer(taskDurationSec: Int) {

        taskDurationMillis = (taskDurationSec) * 1000
        binding.progressTime.isIndeterminate = false
        binding.progressTime.max = taskDurationMillis / 100

        i = 0
        //binding.progressTime.trackCornerRadius = 2

        binding.progressTime.progress = 0

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

        binding.totalTime.text = "Total $textTimeTotal"
        binding.progressTime.max = (taskDurationMillis / 100)

        //isPaused = false

        baseColor = binding.progressTime.indicatorColor

        binding.playPause.setOnClickListener {
            if (isPaused) { // was paused, now playing
               /* Toast.makeText(
                    this,
                    "was paused now playing",
                    Toast.LENGTH_SHORT
                ).show()*/
                play()
            } else { // was playing, now paused
                /*Toast.makeText(
                    this,
                    "was playing now paused",
                    Toast.LENGTH_SHORT
                ).show()*/
                pause()
            }
        }

    }

    fun play() {
        isPaused = false
        binding.playPause.setImageDrawable(resources.getDrawable(R.drawable.pause_black_24dp))
        startCountdownTimer(timeLeft, timeElapsedStop)

        binding.progressTime.setIndicatorColor(baseColor[0])
    }

    fun pause() {
        isPaused = true
        //binding.progressTime.trackColor = Color.parseColor("#E65100")
        binding.progressTime.setIndicatorColor(Color.parseColor("#E65100"))

        binding.playPause.setImageDrawable(resources.getDrawable(R.drawable.play_arrow_black_24dp))
        mCountDownTimer.cancel()

        timeElapsedStop = i
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(0)
    }

    //var doubleBack = 0
    override fun onBackPressed() {

        if (!finished) {
            /*if (doubleBack < 1) {
                Toast.makeText(
                    this,
                    "Vraiment sûr(e) de vouloir quitter ton Flow ?",
                    Toast.LENGTH_LONG
                ).show()
                doubleBack++
            } else {
                super.onBackPressed()
            }*/

            /*Toast.makeText(
                this,
                "Vraiment sûr(e) de vouloir quitter ton Flow ?",
                Toast.LENGTH_LONG
            ).show()*/

            pause()

            val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
            builder.apply {
                setPositiveButton(
                    R.string.ok
                ) { dialog, id ->
                    // User clicked OK button
                    //super.onBackPressed()
                    finish()
                }
                setNegativeButton(
                    R.string.cancel
                ) { dialog, id ->
                    // User cancelled the dialog
                    dialog.dismiss()
                    play()
                }
            }
            // Set other dialog properties
            builder.setTitle("Quitter le Flow ?")
            builder.setMessage("Dommage de s'arrêter si près du but... Vraiment sûr(e) de vouloir quitter le Flow ?")

            // Create the AlertDialog
            builder.create()
            builder.show()
        } else {
            super.onBackPressed()
        }

    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

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