package com.jpec.system_alert_window

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.HashMap
import com.jpec.system_alert_window.utils.Commons
import com.jpec.system_alert_window.views.BodyView
import com.jpec.system_alert_window.views.FooterView
import com.jpec.system_alert_window.views.HeaderView
import com.jpec.system_alert_window.utils.Constants.INTENT_EXTRA_PARAMS_MAP
import com.jpec.system_alert_window.utils.Constants.KEY_BODY
import com.jpec.system_alert_window.utils.Constants.KEY_FOOTER
import com.jpec.system_alert_window.utils.Constants.KEY_HEADER

class BubbleActivity : AppCompatActivity() {
    private var bubbleLayout: LinearLayout? = null
    private var paramsMap: HashMap<String, Object>? = null
    private var mContext: Context? = null
    @SuppressWarnings("unchecked")
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble)
        mContext = this
        bubbleLayout = findViewById(R.id.bubbleLayout)
        val intent: Intent = getIntent()
        if (intent != null && intent.extras != null) {
            paramsMap = intent.getSerializableExtra(INTENT_EXTRA_PARAMS_MAP) as HashMap<String, Object>?
            configureUI()
        }
    }

    fun configureUI() {
        val headersMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_HEADER)
        val bodyMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_BODY)
        val footerMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_FOOTER)
        val headerView: LinearLayout = HeaderView(mContext, headersMap).getView()
        val bodyView: LinearLayout = BodyView(mContext, bodyMap).getView()
        val footerView: LinearLayout = FooterView(mContext, footerMap).getView()
        bubbleLayout!!.setBackgroundColor(Color.WHITE)
        val params: FrameLayout.LayoutParams = LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        bubbleLayout!!.layoutParams = params
        bubbleLayout!!.addView(headerView)
        bubbleLayout!!.addView(bodyView)
        bubbleLayout!!.addView(footerView)
    }
}