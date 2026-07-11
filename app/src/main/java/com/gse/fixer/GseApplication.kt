package com.gse.fixer

import android.app.Application
import com.gse.fixer.di.Module
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        startKoin {
            androidContext(this@GseApplication)
            modules(Module)
        }
    }
}