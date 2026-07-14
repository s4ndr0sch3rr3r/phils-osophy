package com.example.phils_osophy.data.remote

import com.example.phils_osophy.BuildConfig
import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class MealDbSearchResponse(
    val meals: List<MealDbMealDto>? = null
)

@Serializable
data class MealDbMealDto(
    @SerialName("idMeal")
    val id: String,
    @SerialName("strMeal")
    val name: String,
    @SerialName("strMealThumb")
    val thumbnailUrl: String? = null
)

private interface MealDbApi {

    @GET("api/json/v1/{apiKey}/search.php")
    suspend fun searchMeals(
        @Path("apiKey") apiKey: String,
        @Query("s") query: String
    ): MealDbSearchResponse
}

object MealDbClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val api = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/")
        .addConverterFactory(
            json.asConverterFactory(
                "application/json".toMediaType()
            )
        )
        .build()
        .create(MealDbApi::class.java)

    suspend fun searchMeals(
        query: String,
        maxResults: Int = 12
    ): List<MealDbMealDto> {
        val cleanQuery = query
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { value -> value.isNotBlank() }
            ?: return emptyList()
        val candidates = linkedMapOf<String, MealDbMealDto>()

        for (searchQuery in mealDbSearchQueries(cleanQuery)) {
            api.searchMeals(
                apiKey = BuildConfig.MEALDB_API_KEY
                    .trim()
                    .ifBlank { DEFAULT_MEAL_DB_API_KEY },
                query = searchQuery
            ).meals.orEmpty()
                .filter { meal ->
                    meal.id.isNotBlank() && meal.name.isNotBlank()
                }
                .forEach { meal ->
                    candidates.putIfAbsent(meal.id, meal)
                }

            if (candidates.values.any { meal ->
                    normalizeMealDbTitle(meal.name) ==
                        normalizeMealDbTitle(cleanQuery)
                }
            ) {
                break
            }
        }

        return rankMealDbMeals(cleanQuery, candidates.values)
            .take(maxResults.coerceAtLeast(1))
    }

    suspend fun findClosestMeal(query: String): MealDbMealDto? =
        searchMeals(query = query, maxResults = 1).firstOrNull()
}

internal fun mealDbSearchQueries(title: String): List<String> {
    val cleanTitle = title
        .trim()
        .replace(Regex("\\s+"), " ")
    if (cleanTitle.isBlank()) {
        return emptyList()
    }

    val withoutQualifier = cleanTitle
        .replace(
            Regex("""\s*[\[(][^\])]+[\])]\s*$"""),
            ""
        )
        .trim()
    val mainPhrase = withoutQualifier
        .split(
            Regex(
                pattern = """\s+(?:with|and|&|avec|et|au|aux|à|a la|à la)\s+""",
                option = RegexOption.IGNORE_CASE
            ),
            limit = 2
        )
        .first()
        .trim()
    val meaningfulWords = Regex("[\\p{L}\\p{N}]+")
        .findAll(withoutQualifier)
        .map { match -> match.value }
        .filter { word ->
            word.length > 2 &&
                word.lowercase(Locale.ROOT) !in MealDbStopWords
        }
        .take(3)
        .toList()
        .joinToString(" ")

    return listOf(
        cleanTitle,
        withoutQualifier,
        mainPhrase,
        meaningfulWords
    ).filter { query -> query.isNotBlank() }
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(MAX_MEAL_DB_SEARCH_QUERIES)
}

internal fun rankMealDbMeals(
    query: String,
    meals: Collection<MealDbMealDto>
): List<MealDbMealDto> = meals
    .distinctBy { meal -> meal.id }
    .sortedWith(
        compareByDescending<MealDbMealDto> { meal ->
            mealDbMatchScore(query, meal.name)
        }.thenBy { meal -> meal.name.lowercase(Locale.ROOT) }
    )

private fun mealDbMatchScore(query: String, candidate: String): Double {
    val normalizedQuery = normalizeMealDbTitle(query)
    val normalizedCandidate = normalizeMealDbTitle(candidate)
    if (normalizedQuery.isBlank() || normalizedCandidate.isBlank()) {
        return 0.0
    }
    if (normalizedQuery == normalizedCandidate) {
        return 10_000.0
    }

    val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }.toSet()
    val candidateTokens = normalizedCandidate.split(' ').filter { it.isNotBlank() }.toSet()
    val sharedTokens = queryTokens.intersect(candidateTokens).size.toDouble()
    val tokenDice = if (queryTokens.isEmpty() || candidateTokens.isEmpty()) {
        0.0
    } else {
        (2.0 * sharedTokens) / (queryTokens.size + candidateTokens.size)
    }
    val containmentScore = when {
        normalizedCandidate.contains(normalizedQuery) -> 1.0
        normalizedQuery.contains(normalizedCandidate) -> 0.9
        else -> 0.0
    }
    val prefixScore = if (
        normalizedCandidate.startsWith(normalizedQuery) ||
        normalizedQuery.startsWith(normalizedCandidate)
    ) {
        1.0
    } else {
        0.0
    }
    val bigramScore = bigramDice(normalizedQuery, normalizedCandidate)

    return tokenDice * 600.0 +
        bigramScore * 300.0 +
        containmentScore * 200.0 +
        prefixScore * 100.0
}

private fun bigramDice(first: String, second: String): Double {
    if (first.length < 2 || second.length < 2) {
        return if (first == second) 1.0 else 0.0
    }

    val firstBigrams = first.windowed(2).toMutableList()
    val secondBigrams = second.windowed(2).toMutableList()
    var matches = 0

    firstBigrams.forEach { bigram ->
        val matchIndex = secondBigrams.indexOf(bigram)
        if (matchIndex >= 0) {
            matches += 1
            secondBigrams.removeAt(matchIndex)
        }
    }

    return (2.0 * matches) /
        (firstBigrams.size + second.windowed(2).size)
}

private fun normalizeMealDbTitle(value: String): String {
    val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    return withoutAccents
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

private const val DEFAULT_MEAL_DB_API_KEY = "1"
private const val MAX_MEAL_DB_SEARCH_QUERIES = 3

private val MealDbStopWords = setOf(
    "recipe",
    "easy",
    "homemade",
    "dish",
    "meal",
    "the",
    "and",
    "with",
    "une",
    "recette",
    "facile",
    "maison",
    "les",
    "des",
    "avec",
    "pour"
)
