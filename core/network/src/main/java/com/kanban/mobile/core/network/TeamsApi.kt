package com.kanban.mobile.core.network

import com.kanban.mobile.core.network.dto.AddTeamMemberRequestDto
import com.kanban.mobile.core.network.dto.CreateTeamRequestDto
import com.kanban.mobile.core.network.dto.InviteCandidateDto
import com.kanban.mobile.core.network.dto.PatchTeamMemberRequestDto
import com.kanban.mobile.core.network.dto.PatchTeamRequestDto
import com.kanban.mobile.core.network.dto.TeamDto
import com.kanban.mobile.core.network.dto.TeamMemberDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TeamsApi {

    @GET("teams")
    suspend fun listTeams(): List<TeamDto>

    @POST("teams")
    suspend fun createTeam(@Body body: CreateTeamRequestDto): TeamDto

    @GET("teams/{teamId}")
    suspend fun getTeam(@Path("teamId") teamId: String): TeamDto

    @PATCH("teams/{teamId}")
    suspend fun patchTeam(
        @Path("teamId") teamId: String,
        @Body body: PatchTeamRequestDto,
    ): TeamDto

    @GET("teams/{teamId}/members")
    suspend fun listMembers(@Path("teamId") teamId: String): List<TeamMemberDto>

    @POST("teams/{teamId}/members")
    suspend fun addMember(
        @Path("teamId") teamId: String,
        @Body body: AddTeamMemberRequestDto,
    )

    @PATCH("teams/{teamId}/members/{userId}/role")
    suspend fun patchMember(
        @Path("teamId") teamId: String,
        @Path("userId") userId: String,
        @Body body: PatchTeamMemberRequestDto,
    )

    @DELETE("teams/{teamId}/members/{userId}")
    suspend fun removeMember(
        @Path("teamId") teamId: String,
        @Path("userId") userId: String,
    )

    @GET("teams/{teamId}/invite-search")
    suspend fun inviteSearch(
        @Path("teamId") teamId: String,
        @Query("query") query: String,
        @Query("limit") limit: Int,
    ): List<InviteCandidateDto>
}
