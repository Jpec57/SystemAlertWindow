package com.jpec.system_alert_window

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.jpec.system_alert_window.services.WindowServiceNew
import com.jpec.system_alert_window.utils.Commons
import com.jpec.system_alert_window.utils.Constants
import com.jpec.system_alert_window.utils.NotificationHelper
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.view.FlutterCallbackInformation
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments

import com.jpec.system_alert_window.services.WindowServiceNew.INTENT_EXTRA_IS_CLOSE_WINDOW
import com.jpec.system_alert_window.services.WindowServiceNew.INTENT_EXTRA_IS_UPDATE_WINDOW
import com.jpec.system_alert_window.utils.Constants.CHANNEL
import com.jpec.system_alert_window.utils.Constants.INTENT_EXTRA_PARAMS_MAP

class SystemAlertWindowPlugin private constructor(context: Context, activity: Activity, newMethodChannel: MethodChannel) : FlutterPlugin(), MethodCallHandler {
    private val mContext: Context?
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    fun onMethodCall(call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android " + Build.VERSION.RELEASE)
            "requestPermissions" -> if (askPermission()) {
                result.success(true)
            } else {
                result.success(false)
            }
            "checkPermissions" -> if (checkPermission()) {
                result.success(true)
            } else {
                result.success(false)
            }
            "showSystemWindow" -> if (checkPermission()) {
                assert(call.arguments != null)
                val arguments: List = call.arguments
                val title = arguments.get(0) as String
                val body = arguments.get(1) as String
                val params = arguments.get(2) as HashMap<String, Object>
                if (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.d(TAG, "Going to show Bubble")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        showBubble(title, body, params)
                    }
                } else {
                    Log.d(TAG, "Going to show System Alert Window")
                    val i = Intent(mContext, WindowServiceNew::class.kt)
                    i.putExtra(INTENT_EXTRA_PARAMS_MAP, params)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    i.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                    mContext!!.startService(i)
                }
                result.success(true)
            } else {
                Toast.makeText(mContext, "Please give draw over other apps permission", Toast.LENGTH_LONG).show()
                result.success(false)
            }
            "updateSystemWindow" -> if (checkPermission()) {
                assert(call.arguments != null)
                val updateArguments: List = call.arguments
                val updateTitle = updateArguments.get(0) as String
                val updateBody = updateArguments.get(1) as String
                val updateParams = updateArguments.get(2) as HashMap<String, Object>
                if (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    Log.d(TAG, "Going to update Bubble")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        showBubble(updateTitle, updateBody, updateParams)
                    }
                } else {
                    Log.d(TAG, "Going to update System Alert Window")
                    val i = Intent(mContext, WindowServiceNew::class.kt)
                    i.putExtra(INTENT_EXTRA_PARAMS_MAP, updateParams)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    i.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, true)
                    mContext!!.startService(i)
                }
                result.success(true)
            } else {
                Toast.makeText(mContext, "Please give draw over other apps permission", Toast.LENGTH_LONG).show()
                result.success(false)
            }
            "closeSystemWindow" -> if (checkPermission()) {
                if (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    NotificationHelper.getInstance(mContext).dismissNotification()
                } else {
                    val i = Intent(mContext, WindowServiceNew::class.kt)
                    i.putExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, true)
                    mContext!!.startService(i)
                }
                result.success(true)
            } else {
                Toast.makeText(mContext, "Please give draw over other apps permission", Toast.LENGTH_LONG).show()
                result.success(false)
            }
            "openLbc" -> {
                System.out.println("OPENING LBC")
                val intent = mContext!!.packageManager.getLaunchIntentForPackage("com.lbc.lbc_app")
                intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mContext.startActivity(intent)
            }
            "openMeet" -> {
                System.out.println("OPENING MEET")
                val intentMeet = mContext!!.packageManager.getLaunchIntentForPackage("com.lbc.lbc_app")
                intentMeet!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mContext.startActivity(intentMeet)
            }
            "registerCallBackHandler" -> try {
                val callBackArguments: List = call.arguments
                if (callBackArguments != null) {
                    val callbackHandle: Long = Long.parseLong(String.valueOf(callBackArguments.get(0)))
                    val onClickHandle: Long = Long.parseLong(String.valueOf(callBackArguments.get(1)))
                    val preferences = mContext!!.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0)
                    preferences.edit().putLong(Constants.CALLBACK_HANDLE_KEY, callbackHandle)
                            .putLong(Constants.CODE_CALLBACK_HANDLE_KEY, onClickHandle).apply()
                    startCallBackHandler(mContext)
                    result.success(true)
                } else {
                    Log.e(TAG, "Unable to register on click handler. Arguments are null")
                    result.success(false)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception in registerOnClickHandler $ex")
                result.success(false)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(mContext)) {
                Log.e(TAG, "System Alert Window will not work without 'Can Draw Over Other Apps' permission")
                Toast.makeText(mContext,
                        "System Alert Window will not work without 'Can Draw Over Other Apps' permission",
                        Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun askPermission(): Boolean {
        if (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return NotificationHelper.getInstance(mContext).areBubblesAllowed()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mContext!!.packageName))
                if (mActivity == null) {
                    if (mContext != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        mContext.startActivity(intent)
                        Toast.makeText(mContext, "Please grant, Can Draw Over Other Apps permission.",
                                Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Can't detect the permission change, as the mActivity is null")
                    } else {
                        Log.e(TAG, "'Can Draw Over Other Apps' permission is not granted")
                        Toast.makeText(mContext,
                                "Can Draw Over Other Apps permission is required. Please grant it from the app settings",
                                Toast.LENGTH_LONG).show()
                    }
                } else {
                    mActivity!!.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
                }
            } else {
                return true
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun checkPermission(): Boolean {
        if (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // return NotificationHelper.getInstance(mContext).areBubblesAllowed();
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(mContext)
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun showBubble(title: String, body: String, params: HashMap<String, Object>) {
        val icon = Icon.createWithResource(mContext, R.drawable.ic_notification)
        val notificationHelper: NotificationHelper = NotificationHelper.getInstance(mContext)
        notificationHelper.showNotification(icon, title, body, params)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mActivity: Activity?

        @SuppressLint("StaticFieldLeak")
        private var sBackgroundFlutterView: FlutterNativeView? = null
        private var sPluginRegistrantCallback: PluginRegistry.PluginRegistrantCallback? = null
        var sIsIsolateRunning = AtomicBoolean(false)
        var methodChannel: MethodChannel
        var backgroundChannel: MethodChannel? = null
        var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1237
        private val notificationManager: NotificationManager? = null
        private const val TAG = "SystemAlertWindowPlugin"
        @SuppressWarnings("unused")
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), CHANNEL)
            channel.setMethodCallHandler(SystemAlertWindowPlugin(registrar.context(), registrar.activity(), channel))
        }

        fun setPluginRegistrant(callback: PluginRegistry.PluginRegistrantCallback?) {
            sPluginRegistrantCallback = callback
        }

        fun startCallBackHandler(context: Context?) {
            val preferences = context!!.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0)
            val callBackHandle = preferences.getLong(Constants.CALLBACK_HANDLE_KEY, -1)
            Log.d(TAG, "onClickCallBackHandle $callBackHandle")
            if (callBackHandle != -1L) {
                FlutterMain.ensureInitializationComplete(context, null)
                val mAppBundlePath: String = FlutterMain.findAppBundlePath()
                val flutterCallback: FlutterCallbackInformation = FlutterCallbackInformation
                        .lookupCallbackInformation(callBackHandle)
                if (sBackgroundFlutterView == null) {
                    sBackgroundFlutterView = FlutterNativeView(context, true)
                    if (mAppBundlePath != null && !sIsIsolateRunning.get()) {
                        if (sPluginRegistrantCallback == null) {
                            Log.i(TAG, "Unable to start callBackHandle... as plugin is not registered")
                            return
                        }
                        Log.i(TAG, "Starting callBackHandle...")
                        val args = FlutterRunArguments()
                        args.bundlePath = mAppBundlePath
                        args.entrypoint = flutterCallback.callbackName
                        args.libraryPath = flutterCallback.callbackLibraryPath
                        sBackgroundFlutterView.runFromBundle(args)
                        sPluginRegistrantCallback.registerWith(sBackgroundFlutterView.getPluginRegistry())
                        backgroundChannel = MethodChannel(sBackgroundFlutterView, Constants.BACKGROUND_CHANNEL)
                        sIsIsolateRunning.set(true)
                    }
                } else {
                    if (backgroundChannel == null) {
                        backgroundChannel = MethodChannel(sBackgroundFlutterView, Constants.BACKGROUND_CHANNEL)
                    }
                    sIsIsolateRunning.set(true)
                }
            }
        }

        fun invokeCallBack(context: Context, type: String, params: Object) {
            val argumentsList: List<Object> = ArrayList()
            Log.v(TAG, "invoking callback for tag $params")
            /*
         * try { argumentsList.add(type); argumentsList.add(params); Log.v(TAG,
         * "invoking callback for tag "+params); methodChannel.invokeMethod("callBack",
         * argumentsList); } catch (Exception ex) { Log.e(TAG,
         * "invokeCallBack Exception : " + ex.toString()); SharedPreferences preferences
         * = context.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0);
         * long codeCallBackHandle =
         * preferences.getLong(Constants.CODE_CALLBACK_HANDLE_KEY, -1); Log.i(TAG,
         * "codeCallBackHandle " + codeCallBackHandle); if (codeCallBackHandle == -1) {
         * Log.e(TAG, "invokeCallBack failed, as codeCallBackHandle is null"); } else {
         * argumentsList.clear(); argumentsList.add(codeCallBackHandle);
         * argumentsList.add(type); argumentsList.add(params);
         * backgroundChannel.invokeMethod("callBack", argumentsList); } }
         */
            val preferences = context.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0)
            val codeCallBackHandle = preferences.getLong(Constants.CODE_CALLBACK_HANDLE_KEY, -1)
            // Log.i(TAG, "codeCallBackHandle " + codeCallBackHandle);
            if (codeCallBackHandle == -1L) {
                Log.e(TAG, "invokeCallBack failed, as codeCallBackHandle is null")
            } else {
                argumentsList.clear()
                argumentsList.add(codeCallBackHandle)
                argumentsList.add(type)
                argumentsList.add(params)
                if (sIsIsolateRunning.get()) {
                    if (backgroundChannel == null) {
                        Log.v(TAG, "Recreating the background channel as it is null")
                        backgroundChannel = MethodChannel(sBackgroundFlutterView, Constants.BACKGROUND_CHANNEL)
                    }
                    try {
                        Log.v(TAG, "Invoking on method channel")
                        val retries = intArrayOf(2)
                        invokeCallBackToFlutter(backgroundChannel, "callBack", argumentsList, retries)
                        // backgroundChannel.invokeMethod("callBack", argumentsList);
                    } catch (ex: Exception) {
                        Log.e(TAG, "Exception in invoking callback $ex")
                    }
                } else {
                    Log.e(TAG, "invokeCallBack failed, as isolate is not running")
                }
            }
        }

        private fun invokeCallBackToFlutter(channel: MethodChannel?, method: String,
                                            arguments: List<Object>, retries: IntArray) {
            channel.invokeMethod(method, arguments, object : Result() {
                @Override
                fun success(o: Object?) {
                    Log.i(TAG, "Invoke call back success")
                }

                @Override
                fun error(s: String, s1: String, o: Object?) {
                    Log.e(TAG, "Error $s$s1")
                }

                @Override
                fun notImplemented() {
                    // To fix the dart initialization delay.
                    if (retries[0] > 0) {
                        Log.d(TAG, "Not Implemented method $method. Trying again to check if it works")
                        invokeCallBackToFlutter(channel, method, arguments, retries)
                    } else {
                        Log.e(TAG, "Not Implemented method $method")
                    }
                    retries[0]--
                }
            })
        }
    }

    init {
        mContext = context
        mActivity = activity
        methodChannel = newMethodChannel
        methodChannel.setMethodCallHandler(this)
    }
}