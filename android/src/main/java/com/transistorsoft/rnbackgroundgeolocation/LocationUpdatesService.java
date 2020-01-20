/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.transistorsoft.rnbackgroundgeolocation;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

//import com.chabotcorpcar.MainActivity;
//import com.chabotcorpcar.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Set;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "ChabotBiz";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mLocation;
    private Location mLastLocation;

    private static boolean mIsForeground = false;
    private static boolean mIsLocationOn = false; //notiifcation service is on but gps is not comming
    private double mMinDist = 15.0; //min update meter

    private String mAutoConnectBluetoothAddr = null;
    private String mAutoConnectBluetoothName = null;

    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);


            mChannel.setDescription("no sound");
            mChannel.setSound(null,null); //<---- ignore sound
            mChannel.enableLights(false);
            //mChannel.setLightColor(Color.BLUE);
            mChannel.enableVibration(false);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    private void initBluetoothService() {
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//
//        List<String> s = new ArrayList<>();
//        for(BluetoothDevice bt : pairedDevices) {
//            s.add(bt.getName());
//            Log.d(TAG, "###" + ":bt:" + bt.getName());
//        }

        //setListAdapter(new ArrayAdapter<String>(this, R.layout.list, s));
        IntentFilter filter = new IntentFilter();
        //filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //ACTION_ACL_DISCONNECT
        mBondingBroadcastReceiver = createBondingBroadcastReceiver();
        this.registerReceiver(mBondingBroadcastReceiver, filter);
        Log.d(TAG, "Bluetooth" + ":" + "registered");
    }

    //private String mCarName, mCarDist;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Bundle params = intent.getExtras() != null ? intent.getExtras().getBundle("params") : null;
        Log.d(TAG,"onStartCommand:{action:" + action + "}");
        if(params!=null)
            Log.d(TAG,"onStartCommand:{params:" + params.toString() + "}");

        if(action.equals("startService"))
        {
            Log.i(TAG, "Service started");
//            boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
//                    false);
//
//            // We got here because the user decided to remove location updates from the notification.
//            if (startedFromNotification) {
//                Log.i(TAG, "Service started:-1");
//                //removeLocationUpdates();
//                //stopSelf(); //!fixme: check later
//            } else {
                try {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            mLocationCallback, Looper.myLooper());
                    startForeground(NOTIFICATION_ID,
                            getNotification(
                                    params.getString("carName"),
                                    params.getString("carDist"),
                                    params.getString("btnMode")
                            ));
                    mMinDist = params.getDouble("minDist");
                    mLastLocation = null;
                    mIsForeground = true;
                    mIsLocationOn = true;
                } catch (SecurityException unlikely) {
                    ///Utils.setRequestingLocationUpdates(this, false);
                    Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
                }
//          }
            // Tells the system to not try to recreate the service after it has been killed.

            mLocationUpdateServiceCallback.onStarted();
        }
        else if(action.equals("pauseService")) {
            mIsLocationOn = false;
        }
        else if(action.equals("updateNotification"))
        {
            if (serviceIsRunningInForeground(this)) {
                mNotificationManager.notify(NOTIFICATION_ID, getNotification(
                        params.getString("carName"),
                        params.getString("carDist"),
                        params.getString("btnMode")
                ));
            }
        }
        else if(action.equals("startBluetooth"))
        {
            mAutoConnectBluetoothAddr = params.getString("bluetoothAddr");
            mAutoConnectBluetoothName = params.getString("bluetoothName");
            Log.d(TAG,"[BTAutoConn]startBluetooth:"
                    + mAutoConnectBluetoothAddr + "/" + mAutoConnectBluetoothName);

            initBluetoothService();
        }
        else if(action.equals("stopBluetooth"))
        {
            Log.d(TAG,"###:-stopBluetooth");
            if(mBondingBroadcastReceiver!=null)
                this.unregisterReceiver(mBondingBroadcastReceiver);
            mBondingBroadcastReceiver = null;
        }
        else if(action.equals("getPairedBluetoothList")) {
            Log.d(TAG,"[BTAutoConn]getPairedBluetoothList:");

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            ArrayList<String> btAddrs = new ArrayList<>();
            ArrayList<String> btNames = new ArrayList<>();

            for(BluetoothDevice bt : pairedDevices) {
                Log.d(TAG,"[BTAutoConn]getPairedBluetoothList:bt:" + bt.getName() + "/" + bt.getAddress());
                btAddrs.add(bt.getAddress());
                btNames.add(bt.getName());
            }

            Bundle bundle = new Bundle();
            bundle.putStringArrayList("btAddrs", btAddrs);
            bundle.putStringArrayList("btNames", btNames);

            ResultReceiver rec = intent.getParcelableExtra("getPairedBluetoothListReceiver");
            rec.send(1, bundle);
            Log.d(TAG,"[BTAutoConn]getPairedBluetoothList:rec.send");
        }

        return START_NOT_STICKY;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
//        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
//        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
//        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
//            Log.i(TAG, "Starting foreground service");
//
////            startForeground(NOTIFICATION_ID, getNotification());
//        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved()");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        try {
            Log.i(TAG, "Removing location updates");
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            stopSelf();
            stopForeground(true);
            mServiceHandler.removeCallbacksAndMessages(null);
            mIsForeground = false;
            mIsLocationOn = false;

            if(mLocationUpdateServiceCallback!=null)
                mLocationUpdateServiceCallback.onStopped();
            else
                Log.e(TAG,"onDestroy():mLocationUpdateServiceCallback is null");

        } catch (SecurityException unlikely) {
            //Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }

        if(mBondingBroadcastReceiver!=null) {
            Log.d(TAG, "Bluetooth" + ":" + "unregistered");
            this.unregisterReceiver(mBondingBroadcastReceiver);
            mBondingBroadcastReceiver = null;
        }
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        //Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            ///Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }


    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification(String carName, String carDist, String btnMode) {
        Log.d(TAG,"getNotification:{carName:" + carName + ",carDist:" + carDist + "}");
        // The PendingIntent that leads to a call to onStartCommand() in this service.

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .addAction(R.drawable.ic_stat_ic_notification, getString(R.string.launch_activity),
//                        activityPendingIntent)
//                .addAction(R.drawable.ic_stat_ic_notification, getString(R.string.call_insu),
//                        servicePendingIntent)
//                .setContentText(text)
//                .setContentTitle(Utils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_noti_icon) //!fixme: move to main activity?
                //.setTicker(text)
                //.setDefaults(Notification.DEFAULT_LIGHT | Notification.DEFAULT_SOUND)
                .setVibrate(new long[]{0L}) // Passing null here silently fails
                .setWhen(System.currentTimeMillis());


        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        RemoteViews contentView = new RemoteViews(this.getPackageName(), R.layout.custom_push);
        contentView.setTextViewText(R.id.tvCarName, carName);
        contentView.setTextViewText(R.id.tvDistance, carDist);

        if(btnMode.equals("showStart")) {
            contentView.setViewVisibility(R.id.btnStartRecord, View.VISIBLE);
            contentView.setViewVisibility(R.id.btnStopRecord,  View.GONE);
        } else if(btnMode.equals("showStop")){
            contentView.setViewVisibility(R.id.btnStartRecord, View.GONE);
            contentView.setViewVisibility(R.id.btnStopRecord,  View.VISIBLE);
        } else {
            Log.e(TAG, "unknown btnMode:" + btnMode);
        }


        Intent intent;
        PendingIntent pendingIntent;
        int reqCode = 0;

        //layout button functions...
        intent = new Intent(this, LocationHeadlessTask.class);
        intent.putExtra("notiBtnClick", "startRecord");
        pendingIntent = PendingIntent.getService(this, reqCode++, intent, FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.btnStartRecord, pendingIntent);

        intent = new Intent(this, LocationHeadlessTask.class);
        intent.putExtra("notiBtnClick", "stopRecord");
        pendingIntent = PendingIntent.getService(this, reqCode++, intent, FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.btnStopRecord, pendingIntent);

        intent = new Intent(this, LocationHeadlessTask.class);
        intent.putExtra("notiBtnClick", "insuCall");
        pendingIntent = PendingIntent.getService(this, reqCode++, intent, FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.btnInsuCall, pendingIntent);

        //open app on click layout
        PackageManager pm = this.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("com.chabotcorpcar");
        pendingIntent = PendingIntent.getActivity(this, reqCode++,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.rlNotiLayout, pendingIntent);


        Notification notification;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            notification = builder.setCustomContentView(contentView).build();
        } else {
            notification = builder.build();
            notification.contentView = contentView;
        }
        return notification;
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void callHeadlessTask(Location location) {
        Log.i(TAG, "callHeadlessTask: " + location);
        Intent service = new Intent(getApplicationContext(), LocationHeadlessTask.class);
        Bundle bundle = new Bundle();
        bundle.putDouble("Lat",  location.getLatitude());
        bundle.putDouble("Long", location.getLongitude());
        service.putExtras(bundle);

        getApplicationContext().startService(service);
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "onNewLocation:location: " + location);

        if(!mIsLocationOn) {
            Log.i(TAG, "onNewLocation:mIsLocationOn:false: -> skipped");
            return;
        }

        mLocation = location;

        if(mLastLocation!=null) {
            double distMeter = distance(
                location.getLatitude(), location.getLongitude(),
                mLastLocation.getLatitude(), mLastLocation.getLongitude(), "K"
            )/1000.0;

            boolean skip = distMeter < mMinDist;
            Log.d(TAG, "onNewLocation:Location distMeter:" + distMeter + "< minDist(" + mMinDist + ")" + "update:" + !skip);
            if(skip) {
                Log.d(TAG, "onNewLocation:Skipped");
                return;
            }
        }

        // Notify anyone listening for broadcasts about the new location.
//        Intent intent = new Intent(ACTION_BROADCAST);
//        intent.putExtra(EXTRA_LOCATION, location);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service. -> move to after headlesstask
//        if (serviceIsRunningInForeground(this)) {
//            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
//        }

        callHeadlessTask(location);
        mLastLocation = mLocation;
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //https://developer.android.com/training/location/change-location-settings#java
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); //100meter
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public static boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (context.getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isForeground() {
        return mIsForeground;
    }

    public static boolean isLocationOn() {
        return mIsLocationOn;
    }

    static LocationUpdateServiceCallback mLocationUpdateServiceCallback;

    public interface LocationUpdateServiceCallback {
        void onStarted();
        void onStopped();
    }

    public static void setLocationUpdateServiceCallback(LocationUpdateServiceCallback callback) {
        mLocationUpdateServiceCallback = callback;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }


    private BroadcastReceiver mBondingBroadcastReceiver;
    private BroadcastReceiver createBondingBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                Log.d(TAG, "[BTAutoConnn]onReceive:action:" + intent.getAction() + ":Bond state changed for: " + device.getName() + " new state: " + bondState + " previous: " + previousBondState);
                Log.d(TAG, "[BTAutoConnn]onReceive:target:" + mAutoConnectBluetoothAddr + "/" + mAutoConnectBluetoothName);


                // skip other devices
                //if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
                //return;

                String deviceName = device.getName();

                //            public static final int BOND_BONDED = 12;
                //            public static final int BOND_BONDING = 11;
                //            public static final int BOND_NONE = 10;
                //if (bondState == BluetoothDevice.BOND_BONDED)
                //{
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    Log.d(TAG, "[BTAutoConn]Bluetooth" + deviceName + ":" + "connected");
                    // Continue to do what you've started before
                    //enableGlucoseMeasurementNotification(mBluetoothGatt);

                    //mContext.unregisterReceiver(this);
                    //mCallbacks.onBonded();
                    if (deviceName.equals(mAutoConnectBluetoothName)) {
                        //call app by intent and press start button
                        //deep link...
                        Intent _intent = new Intent(context, LocationHeadlessTask.class);
                        _intent.putExtra("notiBtnClick", "startRecord");
                        context.startService(_intent);
                        Log.d(TAG, "[BTAutoConnn]onReceive:connected->call startRecord");
                    } else {
                        Log.d(TAG, "[BTAutoConn]Not Target Device" + deviceName + "!=" + mAutoConnectBluetoothName);
                    }
                }

                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    if (deviceName.equals(mAutoConnectBluetoothName)) {
                        Intent _intent = new Intent(context, LocationHeadlessTask.class);
                        _intent.putExtra("notiBtnClick", "stopRecord");
                        context.startService(_intent);

                        Log.d(TAG, "[BTAutoConnn]onReceive:disconnected:pauseService");
                    }
                }
            }
        };
    }
}
