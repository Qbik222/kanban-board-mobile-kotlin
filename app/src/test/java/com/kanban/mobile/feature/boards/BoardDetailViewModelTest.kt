package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSuccess_showsColumnsAndCards() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val details = BoardDetails(
            id = "b1",
            title = "Board",
            teamId = "t1",
            ownerId = "o1",
            projectIds = emptyList(),
            columns = listOf(
                BoardColumn(
                    id = "c1",
                    title = "Todo",
                    cards = listOf(
                        BoardCard(
                            id = "card1",
                            title = "Task",
                            description = "Desc",
                            priority = "HIGH",
                            commentCount = 2,
                            deadlineDueAt = null,
                        ),
                    ),
                ),
                BoardColumn(
                    id = "c2",
                    title = "Done",
                    cards = emptyList(),
                ),
            ),
        )
        coEvery { repo.getBoard("b1") } returns Result.success(details)
        val vm = BoardDetailViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
        )
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.board)
        assertEquals(2, vm.uiState.value.board!!.columns.size)
        assertEquals("Todo", vm.uiState.value.board!!.columns[0].title)
        assertEquals(1, vm.uiState.value.board!!.columns[0].cards.size)
        assertEquals("Task", vm.uiState.value.board!!.columns[0].cards.first().title)
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun loadFailure_setsError() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.getBoard("b1") } returns Result.failure(IllegalStateException("offline"))
        val vm = BoardDetailViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
        )
        advanceUntilIdle()
        assertEquals("offline", vm.uiState.value.error)
        assertNull(vm.uiState.value.board)
        assertFalse(vm.uiState.value.loading)
    }
}
