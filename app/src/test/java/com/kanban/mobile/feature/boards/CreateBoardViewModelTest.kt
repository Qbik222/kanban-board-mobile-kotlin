package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateBoardViewModelTest {

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
    fun emptyTitle_doesNotCallCreateBoard() = runTest(dispatcher) {
        val boardRepo = mockk<BoardRepository>(relaxed = true)
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { teamsRepo.listTeams() } returns Result.success(listOf(Team("t1", "Team A")))
        val vm = CreateBoardViewModel(
            SavedStateHandle(mapOf("teamId" to "")),
            boardRepo,
            teamsRepo,
        )
        advanceUntilIdle()
        vm.onTitleChange("   ")
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 0) { boardRepo.createBoard(any(), any(), any()) }
    }

    @Test
    fun submitSuccess_callsRepositoryWithParsedProjectIds() = runTest(dispatcher) {
        val boardRepo = mockk<BoardRepository>()
        val teamsRepo = mockk<TeamsRepository>()
        coEvery { teamsRepo.listTeams() } returns Result.success(listOf(Team("team-1", "Team A")))
        coEvery {
            boardRepo.createBoard("My board", "team-1", listOf("p1", "p2"))
        } returns Result.success(BoardSummary("b1", "My board", "team-1", "o1", listOf("p1", "p2")))

        val vm = CreateBoardViewModel(
            SavedStateHandle(mapOf("teamId" to "")),
            boardRepo,
            teamsRepo,
        )
        advanceUntilIdle()
        vm.onTitleChange("My board")
        vm.onProjectIdsTextChange(" p1 , p2 ")
        vm.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            boardRepo.createBoard("My board", "team-1", listOf("p1", "p2"))
        }
    }
}
