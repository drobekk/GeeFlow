package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrewHistoryDataUseCaseTest {
    private val history = mockk<BrewHistoryRepository>(relaxUnitFun = true)
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    @Test
    fun `when dependency contains data then returns requested data`() = runTest {
        every { history.getBrewDataPoints(3) } returns mapOf(0f to point)
        val useCase = GetBrewHistoryDataUseCase(history)

        val result = useCase(3)

        assertEquals(mapOf(0f to point), result)
    }
}
