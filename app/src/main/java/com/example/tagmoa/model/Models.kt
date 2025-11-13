package com.example.tagmoa.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

data class Tag(
    var id: String = "",
    var name: String = ""
)

@IgnoreExtraProperties
data class MainTask(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var startDate: Long? = null,
    var endDate: Long? = null,
    var dueDate: Long? = null,
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    var completedAt: Long? = null,
    var manualSchedule: Boolean = false,
    var mainColor: String = "#559999",
    var tagIds: MutableList<String> = mutableListOf()
)

@IgnoreExtraProperties
data class SubTask(
    var id: String = "",
    var mainTaskId: String = "",
    var content: String = "",
    var priority: Int = 0,
    var startDate: Long? = null,
    var endDate: Long? = null,
    var dueDate: Long? = null,
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    var completedAt: Long? = null
)
