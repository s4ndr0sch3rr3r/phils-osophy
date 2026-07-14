package com.example.phils_osophy.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDbClientTest {

    @Test
    fun `search queries keep the full title and add useful fallbacks`() {
        val queries = mealDbSearchQueries(
            "Easy Chicken Curry with Rice (Family recipe)"
        )

        assertEquals(
            "Easy Chicken Curry with Rice (Family recipe)",
            queries.first()
        )
        assertTrue("Easy Chicken Curry with Rice" in queries)
        assertTrue("Easy Chicken Curry" in queries)
    }

    @Test
    fun `exact normalized meal name ranks before partial matches`() {
        val meals = listOf(
            meal(id = "1", name = "Chicken and Mushroom Hotpot"),
            meal(id = "2", name = "Chicken Curry"),
            meal(id = "3", name = "Thai Green Curry")
        )

        val ranked = rankMealDbMeals("chicken-curry", meals)

        assertEquals("2", ranked.first().id)
    }

    @Test
    fun `accent and punctuation differences still produce the closest match`() {
        val meals = listOf(
            meal(id = "1", name = "Creme Brulee"),
            meal(id = "2", name = "Cream Cheese Tart")
        )

        val ranked = rankMealDbMeals("Crème brûlée!", meals)

        assertEquals("1", ranked.first().id)
    }

    private fun meal(id: String, name: String) = MealDbMealDto(
        id = id,
        name = name,
        thumbnailUrl = "https://example.com/$id.jpg"
    )
}
