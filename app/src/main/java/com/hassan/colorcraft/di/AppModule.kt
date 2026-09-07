package com.hassan.colorcraft.di

import com.hassan.colorcraft.data.db.AppDatabase
import com.hassan.colorcraft.data.seed.DatabasePrepopulator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().coloringPageDao() }
    single { get<AppDatabase>().coloringProgressDao() }
    single { get<AppDatabase>().sketchDao() }
    single { DatabasePrepopulator() }
}
