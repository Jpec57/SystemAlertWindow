package com.jpec.system_alert_window.utils

import android.util.Log

object NumberUtils {
    private const val TAG = "NumberUtils"
    fun getFloat(`object`: Object?): Float {
        return getNumber(`object`).floatValue()
    }

    fun getInt(`object`: Object?): Int {
        return getNumber(`object`).intValue()
    }

    private fun getNumber(`object`: Object?): Number {
        var `val`: Number = 0
        if (`object` != null) {
            try {
                `val` = `object` as Number
            } catch (ex: Exception) {
                Log.d(TAG, ex.toString())
            }
        }
        return `val`
    }
}