package com.jpec.system_alert_window.utils

import android.content.Context

object UiBuilder {
    fun getTextView(context: Context?, textMap: Map<String, Object?>?): TextView? {
        if (textMap == null) return null
        val textView = TextView(context)
        textView.setText(textMap[KEY_TEXT] as String?)
        textView.setTypeface(textView.getTypeface(), Commons.getFontWeight(textMap[KEY_FONT_WEIGHT] as String?, Typeface.NORMAL))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, NumberUtils.getFloat(textMap[KEY_FONT_SIZE]))
        textView.setTextColor(NumberUtils.getInt(textMap[KEY_TEXT_COLOR]))
        val padding: Padding = getPadding(context, textMap[KEY_PADDING])
        textView.setPadding(padding.getLeft(), padding.getTop(), padding.getRight(), padding.getBottom())
        return textView
    }

    fun getPadding(context: Context?, `object`: Object?): Padding {
        @SuppressWarnings("unchecked") val paddingMap = `object` as Map<String, Object>?
                ?: return Padding(0, 0, 0, 0, context)
        return Padding(paddingMap[KEY_LEFT], paddingMap[KEY_TOP], paddingMap[KEY_RIGHT], paddingMap[KEY_BOTTOM], context)
    }

    fun getMargin(context: Context?, `object`: Object?): Margin {
        @SuppressWarnings("unchecked") val marginMap = `object` as Map<String, Object>?
                ?: return Margin(0, 0, 0, 0, context)
        return Margin(marginMap[KEY_LEFT], marginMap[KEY_TOP], marginMap[KEY_RIGHT], marginMap[KEY_BOTTOM], context)
    }

    fun getDecoration(context: Context?, `object`: Object?): Decoration? {
        @SuppressWarnings("unchecked") val decorationMap = `object` as Map<String, Object>?
                ?: return null
        return Decoration(decorationMap[KEY_START_COLOR], decorationMap[KEY_END_COLOR],
                decorationMap[KEY_BORDER_WIDTH], decorationMap[KEY_BORDER_RADIUS],
                decorationMap[KEY_BORDER_COLOR], context)
    }

    fun getButtonView(context: Context, buttonMap: Map<String?, Object?>?): Button? {
        if (buttonMap == null) return null
        val button = Button(context)
        val buttonText: TextView? = getTextView(context, Commons.getMapFromObject(buttonMap, KEY_TEXT))
        assert(buttonText != null)
        button.setText(buttonText.getText())
        val tag = buttonMap[KEY_TAG]
        button.setTag(tag)
        button.setTextSize(Commons.getSpFromPixels(context, buttonText.getTextSize()))
        button.setTextColor(buttonText.getTextColors())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) button.setElevation(10)
        val params: LinearLayout.LayoutParams = LayoutParams(
                Commons.getPixelsFromDp(context, buttonMap[KEY_WIDTH] as Int),
                Commons.getPixelsFromDp(context, buttonMap[KEY_HEIGHT] as Int),
                1.0f)
        val buttonMargin: Margin = getMargin(context, buttonMap[KEY_MARGIN])
        params.setMargins(buttonMargin.getLeft(), buttonMargin.getTop(), buttonMargin.getRight(), Math.min(buttonMargin.getBottom(), 4))
        button.setLayoutParams(params)
        val padding: Padding = getPadding(context, buttonMap[KEY_PADDING])
        button.setPadding(padding.getLeft(), padding.getTop(), padding.getRight(), padding.getBottom())
        val decoration: Decoration? = getDecoration(context, buttonMap[KEY_DECORATION])
        if (decoration != null) {
            val gd: GradientDrawable = getGradientDrawable(decoration)
            button.setBackground(gd)
        }
        button.setOnClickListener { v ->
            if (!SystemAlertWindowPlugin.sIsIsolateRunning.get()) {
                SystemAlertWindowPlugin.startCallBackHandler(context)
            }
            SystemAlertWindowPlugin.invokeCallBack(context, CALLBACK_TYPE_ONCLICK, tag)
        }
        return button
    }

    fun getGradientDrawable(decoration: Decoration?): GradientDrawable {
        val gd = GradientDrawable()
        if (decoration.isGradient()) {
            val colors = intArrayOf(decoration.getStartColor(), decoration.getEndColor())
            gd.setColors(colors)
            gd.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT)
        } else {
            gd.setColor(decoration.getStartColor())
        }
        gd.setCornerRadius(decoration.getBorderRadius())
        gd.setStroke(decoration.getBorderWidth(), decoration.getBorderColor())
        return gd
    }
}