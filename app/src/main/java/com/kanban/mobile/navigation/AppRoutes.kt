package com.kanban.mobile.navigation

object AppRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val Main = "main"
    const val Teams = "teams"
    const val TeamDetail = "teams/{teamId}"

    fun teamDetail(teamId: String): String = "teams/$teamId"
}
