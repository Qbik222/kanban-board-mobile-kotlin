package com.kanban.mobile.feature.teams

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
class TeamsListViewModelTest {

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
    fun loadSuccess_showsTeams() = runTest(dispatcher) {
        val repo = mockk<TeamsRepository>()
        coEvery { repo.listTeams() } returns Result.success(listOf(Team("1", "Alpha")))
        val vm = TeamsListViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.teams.size)
        assertEquals("Alpha", vm.uiState.value.teams.first().name)
        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun loadFailure_setsError() = runTest(dispatcher) {
        val repo = mockk<TeamsRepository>()
        coEvery { repo.listTeams() } returns Result.failure(IllegalStateException("network"))
        val vm = TeamsListViewModel(repo)
        advanceUntilIdle()
        assertEquals("network", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun createTeamSuccess_refreshesList() = runTest(dispatcher) {
        val repo = mockk<TeamsRepository>()
        var listCall = 0
        coEvery { repo.listTeams() } coAnswers {
            listCall++
            if (listCall == 1) {
                Result.success(listOf(Team("1", "Alpha")))
            } else {
                Result.success(listOf(Team("1", "Alpha"), Team("2", "Beta")))
            }
        }
        coEvery { repo.createTeam("Beta") } returns Result.success(Team("2", "Beta"))
        val vm = TeamsListViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.teams.size)
        vm.createTeam("Beta")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.teams.size)
        coVerify(exactly = 1) { repo.createTeam("Beta") }
        coVerify(atLeast = 2) { repo.listTeams() }
    }
}
