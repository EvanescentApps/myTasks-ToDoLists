/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.data

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

    fun timestampToDate(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

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

        return "$day à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)}"
    }

    fun getDate(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.DAY_OF_MONTH)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.YEAR)}-${
            c.get(Calendar.HOUR_OF_DAY)
        }h${c.get(Calendar.MINUTE)}"
    }

    fun String.toSafeCase(): String = Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
        .filter { it.isLetterOrDigit() or it.isWhitespace() }.replace(" ", "_")

fun timestampToDuration(timestamp : Long): String {

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