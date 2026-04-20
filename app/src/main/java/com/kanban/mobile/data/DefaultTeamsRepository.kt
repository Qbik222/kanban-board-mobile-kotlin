package com.kanban.mobile.data

import com.kanban.mobile.core.network.TeamsApi
import com.kanban.mobile.core.network.dto.AddTeamMemberRequestDto
import com.kanban.mobile.core.network.dto.CreateTeamRequestDto
import com.kanban.mobile.core.network.dto.InviteCandidateDto
import com.kanban.mobile.core.network.dto.PatchTeamMemberRequestDto
import com.kanban.mobile.core.network.dto.PatchTeamRequestDto
import com.kanban.mobile.core.network.dto.TeamDto
import com.kanban.mobile.core.network.dto.TeamMemberDto
import com.kanban.mobile.feature.teams.InviteCandidate
import com.kanban.mobile.feature.teams.Team
import com.kanban.mobile.feature.teams.TeamMember
import com.kanban.mobile.feature.teams.TeamMemberRole
import com.kanban.mobile.feature.teams.TeamsRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DefaultTeamsRepository @Inject constructor(
    private val teamsApi: TeamsApi,
) : TeamsRepository {

    override suspend fun listTeams(): Result<List<Team>> = withContext(Dispatchers.IO) {
        runCatching { teamsApi.listTeams().map { it.toDomain() } }
    }

    override suspend fun createTeam(name: String): Result<Team> = withContext(Dispatchers.IO) {
        runCatching {
            teamsApi.createTeam(CreateTeamRequestDto(name = name)).toDomain()
        }
    }

    override suspend fun getTeam(teamId: String): Result<Team> = withContext(Dispatchers.IO) {
        runCatching { teamsApi.getTeam(teamId).toDomain() }
    }

    override suspend fun patchTeam(teamId: String, name: String): Result<Team> =
        withContext(Dispatchers.IO) {
            runCatching {
                teamsApi.patchTeam(teamId, PatchTeamRequestDto(name = name)).toDomain()
            }
        }

    override suspend fun listMembers(teamId: String): Result<List<TeamMember>> =
        withContext(Dispatchers.IO) {
            runCatching { teamsApi.listMembers(teamId).map { it.toDomain() } }
        }

    override suspend fun addMember(
        teamId: String,
        userId: String,
        role: TeamMemberRole?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            teamsApi.addMember(
                teamId,
                AddTeamMemberRequestDto(
                    userId = userId,
                    role = role?.toApiString(),
                ),
            )
        }
    }

    override suspend fun patchMemberRole(
        teamId: String,
        userId: String,
        role: TeamMemberRole,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            teamsApi.patchMember(
                teamId,
                userId,
                PatchTeamMemberRequestDto(role = role.toApiString()),
            )
        }
    }

    override suspend fun removeMember(teamId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { teamsApi.removeMember(teamId, userId) }
        }

    override suspend fun inviteSearch(
        teamId: String,
        query: String,
        limit: Int,
    ): Result<List<InviteCandidate>> = withContext(Dispatchers.IO) {
        runCatching {
            teamsApi.inviteSearch(teamId, query, limit).map { it.toDomain() }
        }
    }

    private fun TeamDto.toDomain(): Team = Team(id = id, name = name)

    private fun TeamMemberDto.toDomain(): TeamMember =
        TeamMember(
            userId = userId,
            email = email,
            name = name,
            role = role.toDomainRole(),
        )

    private fun InviteCandidateDto.toDomain(): InviteCandidate =
        InviteCandidate(userId = userId, email = email, name = name)

    private fun String.toDomainRole(): TeamMemberRole =
        when (uppercase(Locale.US)) {
            "ADMIN", "OWNER" -> TeamMemberRole.ADMIN
            else -> TeamMemberRole.MEMBER
        }
}
