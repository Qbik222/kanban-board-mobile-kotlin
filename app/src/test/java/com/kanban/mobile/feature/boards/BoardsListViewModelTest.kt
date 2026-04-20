package com.kanban.mobile.feature.boards

import com.kanban.mobile.feature.teams.Team
import com.kanban.mobile.feature.teams.TeamsRepository
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
import org.junit.Assert.assertNull
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
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { repo.listBoards() } returns Result.success(
            listOf(
                BoardSummary("1", "Alpha", "t1", "o1", emptyList()),
            ),
        )
        coEvery { teamsRepo.listTeams() } returns Result.success(emptyList())
        val vm = BoardsListViewModel(repo, teamsRepo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.boards.size)
        assertEquals("Alpha", vm.uiState.value.boards.first().title)
        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun loadFailure_setsError() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { repo.listBoards() } returns Result.failure(IllegalStateException("network"))
        coEvery { teamsRepo.listTeams() } returns Result.success(emptyList())
        val vm = BoardsListViewModel(repo, teamsRepo)
        advanceUntilIdle()
        assertEquals("network", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun refresh_callsListBoardsAgain() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { teamsRepo.listTeams() } returns Result.success(emptyList())
        var call = 0
        coEvery { repo.listBoards() } coAnswers {
            call++
            Result.success(listOf(BoardSummary("$call", "B", "t", "o", emptyList())))
        }
        val vm = BoardsListViewModel(repo, teamsRepo)
        advanceUntilIdle()
        assertEquals("1", vm.uiState.value.boards.first().id)
        vm.refresh()
        advanceUntilIdle()
        assertEquals("2", vm.uiState.value.boards.first().id)
        coVerify(atLeast = 2) { repo.listBoards() }
    }

    @Test
    fun filterByTeam_showsOnlyMatchingBoards() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { teamsRepo.listTeams() } returns Result.success(
            listOf(Team("t1", "T1"), Team("t2", "T2")),
        )
        coEvery { repo.listBoards() } returns Result.success(
            listOf(
                BoardSummary("1", "A", "t1", "o", emptyList()),
                BoardSummary("2", "B", "t2", "o", emptyList()),
            ),
        )
        val vm = BoardsListViewModel(repo, teamsRepo)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.visibleBoards.size)
        vm.setFilterTeamId("t1")
        assertEquals(1, vm.uiState.value.visibleBoards.size)
        assertEquals("1", vm.uiState.value.visibleBoards.first().id)
    }

    @Test
    fun filterAll_nullTeamId_showsAll() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { teamsRepo.listTeams() } returns Result.success(emptyList())
        coEvery { repo.listBoards() } returns Result.success(
            listOf(BoardSummary("1", "A", "t1", "o", emptyList())),
        )
        val vm = BoardsListViewModel(repo, teamsRepo)
        advanceUntilIdle()
        vm.setFilterTeamId("t1")
        vm.setFilterTeamId(null)
        assertNull(vm.uiState.value.filterTeamId)
        assertEquals(1, vm.uiState.value.visibleBoards.size)
    }
}
