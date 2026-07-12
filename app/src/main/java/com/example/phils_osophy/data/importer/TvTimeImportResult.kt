package com.example.phils_osophy.data.importer

data class TvTimeImportResult(
    val filesRead: Int,
    val seriesImported: Int,
    val episodesImported: Int,
    val moviesImported: Int,
    val skippedTitles: List<String>
) {
    fun summary(): String {
        val imported = buildList {
            add("$seriesImported series")
            add("$episodesImported watched episodes")
            add("$moviesImported movies")
        }.joinToString(", ")

        return if (skippedTitles.isEmpty()) {
            "Imported $imported from $filesRead backup files."
        } else {
            "Imported $imported. Could not match ${skippedTitles.size} titles: " +
                skippedTitles.take(6).joinToString(", ") +
                if (skippedTitles.size > 6) "…" else ""
        }
    }
}
