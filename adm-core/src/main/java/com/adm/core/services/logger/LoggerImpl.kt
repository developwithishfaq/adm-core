package com.adm.core.services.logger

import android.util.Log

class LoggerImpl : Logger {
    override fun logMessage(tag: String, msg: String, isError: Boolean) {
        if (isError) {
            Log.e(tag, msg)
        } else {
            Log.d(tag, msg)
        }
    }
}