package com.jpec.system_alert_window.models

import android.content.Context
import com.jpec.system_alert_window.utils.Commons
import com.jpec.system_alert_window.utils.NumberUtils

class Padding(left: Object?, top: Object?, right: Object?, bottom: Object?, context: Context?) {
    val left: Int
    val top: Int
    val right: Int
    val bottom: Int

    init {
        this.left = Commons.getPixelsFromDp(context, NumberUtils.getInt(left))
        this.top = Commons.getPixelsFromDp(context, NumberUtils.getInt(top))
        this.right = Commons.getPixelsFromDp(context, NumberUtils.getInt(right))
        this.bottom = Commons.getPixelsFromDp(context, NumberUtils.getInt(bottom))
    }
}