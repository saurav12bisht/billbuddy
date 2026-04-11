package com.android.billreminder.ui.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val dateFormatEn = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val dateTimeFormatEn = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
    private val dateFormatHi = SimpleDateFormat("d MMM yyyy", Locale("hi"))
    private val dateTimeFormatHi = SimpleDateFormat("d MMM yyyy HH:mm", Locale("hi"))

    fun formatDate(timestamp: Long, useHindi: Boolean = false): String {
        val format = if (useHindi) dateFormatHi else dateFormatEn
        return format.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long, useHindi: Boolean = false): String {
        val format = if (useHindi) dateTimeFormatHi else dateTimeFormatEn
        return format.format(Date(timestamp))
    }

    fun formatPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "(${digits.take(3)}) ${digits.substring(3, 6)}-${digits.takeLast(4)}"
            digits.length > 10 -> "(${digits.takeLast(10).take(3)}) ${digits.takeLast(7).take(3)}-${digits.takeLast(4)}"
            else -> phone
        }
    }

    fun formatMonthDayYear(timestamp: Long): String = dateFormatEn.format(Date(timestamp))
}
