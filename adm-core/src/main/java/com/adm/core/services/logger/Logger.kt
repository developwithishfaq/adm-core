package com.adm.core.services.logger

interface Logger {
    fun logMessage(tag: String, msg: String, isError: Boolean)
}