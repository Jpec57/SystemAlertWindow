package com.jpec.system_alert_window.services

import android.annotation.SuppressLint
import android.view.WindowManager

class WindowServiceNew : Service(), OnTouchListener {
    private var wm: WindowManager? = null
    private var windowGravity: String? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var windowMargin: Margin? = null
    private var windowView: LinearLayout? = null
    private var headerView: LinearLayout? = null
    private var bodyView: LinearLayout? = null
    private var footerView: LinearLayout? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0
    private var moving = false
    private var mContext: Context? = null
    @Override
    fun onCreate() {
        createNotificationChannel()
        val notificationIntent = Intent(this, SystemAlertWindowPlugin::class.kt)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0)
        val notification: Notification = Builder(this, CHANNEL_ID)
                .setContentTitle("Overlay window service is running")
                .setSmallIcon(R.drawable.ic_desktop_windows_black_24dp)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    @Override
    fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        if (null != intent && intent.getExtras() != null) {
            @SuppressWarnings("unchecked") val paramsMap: HashMap<String, Object> = intent.getSerializableExtra(INTENT_EXTRA_PARAMS_MAP) as HashMap<String, Object>
            mContext = this
            val isCloseWindow: Boolean = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
            if (!isCloseWindow) {
                assert(paramsMap != null)
                val isUpdateWindow: Boolean = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                if (isUpdateWindow && windowView != null) {
                    updateWindow(paramsMap)
                } else {
                    createWindow(paramsMap)
                }
            } else {
                closeWindow(true)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager = getSystemService(NotificationManager::class.kt)
            assert(manager != null)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun setWindowManager() {
        if (wm == null) {
            wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        }
    }

    private fun setWindowLayoutFromMap(paramsMap: HashMap<String, Object>) {
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
    }

    private val layoutParams: WindowManager.LayoutParams
        private get() {
            val params: WindowManager.LayoutParams
            params = LayoutParams()
            params.width = if (windowWidth == 0) WindowManager.LayoutParams.MATCH_PARENT else Commons.getPixelsFromDp(mContext, windowWidth)
            params.height = if (windowHeight == 0) WindowManager.LayoutParams.WRAP_CONTENT else Commons.getPixelsFromDp(mContext, windowHeight)
            params.format = PixelFormat.TRANSLUCENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            } else {
                params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT or WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            params.gravity = Commons.getGravity(windowGravity, Gravity.TOP)
            val marginTop: Int = windowMargin.getTop()
            val marginBottom: Int = windowMargin.getBottom()
            val marginLeft: Int = windowMargin.getLeft()
            val marginRight: Int = windowMargin.getRight()
            params.x = Math.max(marginLeft, marginRight)
            params.y = if (params.gravity === Gravity.TOP) marginTop else if (params.gravity === Gravity.BOTTOM) marginBottom else Math.max(marginTop, marginBottom)
            return params
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun setWindowView(params: WindowManager.LayoutParams, isCreate: Boolean) {
        val isEnableDraggable = true //params.width == WindowManager.LayoutParams.MATCH_PARENT;
        if (isCreate) {
            windowView = LinearLayout(mContext)
        }
        windowView.setOrientation(LinearLayout.VERTICAL)
        windowView.setBackgroundColor(Color.WHITE)
        windowView.setLayoutParams(params)
        windowView.removeAllViews()
        windowView.addView(headerView)
        if (bodyView != null) windowView.addView(bodyView)
        if (footerView != null) windowView.addView(footerView)
        if (isEnableDraggable) windowView.setOnTouchListener(this)
    }

    private fun createWindow(paramsMap: HashMap<String, Object>) {
        closeWindow(false)
        setWindowManager()
        setWindowLayoutFromMap(paramsMap)
        val params: WindowManager.LayoutParams = layoutParams
        setWindowView(params, true)
        wm.addView(windowView, params)
    }

    private fun updateWindow(paramsMap: HashMap<String, Object>) {
        setWindowLayoutFromMap(paramsMap)
        val params: WindowManager.LayoutParams = windowView.getLayoutParams() as WindowManager.LayoutParams
        params.width = if (windowWidth == 0) WindowManager.LayoutParams.MATCH_PARENT else Commons.getPixelsFromDp(mContext, windowWidth)
        params.height = if (windowHeight == 0) WindowManager.LayoutParams.WRAP_CONTENT else Commons.getPixelsFromDp(mContext, windowHeight)
        setWindowView(params, false)
        wm.updateViewLayout(windowView, params)
    }

    private fun closeWindow(isEverythingDone: Boolean) {
        Log.i(TAG, "Closing the overlay window")
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm.removeView(windowView)
                    windowView = null
                }
            }
            wm = null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "view not found")
        }
        if (isEverythingDone) {
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    fun onTouch(v: View?, event: MotionEvent): Boolean {
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
                val params: WindowManager.LayoutParams = windowView.getLayoutParams() as WindowManager.LayoutParams
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

    @Override
    fun onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Destroying the overlay window service")
        val notificationManager: NotificationManager = getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assert(notificationManager != null)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    @Nullable
    @Override
    fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private val TAG: String = WindowServiceNew::class.kt.getSimpleName()
        const val CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow"
        const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
    }
}