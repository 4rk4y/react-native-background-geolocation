package com.transistorsoft.rnbackgroundgeolocation;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import javax.annotation.Nonnull;

public class RNBackgroundGeolocationModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    public RNBackgroundGeolocationModule(@Nonnull ReactApplicationContext reactContext) {
        super(reactContext);
    }

}
