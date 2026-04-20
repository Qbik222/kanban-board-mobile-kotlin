package com.kanban.mobile.di

import com.kanban.mobile.data.DefaultTeamsRepository
import com.kanban.mobile.feature.teams.TeamsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamsRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTeamsRepository(impl: DefaultTeamsRepository): TeamsRepository
}
