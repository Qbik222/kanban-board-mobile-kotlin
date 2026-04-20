package com.kanban.mobile.feature.teams

enum class TeamMemberRole {
    ADMIN,
    MEMBER;

    /** Backend uses `admin` | `user` for team member roles. */
    fun toApiString(): String =
        when (this) {
            ADMIN -> "admin"
            MEMBER -> "user"
        }
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
