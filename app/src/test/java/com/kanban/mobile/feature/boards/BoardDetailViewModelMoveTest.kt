package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import com.kanban.mobile.core.realtime.BoardRealtimeClient
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.teams.TeamMember
import com.kanban.mobile.feature.teams.TeamMemberRole
import com.kanban.mobile.feature.teams.TeamsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardDetailViewModelMoveTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun board(): BoardDetails =
        BoardDetails(
            id = "b1",
            title = "Board",
            teamId = "t1",
            ownerId = "owner",
            projectIds = emptyList(),
            members = emptyList(),
            columns = listOf(
                BoardColumn(
                    id = "c1",
                    title = "Todo",
                    order = 0,
                    cards = listOf(
                        card("k1", "c1", 0),
                        card("k2", "c1", 1),
                    ),
                ),
                BoardColumn(
                    id = "c2",
                    title = "Done",
                    order = 1,
                    cards = emptyList(),
                ),
            ),
        )

    private fun card(id: String, columnId: String, order: Int): BoardCard =
        BoardCard(
            id = id,
            title = id,
            description = null,
            priority = null,
            columnId = columnId,
            order = order,
            assigneeId = null,
            projectIds = emptyList(),
            comments = emptyList(),
            deadlineDueAt = null,
        )

    @Test
    fun moveCard_failure_rolls_back_snapshot() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val sessionRepository = mockk<SessionRepository>()
        val teamsRepository = mockk<TeamsRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(board())
        coEvery { repo.moveCard(any(), any(), any()) } returns Result.failure(IllegalStateException("net"))
        coEvery { teamsRepository.listMembers("t1") } returns Result.success(
            listOf(TeamMember("owner", null, null, TeamMemberRole.ADMIN)),
        )
        every { sessionRepository.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner", email = "a@a.com"),
        )
        every { sessionRepository.accessTokenFlow } returns flowOf("token")
        val realtime = mockk<BoardRealtimeClient>(relaxed = true)
        every { realtime.events } returns flowOf()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }

        val vm = BoardDetailViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            sessionRepository,
            teamsRepository,
            realtime,
            json,
        )
        advanceUntilIdle()

        val before = vm.uiState.value.board!!
        vm.moveCardToColumn("k1", "c2")
        advanceUntilIdle()

        assertEquals(before, vm.uiState.value.board)
    }

    @Test
    fun createColumn_success_updates_state() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        val sessionRepository = mockk<SessionRepository>()
        val teamsRepository = mockk<TeamsRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(board())
        coEvery { repo.createColumn("b1", "New") } returns Result.success(
            BoardColumn(
                id = "c99",
                title = "New",
                order = 2,
                cards = emptyList(),
            ),
        )
        coEvery { teamsRepository.listMembers("t1") } returns Result.success(emptyList())
        every { sessionRepository.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner", email = null),
        )
        every { sessionRepository.accessTokenFlow } returns flowOf("token")
        val realtime = mockk<BoardRealtimeClient>(relaxed = true)
        every { realtime.events } returns flowOf()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }

        val vm = BoardDetailViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            sessionRepository,
            teamsRepository,
            realtime,
            json,
        )
        advanceUntilIdle()

        vm.createColumn("New")
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.board!!.columns.size)
        assertEquals("New", vm.uiState.value.board!!.columns.last().title)
    }
}
