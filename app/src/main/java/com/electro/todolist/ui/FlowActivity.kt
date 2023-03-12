package com.electro.todolist.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.electro.todolist.R
import com.electro.todolist.databinding.ActivityFlowBinding
import com.electro.todolist.fadeTo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private lateinit var baseColor: IntArray

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
        val motivation = listOf(
            "Reste motivé(e), c'est bientôt fini !",
            "C'est parti ! Hâte de voir le résultat ?",
            "« Vis comme si tu devais mourir demain, apprends comme si tu devais vivre toujours. », Gandhi.",
            "« Le succès n'est pas final, l'échec n'est pas fatal. C'est le courage de continuer qui compte. », Winston Churchill.",
            "« Tout est possible à qui rêve, ose, travaille et n'abandonne jamais. », Xavier Dolan.",
            "« La seule limite à notre épanouissement de demain sera nos doutes d'aujourd'hui. », Franklin Delano Roosevelt.",
            "« Ceux qui ne font rien ne se trompent jamais. », Théodore de Banville.",
            "« Je ne perds jamais. Soit je gagne, soit j'apprends. », Nelson Mandela.",
            "« Vous ne pouvez pas être ce gamin qui reste figé en haut du toboggan en réfléchissant. Vous devez glisser. », Tina Fey.",
            "« J'ai appris il y a longtemps qu'il y a quelque chose de pire que de rater l'objectif : ne pas passer à l'action. », Mia Hamm.",
            "« Un jour, tu te réveilleras et tu n'auras plus le temps de faire ce que tu voulais faire. Fais-le donc maintenant. », Paulo Coelho.",
            "« En suivant le chemin qui s'appelle plus tard, nous arrivons sur la place qui s'appelle jamais. », Sénèque.",
            "« Plus tard, il sera trop tard. Notre vie, c'est maintenant. », Jacques Prévert.",
            "« Dans 20 ans, tu seras plus déçu par les choses que tu n'as pas faites que par celles que tu auras faites. Alors, sors des sentiers battus. Mets les voiles. Explore. Rêve. Découvre. », Mark Twain.",
            "« Prends le temps de réfléchir, mais lorsque c'est le moment de passer à l'action, cesse de penser et vas-y. », Andrew Jackson.",
            "« Croyez en vos rêves et ils se réaliseront peut-être. Croyez en vous et ils se réaliseront sûrement. », Martin Luther King.",
            "« Le succès, c'est vous aimer vous-même, c'est aimer ce que vous faites et c'est aimer comment vous le faites. », Maya Angelou.",
            "« Tu ne sais jamais à quel point tu es fort, jusqu'au jour où être fort reste ta seule option. », Bob Marley.",
            "« Il y a au fond de vous de multiples petites étincelles de potentialités ; elles ne demandent qu'un souffle pour s'enflammer en de magnifiques réussites. », Wilferd Arlan Peterson.",
            "« Soyez vous-même, tous les autres sont déjà pris. », Oscar Wilde.",
            "« Le but de la vie, ce n'est pas l'espoir de devenir parfait, c'est la volonté d'être toujours meilleur. », Ralph Waldo Emerson.",
            "« Il n'y a qu'une façon d'échouer, c'est d'abandonner avant d'avoir réussi. », Georges Clemenceau.",
            "« Il y a plus de courage que de talent dans la plupart des réussites. », Félix Leclerc.",
            "« Ce n'est pas grave si vous avancez lentement, du moment que vous ne vous arrêtez pas. », Confucius.",
            "« La gloire n'est pas de ne jamais tomber, mais de se relever chaque fois que l'on tombe. », Confucius.",
            "« Le succès n'est pas final. L'échec n'est pas fatal. C'est le courage de continuer qui compte. », Winston Churchill.",
            "« Il faut viser la lune, parce qu'au moins, si vous échouez, vous finirez dans les étoiles. », Oscar Wilde.",
            "« La sagesse suprême est d'avoir des rêves assez grands pour ne pas les perdre du regard tandis qu'on les poursuit. », William Faulkner.",
            "« Les meilleures choses qui arrivent dans le monde de l'entreprise ne sont pas le résultat du travail d'un seul homme. C'est le travail de toute une équipe. », Steve Jobs.",
            "« Certains veulent que ça arrive, d'autres aimeraient que ça arrive et quelques-uns font en sorte que ça arrive. », Michael Jordan.",
            "« La réussite appartient à tout le monde. C'est au travail d'équipe qu'en revient le mérite. », Franck Piccard.",
            "« Aucun de nous ne sait ce que nous savons tous, ensemble. », Euripide.",
            "« Se réunir est un début, rester ensemble est un progrès, travailler ensemble est la réussite. », Henry Ford.",
            "« L'excellence ne résulte pas d'une impulsion isolée, mais d'une succession de petits éléments qui sont réunis. », Vincent Van Gogh."
        )


        // Task & Flow duration in seconds

        taskDurationMillis = intent.getIntExtra("taskDuration", 900) * 1000

        val taskTitle = intent.getStringExtra("taskTitle")

        b.taskTitle.text = taskTitle
        b.motivText.text = motivation.random()

        timeLeft = taskDurationMillis.toLong()
        // TODO : pass as an argument in intent bundle

        updateTimer(timeLeft)

        setTimer(taskDurationMillis / 1000)
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

                b.leaveFlow.fadeTo(true, 1000)

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
                    b.countdownTextView.visibility = View.GONE
                    b.totalTime.visibility = View.GONE
                    b.playPause.visibility = View.GONE
                    b.modifyTimer.visibility = View.GONE
                    b.doneAnimation.playAnimation()
                    // TODO : START LOTTIE ANIMATION
                }, 700)
            }
        }
        mCountDownTimer.start()
    }

    private fun setDurationDialog(currentTaskDuration: Int) {

        pause()
        val elapsed = taskDurationMillis - timeLeft


        val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)

        val viewInflated: View = LayoutInflater.from(this)
            .inflate(R.layout.choose_duration, window.decorView.rootView as ViewGroup?, false)

        val hoursPicker = viewInflated.findViewById<NumberPicker>(R.id.hours)
        val minutesPicker = viewInflated.findViewById<NumberPicker>(R.id.minutes)
        val secondsPicker = viewInflated.findViewById<NumberPicker>(R.id.seconds)

        builder.apply {
            setView(viewInflated)
            setTitle("Modifier la Durée")
            setMessage("Définissez une durée pour cette tâche")
            setCancelable(false)

            setPositiveButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.dismiss()

                val hoursPicked = hoursPicker.value
                val minutesPicked = minutesPicker.value
                val secondsPicked = secondsPicker.value

                var durationText = ""
                if (hoursPicked != 0) {
                    durationText += "${hoursPicked}h"
                }
                if (minutesPicked != 0) {
                    val space = if (durationText.isNotEmpty()) " " else ""
                    durationText += "${space}${minutesPicked} min"
                }
                if (secondsPicked != 0) {
                    val space = if (durationText.isNotEmpty()) " " else ""
                    durationText += "${space}${secondsPicked}s"
                }

                val timeDurationSeconds = hoursPicked * 3600 + minutesPicked * 60 + secondsPicked
                val timeDurationMillis = timeDurationSeconds * 1000

                taskDurationMillis = timeDurationMillis
                timeLeft = taskDurationMillis - elapsed
                updateTimer(timeLeft)
                b.progressTime.apply {
                    isIndeterminate = false
                    max = taskDurationMillis / 100
                    progress = (elapsed / 100).toInt()
                }
                play()


                if (durationText.isNotBlank()) b.totalTime.text = "Total $durationText"
            }

            setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                dialog.cancel()
                play()
            }

            show()

            val secondsDuration = currentTaskDuration % 60
            val minutesDuration = (currentTaskDuration / 60) % 60
            val hoursDuration: Int = (currentTaskDuration / 3600) % 24

            hoursPicker.apply {
                minValue = 0
                maxValue = 48
                value = hoursDuration

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textSize = 100f
                    textColor = ContextCompat.getColor(this.context, R.color.textContent)
                }
            }

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
        if (minutes < 10) textTimeTotal += "0"
        textTimeTotal += "${minutes}:"
        if (seconds < 10) textTimeTotal += "0"
        textTimeTotal += "$seconds"

        b.totalTime.text = "Total $textTimeTotal"

        baseColor = b.progressTime.indicatorColor

        b.playPause.setOnClickListener {
            if (isPaused) play() else pause()
        }

        b.modifyTimer.setOnClickListener {
            setDurationDialog(taskDurationMillis / 1000)
            /*val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            builder.apply {
                setTitle("Fonctionnalité en cours de développement")
                setMessage("Cette fonctionnalité n'est pas disponible pour l'instant, ce sera pour une prochaine mise à jour.")
                setPositiveButton("D'accord") { dialog, _ ->
                    dialog.dismiss()
                }
                show()
            }*/

            /*val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            val viewInflated: View = LayoutInflater.from(this)
                .inflate(R.layout.choose_duration, window.decorView.rootView as ViewGroup?, false)

            val hoursPicker = viewInflated.findViewById<NumberPicker>(R.id.hours)
            val minutesPicker = viewInflated.findViewById<NumberPicker>(R.id.minutes)
            val secondsPicker = viewInflated.findViewById<NumberPicker>(R.id.seconds)

            builder.apply {
                setView(viewInflated)
                title = "Définir la Durée"
                setMessage("Définissez une durée pour cette tâche")
                setCancelable(false)

                setPositiveButton(
                    R.string.ok
                ) { dialog, _ ->
                    dialog.dismiss()

                    val hoursVal = hoursPicker.value
                    val minutesVal = minutesPicker.value
                    val secondsVal = secondsPicker.value

                    var durationText = ""
                    if (hoursVal != 0) {
                        durationText += "${hoursVal}h"
                    }
                    if (minutesVal != 0) {
                        val space = if (durationText.isNotEmpty()) " " else ""
                        durationText += "${space}${minutesVal} min"
                    }
                    if (secondsVal != 0) {
                        val space = if (durationText.isNotEmpty()) " " else ""
                        durationText += "${space}${secondsVal}s"
                    }

                    val timeDurationSeconds = hoursVal * 3600 + minutesVal * 60 + secondsVal
                    val timeDurationMillis = timeDurationSeconds * 1000

                    val changedTime = timeDurationMillis.toLong()

                    //if (durationText.isNotBlank()) b.setDuration.text = durationText
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
                    taskDurationSec = (taskDurationMillis / 1000).toInt()

                    secondsDuration = taskDurationSec % 60
                    minutesDuration = (taskDurationSec / 60) % 60
                    hoursDuration = (taskDurationSec / 3600) % 24
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
            }*/
        }
        b.backLeave.setOnClickListener {
            if (!finished) {
                val wasPaused = isPaused
                pause()

                val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                builder.apply {
                    setTitle("Quitter le Flow ?")
                    setMessage("Dommage de s'arrêter si près du but... Vraiment sûr(e) de vouloir quitter le Flow ?")
                    setPositiveButton("Quitter") { _, _ ->
                        finish()
                    }
                    setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                        if (!wasPaused) play()
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
        b.playPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause_black_24dp))
        startCountdownTimer(timeLeft, timeElapsedStop)
        b.progressTime.setIndicatorColor(baseColor[0])
    }

    private fun pause() {
        isPaused = true

        b.progressTime.setIndicatorColor(Color.parseColor("#E65100"))

        b.playPause.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.play_arrow_black_24dp
            )
        )
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
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (!finished) {
            /*if (doubleBack < 1) doubleBack++
            else  super.onBackPressed() */

            val wasPaused = isPaused
            pause()

            val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            builder.apply {
                setTitle("Quitter le Flow ?")
                setMessage("Dommage de s'arrêter si près du but... Vraiment sûr(e) de vouloir quitter le Flow ?")
                setPositiveButton(
                    "Quitter"
                ) { _, _ -> finish() }
                setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.dismiss()

                    if (!wasPaused) play()
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