package com.kanban.mobile.feature.teams

enum class TeamMemberRole {
    ADMIN,
    MEMBER;

    fun toApiString(): String = name
}

data class Team(
    val id: String,
    val name: String,
)

data class TeamMember(
    val userId: String,
    val email: String?,
    val name: String?,
    val role: TeamMemberRole,
)

data class InviteCandidate(
    val userId: String,
    val email: String?,
    val name: String?,
)
