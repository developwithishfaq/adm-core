package com.down.m3u8_parser.listeners

import com.down.m3u8_parser.model.SingleStream

interface M3u8ChunksPicker {
    suspend fun getChunks(m3u8Link: String,headers: Map<String,String>): List<SingleStream>
}