package com.example.tagmoa.model

data class Tag(
    var id: String = "",
    var name: String = ""
)

data class MainTask(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var dueDate: Long? = null,
    var duration: String = "",
    var mainColor: String = "#559999",
    var tagIds: MutableList<String> = mutableListOf()
)

data class SubTask(
    var id: String = "",
    var mainTaskId: String = "",
    var content: String = "",
    var priority: Int = 0,
    var dueDate: Long? = null
)
