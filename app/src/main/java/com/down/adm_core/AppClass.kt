package com.down.adm_core

import android.app.Application
import com.adm.core.di.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class AppClass : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)
            modules(modules = module {
                viewModelOf(::MainScreenViewModel)
            })
            modules(coreModule)
        }
    }
}