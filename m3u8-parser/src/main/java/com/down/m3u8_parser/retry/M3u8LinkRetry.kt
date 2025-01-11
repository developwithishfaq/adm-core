package com.down.m3u8_parser.retry

interface M3u8LinkRetry {
    fun getRetryCount(): Int
    fun delayAfterFailures(): Long
}