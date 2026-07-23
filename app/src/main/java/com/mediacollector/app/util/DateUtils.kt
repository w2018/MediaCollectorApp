package com.mediacollector.app.util

import java.text.SimpleDateFormat
import java.util.*

/** 格式化时间戳为显示字符串 */
object DateUtils {
    private val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    /** 从数据库格式字符串解析 */
    fun fromDbString(dateStr: String): Long {
        return try {
            dbFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /** 格式化显示 */
    fun formatDisplay(dateStr: String): String {
        val ms = fromDbString(dateStr)
        return displayFormat.format(Date(ms))
    }

    /** 时间戳格式化 */
    fun formatTimestamp(ms: Long): String {
        return displayFormat.format(Date(ms))
    }

    /** 简短时间（今天显示时间，昨天显示"昨天"，更早显示日期） */
    fun formatRelative(ms: Long): String {
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = ms
        val targetDay = calendar.get(Calendar.DAY_OF_YEAR)
        val targetYear = calendar.get(Calendar.YEAR)

        return when {
            targetYear != Calendar.getInstance().get(Calendar.YEAR) -> dateFormat.format(Date(ms))
            targetDay == today -> "今天 ${timeFormat.format(Date(ms))}"
            targetDay == today - 1 -> "昨天 ${timeFormat.format(Date(ms))}"
            else -> dateFormat.format(Date(ms))
        }
    }

    /** 日期分组（今天/昨天/更早） */
    fun groupDate(ms: Long): String {
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = ms
        val targetDay = calendar.get(Calendar.DAY_OF_YEAR)

        return when {
            targetDay == today -> "今天"
            targetDay == today - 1 -> "昨天"
            else -> dateFormat.format(Date(ms))
        }
    }
}
