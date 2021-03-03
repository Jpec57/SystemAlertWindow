package com.jpec.system_alert_window.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

import com.jpec.system_alert_window.R;
import com.jpec.system_alert_window.SystemAlertWindowPlugin;

import static com.jpec.system_alert_window.services.WindowServiceNew.INTENT_EXTRA_IS_CLOSE_WINDOW;
import static com.jpec.system_alert_window.utils.Constants.INTENT_EXTRA_PARAMS_MAP;

public class ConfirmCloseDialog extends Service implements View.OnTouchListener {

    private static final String TAG = ConfirmCloseDialog.class.getSimpleName();
    public static final String CHANNEL_ID = "JpecConfirmCloseChannel";
    private static final int NOTIFICATION_ID = 2;
    private static final String INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow";
    public static final String INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow";

    private WindowManager wm;
    private LinearLayout windowView;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;

    private Context mContext;

    @Override
    public void onCreate() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SystemAlertWindowPlugin.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Overlay window service is running")
                .setSmallIcon(R.drawable.ic_desktop_windows_black_24dp)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent && intent.getExtras() != null) {
            mContext = this;
            boolean isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false);
            if (!isCloseWindow) {
                boolean isUpdateWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false);
                if (isUpdateWindow && windowView != null) {
                } else {
                    createWindow();
                }
            } else {
                closeWindow(true);
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Confirm Close Dialog Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void setWindowManager() {
        if (wm == null) {
            wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }
    }

    private WindowManager.LayoutParams getLayoutParams() {
        final WindowManager.LayoutParams params;
        params = new WindowManager.LayoutParams();
        params.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            params.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            params.type = android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            params.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        params.gravity = Gravity.CENTER;
        int marginTop = 0;
        int marginBottom = 0;
        int marginLeft = 0;
        int marginRight = 0;
        params.x = Math.max(marginLeft, marginRight);
        params.y = Math.max(marginTop, marginBottom);
        return params;
    }

    private void createWindow() {
        closeWindow(false);
        setWindowManager();
        windowView = new LinearLayout(mContext);
        WindowManager.LayoutParams params = getLayoutParams();
        View dialogView = View.inflate(mContext, R.layout.confirm_close_dialog, null);
        Button confirmButton = (Button)dialogView.findViewById(R.id.confirm);
        confirmButton.setOnClickListener(v -> {
            //CLASS NAME
            final Intent i = new Intent(mContext, com.jpec.system_alert_window.services.WindowServiceNew.class);
            i.putExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, true);
            mContext.startService(i);
            closeWindow(true);
        });
        Button cancelButton = (Button)dialogView.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(v -> closeWindow(true));
        windowView.addView(dialogView);
        windowView.setOnTouchListener(this);
        wm.addView(windowView, params);
    }

    private void closeWindow(boolean isEverythingDone) {
        Log.i(TAG, "Closing the overlay window");
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm.removeView(windowView);
                    windowView = null;
                }
            }
            wm = null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "view not found");
        }
        if(isEverythingDone){
            stopSelf();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (null != wm) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getRawX();
                float y = event.getRawY();
                moving = false;
                int[] location = new int[2];
                windowView.getLocationOnScreen(location);
                originalXPos = location[0];
                originalYPos = location[1];
                offsetX = originalXPos - x;
                offsetY = originalYPos - y;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX();
                float y = event.getRawY();
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) windowView.getLayoutParams();
                int newX = (int) (offsetX + x);
                int newY = (int) (offsetY + y);
                if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                    return false;
                }
                params.x = newX;
                params.y = newY;
                wm.updateViewLayout(windowView, params);
                moving = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                return moving;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying the overlay window service");
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
