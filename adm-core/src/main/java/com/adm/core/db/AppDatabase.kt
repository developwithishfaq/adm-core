package com.adm.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InProgressVideoDB::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getInProgressVideosDao(): InProgressVideoDao
}