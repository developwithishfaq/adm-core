package com.adm.core.di

import androidx.room.Room
import com.adm.core.DownloadNotificationManager
import com.adm.core.InternetController
import com.adm.core.MyDownloaderManager
import com.adm.core.ProgressManager
import com.adm.core.db.AppDatabase
import com.adm.core.db.InProgressRepository
import com.adm.core.db.InProgressRepositoryImpl
import com.adm.core.db.InProgressVideoDao
import com.adm.core.m3u8.AnalyticHelper
import com.adm.core.m3u8.MaxParallelDownloads
import com.adm.core.m3u8.MaxParallelDownloadsImpl
import com.adm.core.m3u8.MyAnalyticHelper
import com.adm.core.m3u8.SimpleVideosMergerImpl
import com.adm.core.m3u8.TempDirProvider
import com.adm.core.m3u8.TempDirProviderImpl
import com.adm.core.m3u8.VideosMerger
import com.adm.core.services.downloader.DownloaderTypeProvider
import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import com.adm.core.services.logger.Logger
import com.adm.core.services.logger.LoggerImpl
import com.down.m3u8_parser.listeners.M3u8ChunksPicker
import com.down.m3u8_parser.parsers.M3u8ChunksPickerImpl
import org.koin.dsl.module

val coreModule = module {
    single<AppDatabase> {
        Room.databaseBuilder(get(), AppDatabase::class.java, "AppDb")
            .fallbackToDestructiveMigration().build()
    }
    single<InProgressRepository> {
        InProgressRepositoryImpl(get())
    }
    single<MyDownloaderManager> {
        MyDownloaderManager(get(), get(),get())
    }
    single<ProgressManager> {
        ProgressManager(get())
    }
    single<InProgressVideoDao> {
        get<AppDatabase>().getInProgressVideosDao()
    }
    single<TempDirProvider> {
        TempDirProviderImpl(get())
    }
    single<DownloadNotificationManager> {
        DownloadNotificationManager(get())
    }

    single<Logger> {
        LoggerImpl()
    }

    single<TempDirProvider> {
        TempDirProviderImpl(get())
    }

    single<M3u8ChunksPicker> {
        M3u8ChunksPickerImpl()
    }

    single<VideosMerger> {
        SimpleVideosMergerImpl(get())
    }

    single<MaxParallelDownloads> {
        MaxParallelDownloadsImpl()
    }
    single<DownloaderTypeProvider> {
        DownloaderTypeProviderImpl(get(),get(),get(),get(),get(),get(),get())
    }

    single<Logger> {
        LoggerImpl()
    }
    single<AnalyticHelper> {
        MyAnalyticHelper()
    }

    single<Logger> {
        LoggerImpl()
    }

    factory<InternetController> {
        InternetController(get())
    }

}