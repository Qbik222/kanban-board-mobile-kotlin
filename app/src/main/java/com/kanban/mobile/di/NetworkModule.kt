package com.kanban.mobile.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kanban.mobile.core.network.AuthApi
import com.kanban.mobile.core.network.BearerAuthInterceptor
import com.kanban.mobile.core.network.ClearableCookieJar
import com.kanban.mobile.core.network.CsrfCookieInterceptor
import com.kanban.mobile.core.network.NetworkConfig
import com.kanban.mobile.core.network.PersistentCookieJar
import com.kanban.mobile.core.network.TokenRefreshAuthenticator
import com.kanban.mobile.core.session.DataStoreSessionRepository
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        csrfCookieName = BuildConfig.CSRF_COOKIE_NAME,
        csrfHeaderName = BuildConfig.CSRF_HEADER_NAME,
    )

    @Provides
    @Singleton
    fun provideCookieJar(
        @ApplicationContext context: android.content.Context,
        json: Json,
    ): ClearableCookieJar {
        val prefs = context.getSharedPreferences("http_cookies", android.content.Context.MODE_PRIVATE)
        return PersistentCookieJar(prefs, json)
    }

    @Provides
    @Singleton
    @RefreshHttpClient
    fun provideRefreshOkHttp(
        config: NetworkConfig,
        cookieJar: ClearableCookieJar,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val csrf = CsrfCookieInterceptor(config, cookieJar)
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(csrf)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenRefreshAuthenticator(
        config: NetworkConfig,
        json: Json,
        @RefreshHttpClient refreshClient: OkHttpClient,
        sessionRepository: SessionRepository,
    ): TokenRefreshAuthenticator = TokenRefreshAuthenticator(
        apiBaseUrl = config.apiBaseUrl,
        json = json,
        refreshClient = refreshClient,
        sessionRepository = sessionRepository,
    )

    @Provides
    @Singleton
    fun provideOkHttp(
        config: NetworkConfig,
        cookieJar: ClearableCookieJar,
        sessionRepository: DataStoreSessionRepository,
        authenticator: TokenRefreshAuthenticator,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val csrf = CsrfCookieInterceptor(config, cookieJar)
        val bearer = BearerAuthInterceptor { sessionRepository.accessTokenBlocking() }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .authenticator(authenticator)
            .addInterceptor(csrf)
            .addInterceptor(bearer)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val base = config.apiBaseUrl.trimEnd('/') + "/"
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}
