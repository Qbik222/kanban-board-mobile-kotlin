package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import com.kanban.mobile.core.realtime.BoardRealtimeClient
import com.kanban.mobile.core.realtime.BoardRealtimeEvent
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.teams.TeamsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
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
        val sessionRepository = mockk<SessionRepository>()
        val teamsRepository = mockk<TeamsRepository>()
        val details = BoardDetails(
            id = "b1",
            title = "Board",
            teamId = "t1",
            ownerId = "o1",
            projectIds = emptyList(),
            members = emptyList(),
            columns = listOf(
                BoardColumn(
                    id = "c1",
                    title = "Todo",
                    order = 0,
                    cards = listOf(
                        BoardCard(
                            id = "card1",
                            title = "Task",
                            description = "Desc",
                            priority = "HIGH",
                            columnId = "c1",
                            order = 0,
                            assigneeId = null,
                            projectIds = emptyList(),
                            comments = listOf(
                                CardComment("1", "a", null),
                                CardComment("2", "b", null),
                            ),
                            deadlineDueAt = null,
                        ),
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
        coEvery { repo.getBoard("b1") } returns Result.success(details)
        coEvery { teamsRepository.listMembers("t1") } returns Result.success(emptyList())
        every { sessionRepository.sessionState } returns MutableStateFlow(SessionState.Unauthenticated)
        every { sessionRepository.accessTokenFlow } returns flowOf(null)
        val realtime = mockk<BoardRealtimeClient>(relaxed = true)
        every { realtime.events } returns MutableSharedFlow<BoardRealtimeEvent>(extraBufferCapacity = 8)
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
        val sessionRepository = mockk<SessionRepository>()
        val teamsRepository = mockk<TeamsRepository>()
        coEvery { repo.getBoard("b1") } returns Result.failure(IllegalStateException("offline"))
        every { sessionRepository.sessionState } returns MutableStateFlow(SessionState.Unauthenticated)
        every { sessionRepository.accessTokenFlow } returns flowOf(null)
        val realtime = mockk<BoardRealtimeClient>(relaxed = true)
        every { realtime.events } returns MutableSharedFlow<BoardRealtimeEvent>(extraBufferCapacity = 8)
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
        assertEquals("offline", vm.uiState.value.error)
        assertNull(vm.uiState.value.board)
        assertFalse(vm.uiState.value.loading)
    }
}
