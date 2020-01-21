package com.transistorsoft.rnbackgroundgeolocation;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static android.content.Context.WINDOW_SERVICE;

public class RNBackgroundGeolocationModule extends ReactContextBaseJavaModule {

    private static final String TAG = "BackgroundGeolocation";
    private final ReactApplicationContext reactContext;

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";


    public RNBackgroundGeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Log.d(TAG,"RNBackgroundGeolocationModule constructor()");
    }

    @Override
    public String getName() {
        return "RNBackgroundGeolocation";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @ReactMethod
    public void start(ReadableMap params, Promise promise) {
        Log.d(TAG,"start:++");
        LocationUpdatesService.setLocationUpdateServiceCallback(new LocationUpdatesService.LocationUpdateServiceCallback() {
            @Override
            public void onStarted() {
                Log.d(TAG,"setLocationUpdateServiceCallback:onStarted()");
                //sendEvent(reactContext, "locationServiceStarted", null);
                WritableMap map = Arguments.createMap();
                map.putBoolean("enabled", true);
                sendEvent(reactContext, "statuschange", map);
            }
            @Override
            public void onStopped() {
                Log.d(TAG,"setLocationUpdateServiceCallback:onStopped()");
                //sendEvent(reactContext, "locationServiceStopped", null);
                WritableMap map = Arguments.createMap();
                map.putBoolean("enabled", false);
                sendEvent(reactContext, "statuschange", map);
            }
        });

        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("startService");
        intent.putExtra("params", Arguments.toBundle(params));
        /*ComponentName componentName  = */reactContext.startService(intent);

//        if (componentName != null) {
//            promise.resolve(null);
//        } else {
//            promise.reject("error", TAG + ": Foreground service is not started");
//        }
        promise.resolve(null);
    }

    @ReactMethod
    public void pause() {
        Log.d(TAG,"pause()");
        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("pauseService");
        reactContext.startService(intent);
    }

    @ReactMethod
    public void stop() {
        Log.d(TAG,"stop()");
        //killing service disables bluetooth auto connect
        //reactContext.stopService(new Intent(reactContext, LocationUpdatesService.class));
        pause();
    }

    @ReactMethod
    public void isLocationOn(Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("isLocationOn", LocationUpdatesService.isLocationOn()); //apiResult isn't working well on recent android apis
        promise.resolve(map);
    }

    @ReactMethod
    public void isForeground(Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("isForeground", LocationUpdatesService.isForeground()); //apiResult isn't working well on recent android apis
        promise.resolve(map);
    }

    @ReactMethod
    public void showHideNotification(boolean trueThenShow) {
        Log.d(TAG,"showNotification:{}");

        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction(trueThenShow ? "showNotification" : "hideNotification");
        reactContext.startService(intent);
    }

    @ReactMethod
    public void updateNotification(ReadableMap params) {
        Log.d(TAG,"updateNotification:{}");

        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("updateNotification");
        intent.putExtra("params", Arguments.toBundle(params));
        reactContext.startService(intent);
    }

    @ReactMethod
    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }


    @ReactMethod
    public void testCallback(String message, Callback callback) {
        callback.invoke(message, 1, 2, "str");
    }

    @ReactMethod
    public void toast(String message) {
        Toast.makeText(getReactApplicationContext(), message, 0).show();
    }


    // ==== test methods ==== //
    @ReactMethod
    public void testPromise(boolean trueThenSuccess, int intArg, String strArg, Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putInt("intArg", intArg);
        map.putString("strArg", strArg);
        if(trueThenSuccess) {
            promise.resolve(map);
        } else {
            promise.reject("errCode", "errMessage");
        }
    }

    @ReactMethod
    public void testEvent(boolean trueThenSuccess, int intArg, String strArg, Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putInt("intArg", intArg);
        map.putString("strArg", strArg);
        if(trueThenSuccess) {
            promise.resolve(map);
        } else {
            promise.reject("errCode", "errMessage");
        }
    }

    @ReactMethod
    public void eventTest() {
        WritableMap params = Arguments.createMap();
        params.putString("eventProperty", "someValue123");
        sendEvent(reactContext, "BackgroundGeolocationEvent", params);
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        Log.d(TAG,"sendEvent:" + eventName + ":" + params.toString());
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

//    mAutoConnectBluetoothId = params.getString("bluetoothId");
//    mAutoConnectBluetoothName = params.getString("bluetoothName");

    @ReactMethod
    public void getPairedBluetoothList(final Promise promise) {
        Log.d(TAG,"###:[BTAutoConn]getPairedBluetoothList00");
        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("getPairedBluetoothList");

        intent.putExtra("getPairedBluetoothListReceiver", new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Log.d(TAG,"[BTAutoConn]onReceiveResult:retCode:" + resultCode);
                Log.d(TAG,"[BTAutoConn]onReceiveResult:resultData:" + resultData.toString());
                //if (resultCode == StockService.RESULT_ID_QUOTE) {

                //}
                Log.d(TAG,"###:[BTAutoConn]getPairedBluetoothList02");
                promise.resolve(Arguments.fromBundle(resultData));
                Log.d(TAG,"###:[BTAutoConn]getPairedBluetoothList03");
                //promise.reject("error", TAG + ": Foreground service is not started");
            }
        });
        reactContext.startService(intent);
        Log.d(TAG,"###:[BTAutoConn]getPairedBluetoothList01");
    }

    @ReactMethod
    public void startBluetoothService(ReadableMap params) {
        Log.d(TAG,"[BTAutoConn]:startBluetoothService");
        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("startBluetooth");
        intent.putExtra("params", Arguments.toBundle(params));
        reactContext.startService(intent);
    }

    @ReactMethod
    public void stopBluetoothService() {
        Log.d(TAG,"[BTAutoConn]:stopBluetoothService");
        Intent intent = new Intent(reactContext, LocationUpdatesService.class);
        intent.setAction("stopBluetooth");
        //intent.putExtra("params", Arguments.toBundle(params));
        reactContext.startService(intent);
    }

    //https://stackoverflow.com/questions/28983621/detect-soft-navigation-bar-availability-in-android-device-progmatically
    /**
     * Returns {@code null} if this couldn't be determined.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @SuppressLint("PrivateApi")
    public static boolean hasNavigationBar() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            IBinder serviceBinder = (IBinder)serviceManager.getMethod("getService", String.class).invoke(serviceManager, "window");
            Class<?> stub = Class.forName("android.view.IWindowManager$Stub");
            Object windowManagerService = stub.getMethod("asInterface", IBinder.class).invoke(stub, serviceBinder);
            Method hasNavigationBar = windowManagerService.getClass().getMethod("hasNavigationBar");
            return (boolean)hasNavigationBar.invoke(windowManagerService);
        } catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.w("YOUR_TAG_HERE", "Couldn't determine whether the device has a navigation bar", e);
            return false;
        }
    }

    public static boolean hasSoftKeys(WindowManager windowManager) {
        Display d = windowManager.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        Log.d(TAG, "hasSoftKeysW:" + realWidth  + "/" + displayWidth );
        Log.d(TAG, "hasSoftKeysH:" + realHeight + "/" + displayHeight);
        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }

    @ReactMethod
    public void hasSoftKey(Promise promise) {
        WritableMap map = Arguments.createMap();

        boolean[] hasSoftKey = new boolean[5];

        int id = reactContext.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        hasSoftKey[0] =  id > 0 && reactContext.getResources().getBoolean(id);

        hasSoftKey[1] =  ViewConfiguration.get(reactContext).hasPermanentMenuKey();

        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);

        hasSoftKey[2] =  (!(hasBackKey && hasHomeKey));

        hasSoftKey[3] = hasNavigationBar();

        hasSoftKey[4] = hasSoftKeys((WindowManager)reactContext.getSystemService(WINDOW_SERVICE));

        for(int i=0; i<hasSoftKey.length; i++) {
            map.putBoolean("hasSoftKey" + i, hasSoftKey[i]);
        }

        promise.resolve(map);
    }


    private final HashMap<String, Integer> mListeners = new HashMap<>();

    @ReactMethod
    public void addEventListener(String event) {
        Log.d(TAG, "addEventListener:" + event);

//        if (!mEvents.contains(event)) {
//            TSLog.logger.warn(TSLog.warn("[RNBackgroundGeolocation addListener] Unknown event: " + event));
//            return;
//        }
//        BackgroundGeolocation adapter = getAdapter();
        Integer count;

        synchronized(mListeners) {
            if (mListeners.containsKey(event)) {
                count = mListeners.get(event);
                count++;
                mListeners.put(event, count);
            } else {
                count = 1;
                mListeners.put(event, count);
            }
        }
        if (count == 1) {
//            if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_LOCATION)) {
//                adapter.onLocation(new LocationCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_MOTIONCHANGE)) {
//                adapter.onMotionChange(new MotionChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_ACTIVITYCHANGE)) {
//                adapter.onActivityChange(new ActivityChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_PROVIDERCHANGE)) {
//                adapter.onLocationProviderChange(new LocationProviderChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_GEOFENCESCHANGE)) {
//                adapter.onGeofencesChange(new GeofencesChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_GEOFENCE)) {
//                adapter.onGeofence(new GeofenceCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_HEARTBEAT)) {
//                adapter.onHeartbeat(new HeartbeatCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_HTTP)) {
//                adapter.onHttp(new HttpResponseCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_SCHEDULE)) {
//                adapter.onSchedule(new ScheduleCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_POWERSAVECHANGE)) {
//                adapter.onPowerSaveChange(new PowerSaveChangeCallack());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_CONNECTIVITYCHANGE)) {
//                adapter.onConnectivityChange(new ConnectivityChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_ENABLEDCHANGE)) {
//                adapter.onEnabledChange(new EnabledChangeCallback());
//            } else if (event.equalsIgnoreCase(BackgroundGeolocation.EVENT_NOTIFICATIONACTION)) {
//                adapter.onNotificationAction(new NotificationActionCallback());
//            } else if (event.equalsIgnoreCase(TSAuthorization.NAME)) {
//                HttpService.getInstance(getReactApplicationContext()).onAuthorization(new AuthorizationCallback());
//            }
        }
    }

    @ReactMethod
    public void removeListener(String event) {
        Integer count;

        synchronized (mListeners) {
            if (mListeners.containsKey(event)) {
                count = mListeners.get(event);
                count--;
                if (count > 0) {
                    mListeners.put(event, count);
                } else {
//                    getAdapter().removeListeners(event);
                }
            }
        }
    }


}
