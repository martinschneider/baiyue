package io.github.martinschneider.baiyue.di

import io.github.martinschneider.baiyue.data.MapPreferences
import io.github.martinschneider.baiyue.data.backup.BackupManager
import io.github.martinschneider.baiyue.data.db.BaiyueDatabase
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import io.github.martinschneider.baiyue.data.source.HikeDescriptionDataSource
import io.github.martinschneider.baiyue.data.source.MountainDataSource
import io.github.martinschneider.baiyue.platform.createMapPreferences
import io.github.martinschneider.baiyue.platform.createSqlDriver
import io.github.martinschneider.baiyue.platform.readBundledAsset
import io.github.martinschneider.baiyue.ui.screen.detail.DetailViewModel
import io.github.martinschneider.baiyue.ui.screen.list.ListViewModel
import io.github.martinschneider.baiyue.ui.screen.map.MapViewModel
import io.github.martinschneider.baiyue.ui.screen.settings.SettingsViewModel
import io.github.martinschneider.baiyue.ui.screen.stats.StatsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun appModule(context: Any?) = module {
    single {
        val driver = createSqlDriver(context)
        BaiyueDatabase(driver)
    }

    single {
        val source = MountainDataSource()
        val json = readBundledAsset(context, "mountains.json")
        source.loadMountains(json)
        source
    }

    single {
        val source = HikeDescriptionDataSource()
        val json = readBundledAsset(context, "hike_descriptions.json")
        source.load(json)
        source
    }

    single { MountainRepository(get(), get()) }
    single { BackupManager(get()) }
    single<MapPreferences> { createMapPreferences(context) }

    single { ListViewModel(get()) }
    single { MapViewModel(get(), get(), get()) }
    factory { DetailViewModel(get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { StatsViewModel(get()) }
}
