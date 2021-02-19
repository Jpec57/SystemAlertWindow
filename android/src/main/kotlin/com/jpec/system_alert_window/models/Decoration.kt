package com.jpec.system_alert_window.models

import android.content.Context
import com.jpec.system_alert_window.utils.Commons
import com.jpec.system_alert_window.utils.NumberUtils

class Decoration(startColor: Object?, endColor: Object?, borderWidth: Object?, borderRadius: Object?, borderColor: Object?, context: Context?) {
    val startColor: Int
    val endColor = 0
    val borderWidth: Int
    val borderRadius: Float
    val borderColor: Int
    var isGradient = false

    init {
        this.startColor = NumberUtils.getInt(startColor)
        if (endColor != null) {
            this.endColor = NumberUtils.getInt(endColor)
            isGradient = true
        } else {
            isGradient = false
        }
        this.borderWidth = Commons.getPixelsFromDp(context, NumberUtils.getInt(borderWidth))
        this.borderRadius = Commons.getPixelsFromDp(context, NumberUtils.getFloat(borderRadius))
        this.borderColor = NumberUtils.getInt(borderColor)
    }
}