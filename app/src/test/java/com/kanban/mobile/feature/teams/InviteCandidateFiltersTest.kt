package com.kanban.mobile.feature.teams

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCandidateFiltersTest {

    private val samples = listOf(
        InviteCandidate("u-yara", "yara@test.com", "Yara"),
        InviteCandidate("u-alice", "alice@test.com", "Alice"),
        InviteCandidate("uuid-100", null, null),
        InviteCandidate("yoshi-id", "x@x.com", "Ren"),
    )

    @Test
    fun emptyQuery_returnsEmpty() {
        assertEquals(emptyList<InviteCandidate>(), samples.filterByInvitePrefix(""))
        assertEquals(emptyList<InviteCandidate>(), samples.filterByInvitePrefix("   "))
    }

    @Test
    fun filtersByEmailPrefix_ignoreCase() {
        val out = samples.filterByInvitePrefix("alice")
        assertEquals(1, out.size)
        assertEquals("u-alice", out.first().userId)
    }

    @Test
    fun filtersByNamePrefix_ignoreCase() {
        val out = samples.filterByInvitePrefix("yar")
        assertEquals(1, out.size)
        assertEquals("Yara", out.first().name)
    }

    @Test
    fun filtersByUserIdPrefix() {
        val out = samples.filterByInvitePrefix("uuid")
        assertEquals(1, out.size)
        assertEquals("uuid-100", out.first().userId)
    }

    @Test
    fun singleCharacter_prefixOnAnyField() {
        val out = samples.filterByInvitePrefix("y")
        assertEquals(2, out.size)
        assertTrue(out.any { it.userId == "u-yara" })
        assertTrue(out.any { it.userId == "yoshi-id" })
    }

    @Test
    fun excludesSubstringNotPrefix() {
        val list = listOf(InviteCandidate("x", "bob@here.com", "Bob"))
        assertEquals(emptyList<InviteCandidate>(), list.filterByInvitePrefix("here"))
        assertEquals(1, list.filterByInvitePrefix("bob").size)
    }
}
