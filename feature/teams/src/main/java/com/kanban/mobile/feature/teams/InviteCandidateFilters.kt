package com.kanban.mobile.feature.teams

/**
 * Keeps candidates whose email, name, or user id [startsWith] the trimmed query (case-insensitive).
 */
fun List<InviteCandidate>.filterByInvitePrefix(query: String): List<InviteCandidate> {
    val prefix = query.trim().lowercase()
    if (prefix.isEmpty()) return emptyList()
    return filter { c ->
        sequenceOf(c.email, c.name, c.userId).any { field ->
            field != null && field.trim().lowercase().startsWith(prefix)
        }
    }
}
