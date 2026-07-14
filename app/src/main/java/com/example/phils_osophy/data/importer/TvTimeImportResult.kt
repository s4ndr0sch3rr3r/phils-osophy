package com.example.phils_osophy.data.importer

data class TvTimeImportResult(
    val filesRead: Int,
    val seriesImported: Int,
    val episodesImported: Int,
    val moviesImported: Int,
    val skippedTitles: List<String>
) {
    fun summary(): String = if (skippedTitles.isEmpty()) {
        "Imported ${importedSummary()} from $filesRead backup files."
    } else {
        "Imported ${importedSummary()}. Could not match ${skippedTitles.size} titles."
    }

    fun completionReport(): String = buildString {
        append("Imported ${importedSummary()} from $filesRead backup files.")
        if (skippedTitles.isEmpty()) {
            append("\n\nAll movie and series titles were found.")
        } else {
            append("\n\nCould not find ${skippedTitles.size} titles:")
            skippedTitles
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
                .forEach { title -> append("\n• $title") }
        }
    }

    private fun importedSummary(): String = listOf(
        "$seriesImported series",
        "$episodesImported watched episodes",
        "$moviesImported movies"
    ).joinToString(", ")
}
