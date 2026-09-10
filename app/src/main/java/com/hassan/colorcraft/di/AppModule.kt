package com.hassan.colorcraft.di

import com.hassan.colorcraft.data.db.AppDatabase
import com.hassan.colorcraft.data.repository.ColoringRepository
import com.hassan.colorcraft.data.repository.ColoringRepositoryImpl
import com.hassan.colorcraft.data.seed.DatabasePrepopulator
import com.hassan.colorcraft.ui.coloring.ColoringViewModel
import com.hassan.colorcraft.ui.common.ColorPickerViewModel
import com.hassan.colorcraft.ui.home.HomeViewModel
import com.hassan.colorcraft.ui.library.LibraryViewModel
import com.hassan.colorcraft.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().coloringPageDao() }
    single { get<AppDatabase>().coloringProgressDao() }
    single { get<AppDatabase>().sketchDao() }
    single { DatabasePrepopulator() }
    single<ColoringRepository> { ColoringRepositoryImpl(get(), get()) }
    viewModel { SplashViewModel(androidApplication(), get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { LibraryViewModel(get()) }
    viewModel { ColoringViewModel(androidApplication(), get()) }
    viewModel { ColorPickerViewModel(androidApplication()) }
}
