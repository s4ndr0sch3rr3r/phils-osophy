package com.example.phils_osophy.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val storedDateFormatter = DateTimeFormatter.ofPattern(
    "MMMM d, yyyy",
    Locale.ENGLISH
)

fun formatStoredDate(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) {
        return "Unknown"
    }

    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(storedDateFormatter)
}
