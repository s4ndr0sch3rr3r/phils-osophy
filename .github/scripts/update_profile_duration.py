from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "app/src/main/java/com/example/phils_osophy/ui/screens/ProfileScreen.kt"
text = path.read_text(encoding="utf-8")

old_constants = """private const val FallbackEpisodeRuntimeMinutes = 45
private val StatisticsDateFormatter = DateTimeFormatter.ofPattern(\"dd MMM yyyy, HH:mm\")
"""
new_constants = """private const val FallbackEpisodeRuntimeMinutes = 45
private const val MinutesPerHour = 60L
private const val MinutesPerDay = 24L * MinutesPerHour
private const val MinutesPerMonth = 30L * MinutesPerDay
private val StatisticsDateFormatter = DateTimeFormatter.ofPattern(\"dd MMM yyyy, HH:mm\")
"""

old_formatter = """private fun formatDuration(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60

    return when {
        hours == 0L -> \"$minutes min\"
        minutes == 0L -> \"$hours h\"
        else -> \"$hours h $minutes min\"
    }
}
"""
new_formatter = """private fun formatDuration(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val months = safeMinutes / MinutesPerMonth
    val minutesAfterMonths = safeMinutes % MinutesPerMonth
    val days = minutesAfterMonths / MinutesPerDay
    val minutesAfterDays = minutesAfterMonths % MinutesPerDay
    val hours = minutesAfterDays / MinutesPerHour
    val minutes = minutesAfterDays % MinutesPerHour

    return when {
        months > 0L -> formatDurationPair(months, \"mo\", days, \"d\")
        days > 0L -> formatDurationPair(days, \"d\", hours, \"h\")
        hours > 0L -> formatDurationPair(hours, \"h\", minutes, \"min\")
        else -> \"$minutes min\"
    }
}

private fun formatDurationPair(
    primaryValue: Long,
    primaryUnit: String,
    secondaryValue: Long,
    secondaryUnit: String
): String = if (secondaryValue > 0L) {
    \"$primaryValue $primaryUnit $secondaryValue $secondaryUnit\"
} else {
    \"$primaryValue $primaryUnit\"
}
"""

if old_constants not in text:
    raise RuntimeError("Profile duration constants anchor was not found")
if old_formatter not in text:
    raise RuntimeError("Existing profile duration formatter was not found")

text = text.replace(old_constants, new_constants, 1)
text = text.replace(old_formatter, new_formatter, 1)
path.write_text(text, encoding="utf-8")


def expected(total_minutes: int) -> str:
    safe = max(total_minutes, 0)
    months, after_months = divmod(safe, 30 * 24 * 60)
    days, after_days = divmod(after_months, 24 * 60)
    hours, minutes = divmod(after_days, 60)

    def pair(first: int, first_unit: str, second: int, second_unit: str) -> str:
        return f"{first} {first_unit} {second} {second_unit}" if second > 0 else f"{first} {first_unit}"

    if months > 0:
        return pair(months, "mo", days, "d")
    if days > 0:
        return pair(days, "d", hours, "h")
    if hours > 0:
        return pair(hours, "h", minutes, "min")
    return f"{minutes} min"


checks = {
    0: "0 min",
    20: "20 min",
    60: "1 h",
    260: "4 h 20 min",
    24 * 60: "1 d",
    3 * 24 * 60 + 6 * 60 + 20: "3 d 6 h",
    30 * 24 * 60: "1 mo",
    2 * 30 * 24 * 60 + 12 * 24 * 60 + 7 * 60: "2 mo 12 d",
}
for value, result in checks.items():
    actual = expected(value)
    if actual != result:
        raise RuntimeError(f"Unexpected duration for {value}: {actual!r} != {result!r}")
