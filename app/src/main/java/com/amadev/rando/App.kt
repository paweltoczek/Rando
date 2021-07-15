package com.amadev.rando

import android.app.Application
import com.amadev.rando.data.networkModule
import com.amadev.rando.data.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App: Application() {


    override fun onCreate() {
        super.onCreate()

        val modules = listOf(networkModule, viewModelModule)

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@App)
            modules(networkModule)
            modules(viewModelModule)
        }
    }
}