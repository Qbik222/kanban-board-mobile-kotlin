package com.kanban.mobile.feature.teams

interface TeamsRepository {

    suspend fun listTeams(): Result<List<Team>>

    suspend fun createTeam(name: String): Result<Team>

    suspend fun getTeam(teamId: String): Result<Team>

    suspend fun patchTeam(teamId: String, name: String): Result<Team>

    suspend fun listMembers(teamId: String): Result<List<TeamMember>>

    suspend fun addMember(teamId: String, userId: String, role: TeamMemberRole?): Result<Unit>

    suspend fun patchMemberRole(teamId: String, userId: String, role: TeamMemberRole): Result<Unit>

    suspend fun removeMember(teamId: String, userId: String): Result<Unit>

    suspend fun inviteSearch(teamId: String, query: String, limit: Int): Result<List<InviteCandidate>>
}
