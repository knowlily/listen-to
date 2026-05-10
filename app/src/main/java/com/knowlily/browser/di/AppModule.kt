package com.knowlily.browser.di

import android.content.Context
import com.knowlily.browser.plugin.AdBlockerPlugin
import com.knowlily.browser.plugin.DarkModePlugin
import com.knowlily.browser.plugin.PluginManager
import com.knowlily.browser.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePluginManager(@ApplicationContext context: Context): PluginManager {
        val pm = PluginManager.getInstance(context)
        pm.registerPlugin(AdBlockerPlugin())
        pm.registerPlugin(DarkModePlugin())
        pm.loadUserPlugins()
        return pm
    }
}
