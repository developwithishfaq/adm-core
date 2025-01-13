package com.down.m3u8_parser.parsers.single_stream

import android.util.Log
import com.down.m3u8_parser.api.ApiHitter
import com.down.m3u8_parser.api.ApiHitterImpl
import com.down.m3u8_parser.listeners.M3u8SingleStreamParser
import com.down.m3u8_parser.model.SingleStream

class M3u8SingleStreamTestParserImpl(
    private val apiHitter: ApiHitter = ApiHitterImpl(),
) : M3u8SingleStreamParser {

    private val TAG = "M3u8SingleStreamParserImpl"

    private val m3u8Starting = "#EXTM3U"
    private val infoLineStarting = "#EXTINF:"
    private val byteRangeLineStarting = "#EXT-X-BYTERANGE"
    private var filterStarted = false
    private var pickNextLine = false
    private var isThatByteRangerM3u8 = false

    override suspend fun getM3u8Chunks(
        url: String,
        headers: Map<String, String>
    ): List<SingleStream> {
        val response = apiHitter.get(link = url, headers)
        log("Response=${response}")
        val chunks = if (response == null) {
            emptyList()
        } else {
            if (response.contains("#EXTINF:")) {
                val chunks = getM3u8Chunks(response)
                log("Total Chunks : ${chunks.size}")
                chunks
            } else {
                log("Bad Response For Single Streams")
                emptyList()
            }
        }
        log("Chunks:")
        chunks.forEachIndexed { index, chunk ->
            log("$index-${chunk}")
        }
        return chunks
    }

    override suspend fun getM3u8Chunks(text: String): List<SingleStream> {
        val streams = mutableListOf<SingleStream>()
        val lines = text.lines()
        lines.forEach { line ->
            if (line.startsWith(m3u8Starting)) {
                filterStarted = true
            } else if (filterStarted && line.startsWith(infoLineStarting)) {
                pickNextLine = true
            } else if (pickNextLine && line.startsWith(byteRangeLineStarting)) {
                log("Line Picked = $line")
                val bytes = line.substringAfter(":").split("@")
                if (bytes.size == 2) {
                    isThatByteRangerM3u8 = true
                    streams.add(
                        SingleStream(
                            link = "",
                            headers = mapOf(
                                "Range" to "bytes=${bytes[0]}-${bytes[1]}"
                            )
                        )
                    )
                }
            } else if (pickNextLine) {
                if (isThatByteRangerM3u8) {
                    streams[streams.lastIndex] = streams.last().copy(
                        link = line
                    )
                } else {
                    streams.add(
                        SingleStream(
                            link = line,
                            headers = emptyMap()
                        )
                    )
                }
                pickNextLine = false
            }
        }
        return streams
    }

    fun log(msg: String) {
        Log.d(TAG, "M3u8SingleStreamParserImpl:$msg")
    }
}