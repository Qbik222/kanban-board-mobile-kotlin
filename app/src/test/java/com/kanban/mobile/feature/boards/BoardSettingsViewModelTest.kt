package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.teams.TeamMember
import com.kanban.mobile.feature.teams.TeamMemberRole
import com.kanban.mobile.feature.teams.TeamsRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBoard(ownerId: String = "owner-1"): BoardDetails =
        BoardDetails(
            id = "b1",
            title = "My board",
            teamId = "t1",
            ownerId = ownerId,
            projectIds = listOf("proj-a"),
            members = listOf(
                BoardMember("u2", BoardRole.EDITOR, email = "e2@test"),
            ),
            columns = emptyList(),
        )

    @Test
    fun loadSuccess_populatesDraftAndMembers() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(sampleBoard())
        val session = mockk<SessionRepository>()
        every { session.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner-1", email = "o@test"),
        )
        every { session.accessTokenFlow } returns flowOf(null)
        val teams = mockk<TeamsRepository>()
        coEvery { teams.listMembers("t1") } returns Result.success(emptyList())

        val vm = BoardSettingsViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            session,
            teams,
        )
        advanceUntilIdle()

        val st = vm.uiState.value
        assertEquals("My board", st.titleDraft)
        assertEquals(listOf("proj-a"), st.projectIds)
        assertEquals(1, st.members.size)
        assertEquals(BoardRole.OWNER, st.effectiveRole)
        assertFalse(st.loading)
    }

    @Test
    fun saveBoard_callsUpdateRepository() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(sampleBoard())
        coEvery { repo.updateBoard(any(), any(), any()) } returns Result.success(Unit)
        val session = mockk<SessionRepository>()
        every { session.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner-1", email = "o@test"),
        )
        every { session.accessTokenFlow } returns flowOf(null)
        val teams = mockk<TeamsRepository>()
        coEvery { teams.listMembers("t1") } returns Result.success(emptyList())

        val vm = BoardSettingsViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            session,
            teams,
        )
        advanceUntilIdle()

        vm.onTitleChange("Renamed")
        vm.saveBoard()
        advanceUntilIdle()

        coVerify {
            repo.updateBoard("b1", "Renamed", listOf("proj-a"))
        }
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun saveBoard_failure_emitsSnackbarEffect() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(sampleBoard())
        coEvery { repo.updateBoard(any(), any(), any()) } returns Result.failure(Exception("fail"))
        val session = mockk<SessionRepository>()
        every { session.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner-1", email = "o@test"),
        )
        every { session.accessTokenFlow } returns flowOf(null)
        val teams = mockk<TeamsRepository>()
        coEvery { teams.listMembers("t1") } returns Result.success(emptyList())

        val vm = BoardSettingsViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            session,
            teams,
        )
        advanceUntilIdle()

        vm.effects.test {
            vm.saveBoard()
            advanceUntilIdle()
            val item = awaitItem()
            assertTrue(item is BoardSettingsUiEffect.Snackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteBoard_success_emitsNavigateEffect() = runTest(dispatcher) {
        val repo = mockk<BoardRepository>()
        coEvery { repo.getBoard("b1") } returns Result.success(sampleBoard())
        coEvery { repo.deleteBoard("b1") } returns Result.success(Unit)
        val session = mockk<SessionRepository>()
        every { session.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "owner-1", email = "o@test"),
        )
        every { session.accessTokenFlow } returns flowOf(null)
        val teams = mockk<TeamsRepository>()
        coEvery { teams.listMembers("t1") } returns Result.success(
            listOf(TeamMember("owner-1", null, null, TeamMemberRole.ADMIN)),
        )

        val vm = BoardSettingsViewModel(
            SavedStateHandle(mapOf("boardId" to "b1")),
            repo,
            session,
            teams,
        )
        advanceUntilIdle()

        vm.effects.test {
            vm.deleteBoard()
            advanceUntilIdle()
            assertEquals(
                BoardSettingsUiEffect.NavigateToBoardsAfterDelete,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
