package com.kanban.mobile.core.network

import retrofit2.HttpException

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> when (code()) {
        403 -> "Немає прав"
        404 -> "Не знайдено"
        else -> message() ?: "Помилка мережі"
    }
    else -> message ?: "Помилка"
}
