/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.data

import android.content.Context
import com.evanescent.mytasks.R
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

    fun timestampToDate(timestamp: Long, context: Context): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val currentCal = Calendar.getInstance()
        currentCal.timeInMillis = System.currentTimeMillis()

        val today = currentCal.get(Calendar.DAY_OF_YEAR)
        val day = when(cal.get(Calendar.DAY_OF_YEAR)) {
            today - 2 -> context.getString(R.string.day_before_yesterday)
            today - 1 -> context.getString(R.string.yesterday)
            today -> context.getString(R.string.today)
            today + 1 -> context.getString(R.string.tomorrow)
            today + 2 -> context.getString(R.string.day_after_tomorrow)
            else -> SimpleDateFormat("EEE. d MMM", Locale.getDefault()).format(cal.time)
        }

        val at = context.getString(R.string.at)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        return "$day $at $time"
    }

    fun getDate(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.DAY_OF_MONTH)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.YEAR)}-${
            c.get(Calendar.HOUR_OF_DAY)
        }h${c.get(Calendar.MINUTE)}"
    }

    fun String.toSafeCase(): String = Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
        .filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ", "_")

fun timestampToDuration(timestamp : Long, context: Context): String {

    val taskDurationSec = (timestamp/1000).toInt()

    val hours = (taskDurationSec/ 3600 ) % 24
    val minutes = (taskDurationSec / 60) % 60
    val seconds = taskDurationSec % 60

    var durationText = ""
    if (hours != 0) {
        durationText += "${hours}${context.getString(R.string.duration_hour)}"
    }
    if (minutes != 0) {
        var space = ""
        if (durationText.isNotEmpty()) space = " "

        durationText += "${space}${minutes} ${context.getString(R.string.duration_minute)}"
    }
    if (seconds != 0) {
        var space = ""
        if (durationText.isNotEmpty()) space = " "

        durationText += "${space}${seconds}${context.getString(R.string.duration_second)}"
    }
    return durationText
}

