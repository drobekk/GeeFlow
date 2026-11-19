package app.geeflow.presentation.feature.device.dashboard.main

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeDescriptionSectionsTest {
    @Test
    fun separatesTechnicalCaveatsAndSourceWithoutDiscardingThem() {
        val intent = "Ramp flow during extraction."
        val caveat = "The limit leaves telemetry headroom."
        val instruction = "Stop at 60 ml."
        val source = "Source: https://example.test/recipe"
        val (main, details) = recipeDescriptionSections("$intent $caveat $instruction $source")
        assertEquals("$intent $instruction", main)
        assertTrue(caveat in details)
        assertTrue(source in details)
    }

    @Test
    fun unlabelledReferenceUrlMovesToDetails() {
        val (main, details) = recipeDescriptionSections("Bloom gently. https://example.test/recipe")
        assertEquals("Bloom gently.", main)
        assertTrue("https://example.test/recipe" in details)
    }

    @Test
    fun ordinaryRecipeInstructionsStayVisible() {
        val text = "Preinfuse for 10 seconds. Extract until 36 g."
        assertEquals(text to "", recipeDescriptionSections(text))
    }
}
