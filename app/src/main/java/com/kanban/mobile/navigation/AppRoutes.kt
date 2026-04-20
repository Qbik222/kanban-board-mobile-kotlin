package com.kanban.mobile.navigation

object AppRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val Main = "main"
    const val Teams = "teams"
    const val TeamDetail = "teams/{teamId}"

    const val Boards = "boards"
    const val BoardDetail = "boards/{boardId}"
    const val BoardCardDetail = "boards/{boardId}/cards/{cardId}"
    const val BoardCreate = "boards/create?teamId={teamId}"

    fun teamDetail(teamId: String): String = "teams/$teamId"

    fun boardDetail(boardId: String): String = "boards/$boardId"

    fun boardCardDetail(boardId: String, cardId: String): String = "boards/$boardId/cards/$cardId"

    fun boardCreate(teamId: String = ""): String =
        if (teamId.isEmpty()) {
            "boards/create?teamId="
        } else {
            "boards/create?teamId=$teamId"
        }
}
