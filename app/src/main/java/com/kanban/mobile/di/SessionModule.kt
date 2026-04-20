package com.kanban.mobile.di

import android.content.Context
import com.kanban.mobile.core.session.DataStoreSessionRepository
import com.kanban.mobile.core.session.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideDataStoreSessionRepository(
        @ApplicationContext context: Context,
    ): DataStoreSessionRepository = DataStoreSessionRepository(context)

    @Provides
    @Singleton
    fun provideSessionRepository(
        impl: DataStoreSessionRepository,
    ): SessionRepository = impl
}
