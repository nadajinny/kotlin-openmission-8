package com.ndjinny.tagmoa.view

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long?.asDateLabel(): String {
    if (this == null) return ""
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return formatter.format(Date(this))
}

fun formatDateRange(
    startDate: Long?,
    endDate: Long?,
    fallbackDate: Long? = null
): String {
    val (start, end) = normalizeDateRange(startDate, endDate, fallbackDate)
    val startLabel = start.asDateLabel()
    val endLabel = end.asDateLabel()
    if (startLabel.isEmpty() && endLabel.isEmpty()) return ""
    if (startLabel.isNotEmpty() && endLabel.isNotEmpty()) {
        return if (startLabel == endLabel) {
            startLabel
        } else {
            "$startLabel ~ $endLabel"
        }
    }
    return startLabel.ifEmpty { endLabel }
}

fun normalizeDateRange(
    startDate: Long?,
    endDate: Long?,
    fallbackDate: Long? = null
): Pair<Long?, Long?> {
    var start = startDate
    var end = endDate
    if (start == null && end == null) {
        start = fallbackDate
        end = fallbackDate
    } else if (start == null) {
        start = end
    } else if (end == null) {
        end = start
    }
    if (start != null && end != null && start > end) {
        val tmp = start
        start = end
        end = tmp
    }
    return start to end
}

fun overlapsWithRange(
    startDate: Long?,
    endDate: Long?,
    rangeStart: Long,
    rangeEnd: Long,
    fallbackDate: Long? = null
): Boolean {
    val (start, end) = normalizeDateRange(startDate, endDate, fallbackDate)
    if (start == null || end == null) return false
    return end >= rangeStart && start <= rangeEnd
}
