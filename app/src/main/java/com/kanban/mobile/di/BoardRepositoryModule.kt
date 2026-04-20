package com.kanban.mobile.di

import com.kanban.mobile.data.DefaultBoardRepository
import com.kanban.mobile.feature.boards.BoardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BoardRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBoardRepository(impl: DefaultBoardRepository): BoardRepository
}
