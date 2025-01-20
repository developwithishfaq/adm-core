package com.adm.core.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InProgressVideoDao {

    @Upsert
    suspend fun addInQue(inProgressVideo: InProgressVideoDB)

    @Query("SELECT * FROM InProgressVideoDB  ORDER BY downloadId DESC")
    fun getAllQueVideos(): Flow<List<InProgressVideoDB>>

  @Query("SELECT * FROM InProgressVideoDB  ORDER BY downloadId DESC")
    suspend fun getAllQueVideosSingle():  List<InProgressVideoDB>

    @Query("SELECT * FROM InProgressVideoDB  ORDER BY downloadId DESC ")
    fun getInProgressQueVideosSingle(): List<InProgressVideoDB>


    @Query("SELECT * FROM InProgressVideoDB WHERE downloadId=:downloadId")
    suspend fun getItemById(downloadId: Long): InProgressVideoDB?

    @Query("DELETE FROM InProgressVideoDB WHERE downloadId=:id")
    suspend fun deleteFromQue(id: String)


}