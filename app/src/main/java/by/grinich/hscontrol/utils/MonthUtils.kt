package by.grinich.hscontrol.utils

import java.util.Calendar
import java.util.Locale

object MonthUtils {

    fun currentMonth(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        return "$y-${m.toString().padStart(2, '0')}"
    }

    fun shiftMonth(month: String, delta: Int): String {
        val parts = month.split("-")
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        cal.add(Calendar.MONTH, delta)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        return "$y-${m.toString().padStart(2, '0')}"
    }

    fun monthDisplay(month: String): String {
        val parts = month.split("-")
        val y = parts[0]
        val m = parts[1].toInt()
        val names = arrayOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        return "${names[m - 1]} $y"
    }

    fun motivationVersion(month: String): Int {
        val parts = month.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        return when {
            y > 2026 -> 3
            y == 2026 && m >= 9 -> 3
            y == 2026 && m >= 6 -> 2
            else -> 1
        }
    }

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    fun defaultDateForMonth(month: String): String {
        val today = Calendar.getInstance()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val parts = month.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val lastDayCal = Calendar.getInstance().apply {
            set(y, m - 1, 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val lastDay = lastDayCal.get(Calendar.DAY_OF_MONTH)
        val safeDay = day.coerceAtMost(lastDay)
        return "$y-${m.toString().padStart(2, '0')}-${safeDay.toString().padStart(2, '0')}"
    }

    fun dateDisplay(date: String): String {
        val parts = date.split("-")
        if (parts.size != 3) return date
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        val d = parts[2].toInt()
        val m = parts[1].toInt()
        return "$d ${months[m - 1]} ${parts[0]}"
    }

    fun formatMoney(value: Double, decimals: Int = 2): String {
        val s = String.format(Locale.US, "%.${decimals}f", value)
        val parts = s.split(".")
        val intPart = parts[0].reversed().chunked(3).joinToString(" ").reversed()
        return if (decimals > 0) "$intPart,${parts[1]}" else intPart
    }
}