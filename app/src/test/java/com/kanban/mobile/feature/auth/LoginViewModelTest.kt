package com.kanban.mobile.feature.auth

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

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
    fun loginSuccess_invokesCallbackAndClearsLoading() = runTest(dispatcher) {
        val repo = mockk<AuthRepository>()
        coEvery { repo.login("a@b.co", "secret") } returns Result.success(Unit)
        val vm = LoginViewModel(repo)
        vm.onEmailChange("a@b.co")
        vm.onPasswordChange("secret")
        var called = false
        vm.login { called = true }
        advanceUntilIdle()
        assertTrue(called)
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.login("a@b.co", "secret") }
    }

    @Test
    fun loginFailure_setsError() = runTest(dispatcher) {
        val repo = mockk<AuthRepository>()
        coEvery { repo.login(any(), any()) } returns Result.failure(IllegalStateException("bad"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("a@b.co")
        vm.onPasswordChange("secret")
        vm.login {}
        advanceUntilIdle()
        assertEquals("bad", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }
}
