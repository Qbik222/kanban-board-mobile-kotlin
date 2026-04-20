package com.kanban.mobile.di

import com.kanban.mobile.core.realtime.BoardRealtimeClient
import com.kanban.mobile.core.realtime.SocketBoardRealtimeClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealtimeModule {

    @Provides
    @Singleton
    fun provideBoardRealtimeClient(impl: SocketBoardRealtimeClient): BoardRealtimeClient = impl
}
