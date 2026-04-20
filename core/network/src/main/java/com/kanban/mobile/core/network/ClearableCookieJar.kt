package com.kanban.mobile.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

interface ClearableCookieJar : CookieJar {
    fun clear()
}
