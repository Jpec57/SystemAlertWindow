package com.jpec.system_alert_window.services

import android.annotation.SuppressLint

class WindowService : JobIntentService(), OnTouchListener {
    private var oServiceHandler: Handler? = null
    private var windowGravity: String? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var windowMargin: Margin? = null
    private var headerView: LinearLayout? = null
    private var bodyView: LinearLayout? = null
    private var footerView: LinearLayout? = null
    private var mContext: Context? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0
    private var moving = false
    @Override
    fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating Window Service")
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        oServiceHandler = Handler()
    }

    @Override
    fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //startTheServiceProcess(intent);
        Log.d(TAG, "onStartCommand")
        oServiceHandler = Handler()
        return super.onStartCommand(intent, flags, startId)
    }

    @Override
    fun onStopCurrentWork(): Boolean {
        closeOverlayService()
        return super.onStopCurrentWork()
    }

    @Override
    protected fun onHandleWork(@NonNull intent: Intent?) {
        Log.d(TAG, "Starting the service process")
        startTheServiceProcess(intent)
    }

    private fun startTheServiceProcess(intent: Intent?) {
        mContext = this
        if (null != intent && intent.getExtras() != null) {
            Log.i(TAG, "Intent extras are not null")
            val isCloseWindow: Boolean = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
            if (!isCloseWindow) {
                var isUpdateWindow: Boolean = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                if (windowView == null || !ViewCompat.isAttachedToWindow(windowView)) {
                    isUpdateWindow = false
                }
                if (!isUpdateWindow) {
                    closeOverlayService()
                    wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                } else {
                    try {
                        wm.removeView(windowView)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Caught exception $ex")
                    }
                }
                @SuppressWarnings("unchecked") val paramsMap: HashMap<String, Object> = intent.getSerializableExtra(INTENT_EXTRA_PARAMS_MAP) as HashMap<String, Object>
                assert(paramsMap != null)
                val headersMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_HEADER)
                val bodyMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_BODY)
                val footerMap: Map<String, Object> = Commons.getMapFromObject(paramsMap, KEY_FOOTER)
                windowMargin = UiBuilder.getMargin(mContext, paramsMap.get(KEY_MARGIN))
                windowGravity = paramsMap.get(KEY_GRAVITY)
                windowWidth = NumberUtils.getInt(paramsMap.get(KEY_WIDTH))
                windowHeight = NumberUtils.getInt(paramsMap.get(KEY_HEIGHT))
                headerView = HeaderView(mContext, headersMap).getView()
                if (bodyMap != null) bodyView = BodyView(mContext, bodyMap).getView()
                if (footerMap != null) footerView = FooterView(mContext, footerMap).getView()
                if (wm != null) {
                    showWindow(isUpdateWindow)
                } else {
                    Log.e(TAG, "Unable to show the overlay window as the window manager is null")
                }
            } else {
                closeOverlayService()
                try {
                    Log.d(TAG, "Calling stopSelf")
                    stopSelf()
                    Log.d(TAG, "Stopped self")
                } catch (ex: Exception) {
                    Log.d(TAG, "Exception in stopping self")
                }
            }
        } else {
            Log.e(TAG, "Intent extras are null!")
        }
    }

    private fun showWindow(isUpdateWindow: Boolean) {
        if (isUpdateWindow) {
            Log.d(TAG, "Updating the window")
        } else {
            Log.d(TAG, "Creating the window")
        }
        val params: WindowManager.LayoutParams
        params = LayoutParams()
        params.width = if (windowWidth == 0) LayoutParams.MATCH_PARENT else Commons.getPixelsFromDp(mContext, windowWidth)
        params.height = if (windowHeight == 0) LayoutParams.WRAP_CONTENT else Commons.getPixelsFromDp(mContext, windowHeight)
        params.format = PixelFormat.TRANSLUCENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
            params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_SHOW_WHEN_LOCKED or LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT or LayoutParams.TYPE_SYSTEM_OVERLAY
            params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE
        }
        params.gravity = Commons.getGravity(windowGravity, Gravity.TOP)
        val marginTop: Int = windowMargin.getTop()
        val marginBottom: Int = windowMargin.getBottom()
        val marginLeft: Int = windowMargin.getLeft()
        val marginRight: Int = windowMargin.getRight()
        params.x = Math.max(marginLeft, marginRight)
        params.y = if (params.gravity === Gravity.TOP) marginTop else if (params.gravity === Gravity.BOTTOM) marginBottom else Math.max(marginTop, marginBottom)
        /* params.horizontalMargin = Math.max(marginLeft, marginRight);
        params.verticalMargin = (params.gravity == Gravity.TOP) ? marginTop :
                (params.gravity == Gravity.BOTTOM) ? marginBottom : Math.max(marginTop, marginBottom);*/
        //windowView.setOnTouchListener(this);
        oServiceHandler.post {
            val contentParams: LinearLayout.LayoutParams = LayoutParams(params.width, params.height)
            buildWindowView(contentParams, params.width === LayoutParams.MATCH_PARENT)
        }
        oServiceHandler.post {
            //WindowService.this.buildWindowView();
            wm.addView(windowView, params)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildWindowView(params: LinearLayout.LayoutParams, enableDraggable: Boolean) {
        windowView = LinearLayout(mContext)
        windowView.setOrientation(LinearLayout.VERTICAL)
        windowView.setBackgroundColor(Color.WHITE)
        windowView.setLayoutParams(params)
        windowView.removeAllViews()
        windowView.addView(headerView)
        if (bodyView != null) windowView.addView(bodyView)
        if (footerView != null) windowView.addView(footerView)
        if (enableDraggable) windowView.setOnTouchListener(this)
    }

    private fun closeOverlayService() {
        Log.d(TAG, "Ending the service process")
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm.removeView(windowView)
                }
            }
            wm = null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "view not found")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    fun onTouch(view: View?, event: MotionEvent): Boolean {
        if (null != wm) {
            if (event.getAction() === MotionEvent.ACTION_DOWN) {
                val x: Float = event.getRawX()
                val y: Float = event.getRawY()
                moving = false
                val location = IntArray(2)
                windowView.getLocationOnScreen(location)
                originalXPos = location[0]
                originalYPos = location[1]
                offsetX = originalXPos - x
                offsetY = originalYPos - y
            } else if (event.getAction() === MotionEvent.ACTION_MOVE) {
                val x: Float = event.getRawX()
                val y: Float = event.getRawY()
                val params: WindowManager.LayoutParams = windowView.getLayoutParams() as LayoutParams
                val newX = (offsetX + x).toInt()
                val newY = (offsetY + y).toInt()
                if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                    return false
                }
                params.x = newX
                params.y = newY
                wm.updateViewLayout(windowView, params)
                moving = true
            } else if (event.getAction() === MotionEvent.ACTION_UP) {
                return moving
            }
        }
        return false
    }

    companion object {
        private const val TAG = "WindowService"
        const val JOB_ID = 1
        private const val INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow"
        private const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
        private var wm: WindowManager? = null

        @SuppressLint("StaticFieldLeak")
        private var windowView: LinearLayout? = null
        fun enqueueWork(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Start work")
            intent.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
            enqueueWork(context, WindowService::class.kt, JOB_ID, intent)
        }

        fun updateWindow(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Update window")
            intent.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, true)
            enqueueWork(context, WindowService::class.kt, JOB_ID, intent)
        }

        fun dequeueWork(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Stop work")
            intent.putExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, true)
            enqueueWork(context, WindowService::class.kt, JOB_ID, intent)
        }
    }
}