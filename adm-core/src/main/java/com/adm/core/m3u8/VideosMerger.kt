package com.adm.core.m3u8

interface VideosMerger {
    suspend fun mergeVideos(folderPath: String, destPath: String): Result<Boolean>
}

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>() {
        // Provide direct access to the data (with null safety)
        fun getOrNull(): T? = data
    }

    data class Error<out T>(val exception: Exception? = null, val message: String? = null) :
        Resource<T>()
}