package com.kanban.mobile.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamDto(
    val id: String,
    val name: String,
)

@Serializable
data class TeamMemberDto(
    @SerialName("id") val userId: String,
    val role: String,
    val email: String? = null,
    val name: String? = null,
)

@Serializable
data class CreateTeamRequestDto(
    val name: String,
)

@Serializable
data class PatchTeamRequestDto(
    val name: String,
)

@Serializable
data class AddTeamMemberRequestDto(
    val userId: String,
    val role: String? = null,
)

@Serializable
data class PatchTeamMemberRequestDto(
    val role: String,
)

@Serializable
data class InviteCandidateDto(
    @SerialName("id") val userId: String,
    val email: String? = null,
    val name: String? = null,
)
