package com.kanban.mobile.feature.boards

import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardsListViewModelTest {

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
    fun loadSuccess_showsBoards() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.listBoards() } returns Result.success(
            listOf(
                BoardSummary("1", "Alpha", "t1", "o1", emptyList()),
            ),
        )
        val vm = BoardsListViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.boards.size)
        assertEquals("Alpha", vm.uiState.value.boards.first().title)
        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun loadFailure_setsError() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.listBoards() } returns Result.failure(IllegalStateException("network"))
        val vm = BoardsListViewModel(repo)
        advanceUntilIdle()
        assertEquals("network", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun refresh_callsListBoardsAgain() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        var call = 0
        coEvery { repo.listBoards() } coAnswers {
            call++
            Result.success(listOf(BoardSummary("$call", "B", "t", "o", emptyList())))
        }
        val vm = BoardsListViewModel(repo)
        advanceUntilIdle()
        assertEquals("1", vm.uiState.value.boards.first().id)
        vm.refresh()
        advanceUntilIdle()
        assertEquals("2", vm.uiState.value.boards.first().id)
        coVerify(atLeast = 2) { repo.listBoards() }
    }
}
