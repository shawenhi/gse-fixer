package com.gse.fixer.di

import com.gse.fixer.core.asset.BundledApks
import com.gse.fixer.core.detector.GoogleServiceDetector
import com.gse.fixer.core.downloader.GmsDownloader
import com.gse.fixer.core.enabler.ShizukuEnabler
import com.gse.fixer.core.installer.ApkInstaller
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.core.verifier.ServiceVerifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val Module = module {
    single { SimpleLogger(androidContext()) }
    single { GoogleServiceDetector(androidContext(), get()) }
    single { BundledApks(androidContext(), get()) }
    single { GmsDownloader(androidContext(), get()) }
    single { ShizukuEnabler(androidContext(), get()) }
    single { ApkInstaller(androidContext(), get(), get(), get(), get()) }
    single { ServiceVerifier(androidContext(), get()) }
}