/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.utils.cmremix;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.util.WeatherUtils;

import com.android.internal.utils.cmremix.WeatherControllerUtils;

import java.util.ArrayList;

import static cyanogenmod.providers.WeatherContract.WeatherColumns.CURRENT_CITY;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.CURRENT_CONDITION;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.CURRENT_CONDITION_CODE;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.CURRENT_TEMPERATURE;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.CURRENT_TEMPERATURE_UNIT;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit.CELSIUS;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT;

public class WeatherControllerImpl implements WeatherController {

    private static final String TAG = WeatherController.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private WeatherContentObserver mWeatherContentObserver;
    private Handler mHandler;
    private int mWeatherUnit;
    private Uri mWeatherTempetarureUri;

    public static final ComponentName COMPONENT_WEATHER_FORECAST = new ComponentName(
            "com.cyanogenmod.lockclock", "com.cyanogenmod.lockclock.weather.ForecastActivity");
    public static final String ACTION_FORCE_WEATHER_UPDATE
            = "com.cyanogenmod.lockclock.action.FORCE_WEATHER_UPDATE";
    private static final String[] WEATHER_PROJECTION = new String[]{
            CURRENT_CITY,
            CURRENT_CONDITION,
            CURRENT_CONDITION_CODE,
            CURRENT_TEMPERATURE,
            CURRENT_TEMPERATURE_UNIT
    };
    public static final String LOCK_CLOCK_PACKAGE_NAME = "com.cyanogenmod.lockclock";

    private static final int WEATHER_ICON_MONOCHROME = 0;
    private static final int WEATHER_ICON_COLORED = 1;

    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private final Context mContext;

    private WeatherInfo mCachedInfo = new WeatherInfo();

    public WeatherControllerImpl(Context context) {
        mContext = context;
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mHandler = new Handler();
        mWeatherContentObserver = new WeatherContentObserver(mHandler);
        mWeatherTempetarureUri
                = CMSettings.Global.getUriFor(CMSettings.Global.WEATHER_TEMPERATURE_UNIT);
        mContext.getContentResolver().registerContentObserver(
                WeatherContract.WeatherColumns.CURRENT_WEATHER_URI,true, mWeatherContentObserver);
        mContext.getContentResolver().registerContentObserver(mWeatherTempetarureUri, true,
                mWeatherContentObserver);
        queryWeatherTempUnit();
        queryWeather();
    }

    public void addCallback(Callback callback) {
        if (callback == null || mCallbacks.contains(callback)) return;
        if (DEBUG) Log.d(TAG, "addCallback " + callback);
        mCallbacks.add(callback);
        callback.onWeatherChanged(mCachedInfo); // immediately update with current values
    }

    public void removeCallback(Callback callback) {
        if (callback == null) return;
        if (DEBUG) Log.d(TAG, "removeCallback " + callback);
        mCallbacks.remove(callback);
    }

    private Drawable getIcon(int conditionCode) {
        int iconNameValue = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 0);
        String iconName;

        if (iconNameValue == WEATHER_ICON_MONOCHROME) {
            iconName = "weather_";
        } else if (iconNameValue == WEATHER_ICON_COLORED) {
            iconName = "weather_color_";
        } else {
            iconName = "weather_vclouds_";
        }

        try {
            Resources resources =
                    mContext.createPackageContext(LOCK_CLOCK_PACKAGE_NAME, 0).getResources();
            return resources.getDrawable(resources.getIdentifier(iconName + WeatherControllerUtils.addOffsetToConditionCodeFromWeatherContract(conditionCode),
                    "drawable", LOCK_CLOCK_PACKAGE_NAME));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public WeatherInfo getWeatherInfo() {
        return mCachedInfo;
    }

    private void queryWeather() {
        Cursor c = mContext.getContentResolver().query(
                WeatherContract.WeatherColumns.CURRENT_WEATHER_URI, WEATHER_PROJECTION,
                null, null, null);
        if (c == null) {
            if(DEBUG) Log.e(TAG, "cursor was null for temperature, forcing weather update");
            //LockClock keeps track of the user settings (temp unit, search by geo location/city)
            //so we delegate the responsibility of handling a weather update to LockClock
            mContext.sendBroadcast(new Intent(ACTION_FORCE_WEATHER_UPDATE));
        } else {
            try {
                c.moveToFirst();
                double temp = c.getDouble(0);
                int reportedUnit = c.getInt(1);
                if (reportedUnit == CELSIUS && mWeatherUnit == FAHRENHEIT) {
                    temp = WeatherUtils.celsiusToFahrenheit(temp);
                } else if (reportedUnit == FAHRENHEIT && mWeatherUnit == CELSIUS) {
                    temp = WeatherUtils.fahrenheitToCelsius(temp);
                }

                mCachedInfo.temp = temp;
                mCachedInfo.tempUnit = mWeatherUnit;
                mCachedInfo.city = c.getString(0);
                mCachedInfo.condition = c.getString(1);
                mCachedInfo.conditionCode = c.getInt(2);
                mCachedInfo.conditionDrawable = getIcon(mCachedInfo.conditionCode);
            } finally {
                c.close();
            }
        }
    }

    private void fireCallback() {
        for (Callback callback : mCallbacks) {
            callback.onWeatherChanged(mCachedInfo);
        }
    }

    private class WeatherContentObserver extends ContentObserver {

        public WeatherContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                if (uri.compareTo(WeatherContract.WeatherColumns.CURRENT_WEATHER_URI) == 0) {
                    queryWeather();
                    fireCallback();
                } else if (uri.compareTo(mWeatherTempetarureUri) == 0) {
                    queryWeatherTempUnit();
                    fixCachedWeatherInfo();
                    fireCallback();
                } else {
                    super.onChange(selfChange, uri);
                }
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    }

    private void queryWeatherTempUnit() {
        try {
            mWeatherUnit = CMSettings.Global.getInt(mContext.getContentResolver(),
                    CMSettings.Global.WEATHER_TEMPERATURE_UNIT);
        } catch (CMSettings.CMSettingNotFoundException e) {
            //CMSettingsProvider should have taken care of setting a default value for this setting
            //so how is that we ended up here?? We need to set a valid temp unit anyway to keep
            //this going
            mWeatherUnit = WeatherContract.WeatherColumns.TempUnit.CELSIUS;
        }
    }

    private void fixCachedWeatherInfo() {
        if (mCachedInfo.tempUnit == CELSIUS && mWeatherUnit == FAHRENHEIT) {
            mCachedInfo.temp = WeatherUtils.celsiusToFahrenheit(mCachedInfo.temp);
            mCachedInfo.tempUnit = FAHRENHEIT;
        } else if (mCachedInfo.tempUnit == FAHRENHEIT && mWeatherUnit == CELSIUS) {
            mCachedInfo.temp = WeatherUtils.fahrenheitToCelsius(mCachedInfo.temp);
            mCachedInfo.tempUnit = CELSIUS;
        }
    }

    @Override
    public void updateWeather() {
        queryWeather();
        fireCallback();
    }
}
