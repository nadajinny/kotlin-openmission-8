package com.example.tagmoa.view

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long?.asDateLabel(): String {
    if (this == null) return ""
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return formatter.format(Date(this))
}
