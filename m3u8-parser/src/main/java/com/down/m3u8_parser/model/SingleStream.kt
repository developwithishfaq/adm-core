package com.down.m3u8_parser.model


data class SingleStream(
    val link: String,
    val headers: Map<String, String>,
    val size: Long? = null
)