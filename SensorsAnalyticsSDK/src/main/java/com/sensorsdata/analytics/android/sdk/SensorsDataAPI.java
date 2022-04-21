/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.autotrack.ActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.FragmentViewScreenCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.aop.FragmentTrackHelper;
import com.sensorsdata.analytics.android.sdk.data.PFDbManager;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.EventType;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sensors Analytics SDK
 */
public class SensorsDataAPI implements ISensorsDataAPI {
    // SDK 版本，此属性插件会进行访问，谨慎修改
    static final String VERSION = BuildConfig.SDK_VERSION;
    /**
     * 插件版本号，插件会用到此属性，请谨慎修改
     */
    static String ANDROID_PLUGIN_VERSION = "";

    protected static final String TAG = "SA.SensorsDataAPI";

    // Maps each token to a singleton SensorsDataAPI instance
    static boolean mIsMainProcess = false;
    protected static SAConfigOptions mSAConfigOptions;
    protected Context mContext;
    protected ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    protected AnalyticsMessages mMessages;
    /* SensorsAnalytics 地址 */
    protected String mServerUrl;
    protected String mOriginServerUrl;
    /* SDK 配置是否初始化 */
    protected boolean mSDKConfigInit;
    /* Debug 模式选项 */
    protected SensorsDataAPI.DebugMode mDebugMode = SensorsDataAPI.DebugMode.DEBUG_OFF;
    /* 当前页面的 Title */
    protected String mCurrentScreenTitle;
    /* 是否请求网络 */
    protected boolean mEnableNetworkRequest = true;
    // Session 时长
    protected TrackTaskManager mTrackTaskManager;
    protected TrackTaskManagerThread mTrackTaskManagerThread;
    protected SimpleDateFormat mIsFirstDayDateFormat;


    private final Map<String, Object> mProperties = new HashMap<>();

    //private
    SensorsDataAPI() {

    }

    private static class SingletonInstance{
        private final static SensorsDataAPI S = new SensorsDataAPI();
    }
    // 3 返回对象
    public static SensorsDataAPI getInstance() {
        return SingletonInstance.S;
    }

    /**
     * 初始化神策 SDK
     *
     * @param context App 的 Context
     * @param saConfigOptions SDK 的配置项
     */
    public void buildConfig(Context context, SAConfigOptions saConfigOptions) {
        if (context == null || saConfigOptions == null) {
            throw new NullPointerException("Context、SAConfigOptions 不可以为 null");
        }
        this.mContext = context;

        setDebugMode(DebugMode.DEBUG_OFF);
        final String packageName = context.getApplicationContext().getPackageName();
        try {
            mSAConfigOptions = saConfigOptions.clone();
            mTrackTaskManager = TrackTaskManager.getInstance();
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            SensorsDataExceptionHandler.init();
            initSAConfig(mSAConfigOptions.mServerUrl, packageName);
            mMessages = AnalyticsMessages.getInstance(mContext, (SensorsDataAPI) this);

            registerLifecycleCallbacks();
            SensorsDataUtils.initUniAppStatus();
        } catch (Throwable ex) {
            SALog.d(TAG, ex.getMessage());
        }
        appendProperties(mProperties);

        if (!mSDKConfigInit) {
            applySAConfigOptions();
        }
    }

    @Override
    public void enableLog(boolean enable) {
        SALog.setEnableLog(enable);
    }

    @Override
    public void setFlushNetworkPolicy(int networkType) {
        mSAConfigOptions.setNetworkTypePolicy(networkType);
    }

    int getFlushNetworkPolicy() {
        return mSAConfigOptions.mNetworkTypePolicy;
    }

    @Override
    public int getFlushBulkSize() {
        return mSAConfigOptions.mFlushBulkSize;
    }

    @Override
    public void track(final String eventName, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    trackEvent(EventType.TRACK, eventName, cloneProperties);
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    @Deprecated
    public void trackViewScreen(final String url, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!TextUtils.isEmpty(url)) {// || cloneProperties != null
                            String currentUrl = url;
                            JSONObject trackProperties = new JSONObject();

                            if (cloneProperties != null) {
                                if (cloneProperties.has("$title")) {
                                    mCurrentScreenTitle = cloneProperties.getString("$title");
                                } else {
                                    mCurrentScreenTitle = null;
                                }
                                if (cloneProperties.has("$url")) {
                                    currentUrl = cloneProperties.optString("$url");
                                }
                            }
                            trackProperties.put("$url", currentUrl);
                            if (cloneProperties != null) {
                                SensorsDataUtils.mergeJSONObject(cloneProperties, trackProperties);
                            }
                            trackEvent(EventType.TRACK, "$AppViewScreen", trackProperties);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity == null) {
                        return;
                    }
                    JSONObject properties = AopUtil.buildTitleAndScreenName(activity);
                    trackViewScreen(SensorsDataUtils.getScreenUrl(activity), properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment)) &&
                !(appFragmentClass != null && appFragmentClass.isInstance(fragment)) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject properties = new JSONObject();
                    String screenName = fragment.getClass().getCanonicalName();

                    String title = null;

                    if (Build.VERSION.SDK_INT >= 11) {
                        Activity activity = null;
                        try {
                            Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                            if (getActivityMethod != null) {
                                activity = (Activity) getActivityMethod.invoke(fragment);
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                        if (activity != null) {
                            if (TextUtils.isEmpty(title)) {
                                title = SensorsDataUtils.getActivityTitle(activity);
                            }
                            screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                        }
                    }

                    if (!TextUtils.isEmpty(title)) {
                        properties.put(AopConstants.TITLE, title);
                    }
                    properties.put("$screen_name", screenName);
                    trackViewScreen(SensorsDataUtils.getScreenUrl(fragment), properties);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void flush() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mMessages.flush();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    @Override
    public void flushSync() {
        flush();
    }

    @Override
    public void deleteAll() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mMessages.deleteAll();
            }
        });
    }

    @Override
    public boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return mEnableNetworkRequest;
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {
        this.mEnableNetworkRequest = isRequest;
    }

    @Override
    public void setServerUrl(final String serverUrl) {
        try {
            mOriginServerUrl = serverUrl;
            if (TextUtils.isEmpty(serverUrl)) {
                mServerUrl = serverUrl;
                SALog.i(TAG, "Server url is null or empty.");
                return;
            }

            final Uri serverURI = Uri.parse(serverUrl);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    String hostServer = serverURI.getHost();
                    if (!TextUtils.isEmpty(hostServer) && hostServer.contains("_")) {
                        SALog.i(TAG, "Server url " + serverUrl + " contains '_' is not recommend，" +
                                "see details: https://en.wikipedia.org/wiki/Hostname");
                    }
                }
            });

            if (mDebugMode != DebugMode.DEBUG_OFF) {
                String uriPath = serverURI.getPath();
                if (TextUtils.isEmpty(uriPath)) {
                    return;
                }

                int pathPrefix = uriPath.lastIndexOf('/');
                if (pathPrefix != -1) {
                    String newPath = uriPath.substring(0, pathPrefix) + "/debug";
                    // 将 URI Path 中末尾的部分替换成 '/debug'
                    mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
                }
            } else {
                mServerUrl = serverUrl;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 不能动位置，因为 SF 反射获取使用
     *
     * @return ServerUrl
     */
    @Override
    public String getServerUrl() {
        return mServerUrl;
    }

    /**
     * 获取 SDK 的版本号
     *
     * @return SDK 的版本号
     */
    public String getSDKVersion() {
        return VERSION;
    }


    /**
     * Debug 模式，用于检验数据导入是否正确。该模式下，事件会逐条实时发送到 Sensors Analytics，并根据返回值检查
     * 数据导入是否正确。
     * Debug 模式的具体使用方式，请参考:
     * http://www.sensorsdata.cn/manual/debug_mode.html
     * Debug 模式有三种：
     * DEBUG_OFF - 关闭DEBUG模式
     * DEBUG_ONLY - 打开DEBUG模式，但该模式下发送的数据仅用于调试，不进行数据导入
     * DEBUG_AND_TRACK - 打开DEBUG模式，并将数据导入到SensorsAnalytics中
     */
    public enum DebugMode {
        DEBUG_OFF(false, false),
        DEBUG_ONLY(true, false),
        DEBUG_AND_TRACK(true, true);

        private final boolean debugMode;
        private final boolean debugWriteData;

        DebugMode(boolean debugMode, boolean debugWriteData) {
            this.debugMode = debugMode;
            this.debugWriteData = debugWriteData;
        }

        boolean isDebugMode() {
            return debugMode;
        }

        boolean isDebugWriteData() {
            return debugWriteData;
        }
    }


    /**
     * 网络类型
     */
    public final class NetworkType {
        public static final int TYPE_NONE = 0;//NULL
        public static final int TYPE_2G = 1;//2G
        public static final int TYPE_3G = 1 << 1;//3G
        public static final int TYPE_4G = 1 << 2;//4G
        public static final int TYPE_WIFI = 1 << 3;//WIFI
        public static final int TYPE_5G = 1 << 4;//5G
        public static final int TYPE_ALL = 0xFF;//ALL
    }

    public void appendProperties(Map<String, Object> properties) {
        String osVersion = DeviceUtils.getHarmonyOSVersion();
        if (!TextUtils.isEmpty(osVersion)) {
            properties.put("$os", "HarmonyOS");
            properties.put("$os_version", osVersion);
        } else {
            properties.put("$os", "Android");
            properties.put("$os_version", DeviceUtils.getOS());
        }

        properties.put("$lib", "Android");
        properties.put("$lib_version", SensorsDataAPI.getInstance().getSDKVersion());
        properties.put("$manufacturer", DeviceUtils.getManufacturer());
        properties.put("$model", DeviceUtils.getModel());
        properties.put("$brand", DeviceUtils.getBrand());
        properties.put("$app_version", AppInfoUtils.getAppVersionName(mContext));
        int[] size = DeviceUtils.getDeviceSize(mContext);
        properties.put("$screen_width", size[0]);
        properties.put("$screen_height", size[1]);

        String carrier = SensorsDataUtils.getCarrier(mContext);
        if (!TextUtils.isEmpty(carrier)) {
            properties.put("$carrier", carrier);
        }

        Integer zone_offset = TimeUtils.getZoneOffset();
        if (zone_offset != null) {
            properties.put("$timezone_offset", zone_offset);
        }

        properties.put("$app_id", AppInfoUtils.getProcessName(mContext));
        properties.put("$app_name", AppInfoUtils.getAppName(mContext));
        String mAndroidId = SensorsDataUtils.getAndroidID(mContext);
        properties.put("$device_id", mAndroidId);
        JSONArray libPluginVersion = getPluginVersion();
        if(libPluginVersion != null)
            properties.put("$lib_plugin_version", libPluginVersion);
        //$anonymization_id 防止用户自定义事件以及公共属性可能会加 $device_id 属性，导致覆盖 sdk 原始的 $device_id 属性值
        properties.put("$anonymization_id", "$anonymization_id");

    }

    public Context getContext() {
        return mContext;
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     */
    public void trackInternal(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    public SensorsDataAPI.DebugMode getDebugMode() {
        return mDebugMode;
    }

    public void setDebugMode(SensorsDataAPI.DebugMode debugMode) {
        mDebugMode = debugMode;
        if (debugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
            enableLog(false);
            SALog.setDebug(false);
            mServerUrl = mOriginServerUrl;
        } else {
            enableLog(true);
            SALog.setDebug(true);
            setServerUrl(mOriginServerUrl);
        }
    }

    /**
     * SDK 全埋点调用方法，支持可视化自定义属性
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    void trackAutoEvent(final String eventName, final JSONObject properties) {
        //添加 $lib_method 属性
        JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
        trackInternal(eventName, eventProperties);
    }

    protected boolean isFirstDay(long eventTime) {
        String firstDay = null;
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            String current = mIsFirstDayDateFormat.format(eventTime);
            return firstDay.equals(current);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return true;
    }

    protected void trackEvent(final EventType eventType, String eventName, final JSONObject properties) {
        try {

            if (eventType.isTrack()) {
                SADataHelper.assertEventName(eventName);
            }

            try {
                JSONObject sendProperties = new JSONObject();

                // 将属性插件的属性合并到 sendProperties
                sendProperties = SensorsDataUtils.mergeSuperJSONObject(
                        new JSONObject(mProperties),
                        sendProperties);

                if (eventType.isTrack()) {

                    // 当前网络状况
                    String networkType = NetworkUtils.networkType(mContext);
                    sendProperties.put("$wifi", "WIFI".equals(networkType));
                    sendProperties.put("$network_type", networkType);

                }

                trackEventInternal(eventType, eventName, properties, sendProperties);
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    protected void initSAConfig(String serverURL, String packageName) {
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mContext);
        if (mSAConfigOptions == null) {
            this.mSDKConfigInit = false;
            mSAConfigOptions = new SAConfigOptions();
        } else {
            this.mSDKConfigInit = true;
        }

        PFDbManager.getInstance(mContext, packageName);

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        } else {
            enableLog(configBundle.getBoolean("com.sensorsdata.analytics.android.EnableLogging",
                    this.mDebugMode != SensorsDataAPI.DebugMode.DEBUG_OFF));
        }

        setServerUrl(serverURL);
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        mIsMainProcess = AppInfoUtils.isMainProcess(mContext, configBundle);
    }

    protected void applySAConfigOptions() {
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        }
    }

    private void trackEventInternal(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties) throws JSONException, InvalidDataException {
        String libDetail = null;
        String lib_version = VERSION;
        long eventTime = System.currentTimeMillis();
        JSONObject libProperties = new JSONObject();
        if (null != properties) {
            try {
                if (properties.has("$lib_detail")) {
                    libDetail = properties.getString("$lib_detail");
                    properties.remove("$lib_detail");
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            SensorsDataUtils.mergeJSONObject(properties, sendProperties);
            if (eventType.isTrack()) {
                if ("autoTrack".equals(properties.optString("$lib_method"))) {
                    libProperties.put("$lib_method", "autoTrack");
                } else {
                    libProperties.put("$lib_method", "code");
                    sendProperties.put("$lib_method", "code");
                }
            } else {
                libProperties.put("$lib_method", "code");
            }
        } else {
            libProperties.put("$lib_method", "code");
            if (eventType.isTrack()) {
                sendProperties.put("$lib_method", "code");
            }
        }

        libProperties.put("$lib", "Android");
        libProperties.put("$lib_version", lib_version);

        final JSONObject dataObj = new JSONObject();

        try {
            SecureRandom random = new SecureRandom();
            dataObj.put("_track_id", random.nextInt());
        } catch (Exception e) {
            // ignore
        }

        dataObj.put("time", eventTime);
        dataObj.put("type", eventType.getEventType());
        try {
            if (sendProperties.has("$project")) {
                dataObj.put("project", sendProperties.optString("$project"));
                sendProperties.remove("$project");
            }

            if (sendProperties.has("$token")) {
                dataObj.put("token", sendProperties.optString("$token"));
                sendProperties.remove("$token");
            }

            if (sendProperties.has("$time")) {
                try {
                    Object timeDate = sendProperties.opt("$time");
                    if (timeDate instanceof Date) {
                        if (TimeUtils.isDateValid((Date) timeDate)) {
                            eventTime = ((Date) timeDate).getTime();
                            dataObj.put("time", eventTime);
                        }
                    }
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
                sendProperties.remove("$time");
            }

        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        dataObj.put("login_id", "login_id");

        dataObj.put("lib", libProperties);

        if (eventType == EventType.TRACK) {
            dataObj.put("event", eventName);
            //是否首日访问
            sendProperties.put("$is_first_day", isFirstDay(eventTime));
        }

        if (TextUtils.isEmpty(libDetail)) {
            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
            }
        }

        libProperties.put("$lib_detail", libDetail);

        SADataHelper.assertPropertyTypes(sendProperties);
        dataObj.put("properties", sendProperties);

        mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
        }
    }

    private JSONArray getPluginVersion() {
        try {
            if (!TextUtils.isEmpty(SensorsDataAPI.ANDROID_PLUGIN_VERSION)) {
                SALog.i(TAG, "android plugin version: " + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                JSONArray libPluginVersion = new JSONArray();
                libPluginVersion.put("android:" + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                return libPluginVersion;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 注册 ActivityLifecycleCallbacks
     */
    private void registerLifecycleCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) mContext.getApplicationContext();
                final SensorsDataActivityLifecycleCallbacks lifecycleCallbacks = new SensorsDataActivityLifecycleCallbacks();
                mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks((SensorsDataAPI) this);
                lifecycleCallbacks.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
                SensorsDataExceptionHandler.addExceptionListener(mActivityLifecycleCallbacks);
                FragmentTrackHelper.addFragmentCallbacks(new FragmentViewScreenCallbacks());

                /** 防止并发问题注册一定要在 {@link SensorsDataActivityLifecycleCallbacks#addActivityLifecycleCallbacks(SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks)} 之后执行 */
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}