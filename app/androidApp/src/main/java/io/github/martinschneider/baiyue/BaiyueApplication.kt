package io.github.martinschneider.baiyue

import android.app.Application
import io.github.martinschneider.baiyue.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaiyueApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BaiyueApplication)
            modules(appModule(this@BaiyueApplication))
        }
    }
}
