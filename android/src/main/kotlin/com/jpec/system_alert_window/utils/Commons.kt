package com.jpec.system_alert_window.utils

import android.annotation.TargetApi

object Commons {
    @SuppressWarnings("unchecked")
    fun getMapFromObject(@NonNull map: Map<String?, Object?>, key: String?): Map<String, Object>? {
        return map[key] as Map<String, Object>?
    }

    @SuppressWarnings("unchecked")
    fun getMapListFromObject(@NonNull map: Map<String?, Object?>, key: String?): List<Map<String, Object>>? {
        return map[key] as List<Map<String, Object>>?
    }

    fun getSpFromPixels(@NonNull context: Context, px: Float): Float {
        val scaledDensity: Float = context.getResources().getDisplayMetrics().scaledDensity
        return px / scaledDensity
    }

    fun getPixelsFromDp(@NonNull context: Context, dp: Int): Int {
        return if (dp == -1) -1 else TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics())
    }

    fun getPixelsFromDp(@NonNull context: Context, dp: Float): Float {
        return if (dp == -1f) (-1).toFloat() else TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics())
    }

    fun getGravity(@Nullable gravityStr: String?, defVal: Int): Int {
        var gravity = defVal
        if (gravityStr != null) {
            when (gravityStr) {
                "top" -> gravity = Gravity.TOP
                "center" -> gravity = Gravity.CENTER
                "bottom" -> gravity = Gravity.BOTTOM
                "leading" -> gravity = Gravity.START
                "trailing" -> gravity = Gravity.END
            }
        }
        return gravity
    }

    fun getFontWeight(@Nullable fontWeightStr: String?, defVal: Int): Int {
        var fontWeight = defVal
        if (fontWeightStr != null) {
            fontWeight = when (fontWeightStr) {
                "normal" -> Typeface.NORMAL
                "bold" -> Typeface.BOLD
                "italic" -> Typeface.ITALIC
                "bold_italic" -> Typeface.BOLD_ITALIC
                else -> Typeface.NORMAL
            }
        }
        return fontWeight
    }

    fun setMargin(context: Context?, params: LinearLayout.LayoutParams, map: Map<String?, Object?>) {
        val margin: Margin = UiBuilder.getMargin(context, map[KEY_MARGIN])
        params.setMargins(margin.getLeft(), margin.getTop(), margin.getRight(), margin.getBottom())
    }

    fun isForceAndroidBubble(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activityManager: ActivityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (activityManager != null) {
                val pm: PackageManager = context.getPackageManager()
                return !pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) || pm.hasSystemFeature(PackageManager.FEATURE_RAM_LOW) || activityManager.isLowRamDevice()
            } else {
                Log.i("Commons", "Marking force android bubble as false")
            }
        }
        return false
    }
}