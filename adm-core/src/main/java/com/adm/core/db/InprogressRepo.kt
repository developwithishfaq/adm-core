package com.adm.core.db

import kotlinx.coroutines.flow.Flow

interface InProgressRepository {

    suspend fun addInQue(inProgressVideo: InProgressVideoDB)
    fun getAllQueVideos(): Flow<List<InProgressVideoDB>>
   suspend fun getAllQueVideosSingle():  List<InProgressVideoDB>
    suspend fun getInProgressQueVideosSingle():  List<InProgressVideoDB>
      suspend fun getItemById(id: Long): InProgressVideoDB?
    suspend fun deleteFromQue(id: Long)

}