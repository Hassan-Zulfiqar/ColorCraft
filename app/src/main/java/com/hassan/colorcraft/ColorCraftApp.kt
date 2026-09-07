package com.hassan.colorcraft

import android.app.Application
import com.hassan.colorcraft.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ColorCraftApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ColorCraftApp)
            modules(appModule)
        }
    }
}
