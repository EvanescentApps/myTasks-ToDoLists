/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.electro.todolist.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SerialListObject(val id: String, val title: String, var position: Int, var isCurrentSelected: Boolean = false)