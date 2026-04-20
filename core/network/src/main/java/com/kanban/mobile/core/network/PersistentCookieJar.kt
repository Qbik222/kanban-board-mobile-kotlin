package com.kanban.mobile.core.network

import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.HttpUrl

private const val PREFS_KEY = "okhttp_cookies_v1"

/**
 * Persists cookies in [SharedPreferences] so refresh httpOnly cookies survive process death.
 */
class PersistentCookieJar(
    private val prefs: SharedPreferences,
    private val json: Json,
) : ClearableCookieJar {

    private val lock = Any()
    private var cache: MutableList<StoredCookie>? = null

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            val all = loadAll().toMutableList()
            for (c in cookies) {
                all.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
                all.add(StoredCookie.from(c))
            }
            persist(all)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val loaded = loadAll().toMutableList()
            val valid = loaded.filter { cookie ->
                val exp = cookie.expiresAtMillis
                exp == null || exp > now
            }
            if (valid.size != loaded.size) {
                persist(valid.toMutableList())
            }
            return valid.mapNotNull { it.toCookieIfMatches(url) }
        }
    }

    override fun clear() {
        synchronized(lock) {
            cache = null
            prefs.edit().remove(PREFS_KEY).apply()
        }
    }

    private fun loadAll(): List<StoredCookie> {
        cache?.let { return it }
        val raw = prefs.getString(PREFS_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<StoredCookie>>(raw).toMutableList().also { cache = it }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: MutableList<StoredCookie>) {
        cache = list
        prefs.edit().putString(PREFS_KEY, json.encodeToString(list.toList())).apply()
    }

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresAtMillis: Long? = null,
        val secure: Boolean = false,
        val httpOnly: Boolean = false,
        val hostOnly: Boolean = false,
        val persistent: Boolean = false,
    ) {
        fun toCookieIfMatches(url: HttpUrl): Cookie? {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .path(path)
            if (hostOnly) {
                builder.hostOnlyDomain(domain)
            } else {
                builder.domain(domain)
            }
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            if (persistent && expiresAtMillis != null) {
                builder.expiresAt(expiresAtMillis)
            }
            val cookie = builder.build()
            return if (cookie.matches(url)) cookie else null
        }

        companion object {
            fun from(c: Cookie) = StoredCookie(
                name = c.name,
                value = c.value,
                domain = c.domain,
                path = c.path,
                expiresAtMillis = if (c.persistent) c.expiresAt else null,
                secure = c.secure,
                httpOnly = c.httpOnly,
                hostOnly = c.hostOnly,
                persistent = c.persistent,
            )
        }
    }
}
