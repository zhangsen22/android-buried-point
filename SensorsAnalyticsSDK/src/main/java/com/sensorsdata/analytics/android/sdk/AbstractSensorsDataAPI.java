/*
 * Created by dengshiwei on 2020/10/20.
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.autotrack.ActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.ActivityPageLeaveCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.FragmentPageLeaveCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.FragmentViewScreenCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.aop.FragmentTrackHelper;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallation;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPresetPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.SensorsDataPropertyPluginManager;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SAContextManager;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.util.ToastUtil;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class AbstractSensorsDataAPI implements ISensorsDataAPI {
    protected static final String TAG = "SA.SensorsDataAPI";
    // SDK版本
    static final String VERSION = BuildConfig.SDK_VERSION;
    // Maps each token to a singleton SensorsDataAPI instance
    protected static final Map<Context, SensorsDataAPI> sInstanceMap = new HashMap<>();
    static boolean mIsMainProcess = false;
    static boolean SHOW_DEBUG_INFO_VIEW = true;
    protected static SensorsDataGPSLocation mGPSLocation;
    /* 远程配置 */
    protected static SAConfigOptions mSAConfigOptions;
    protected SAContextManager mSAContextManager;
    protected final Context mContext;
    protected ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    protected AnalyticsMessages mMessages;
    protected final PersistentSuperProperties mSuperProperties;
    protected final PersistentFirstStart mFirstStart;
    protected final PersistentFirstDay mFirstDay;
    protected final PersistentFirstTrackInstallation mFirstTrackInstallation;
    protected final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
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
    protected SensorsDataTrackEventCallBack mTrackEventCallBack;
    protected SAStoreManager mStoreManager;
    SensorsDataEncrypt mSensorsDataEncrypt;
    BaseSensorsDataSDKRemoteManager mRemoteManager;
    /**
     * 标记是否已经采集了带有插件版本号的事件
     */
    private boolean isTrackEventWithPluginVersion = false;

    public AbstractSensorsDataAPI(Context context, SAConfigOptions configOptions, SensorsDataAPI.DebugMode debugMode) {
        mContext = context;
        setDebugMode(debugMode);
        final String packageName = context.getApplicationContext().getPackageName();
        PersistentLoader.initLoader(context);
        mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(DbParams.PersistentName.SUPER_PROPERTIES);
        mFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent(DbParams.PersistentName.FIRST_START);
        mFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent(DbParams.PersistentName.FIRST_INSTALL);
        mFirstTrackInstallationWithCallback = (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent(DbParams.PersistentName.FIRST_INSTALL_CALLBACK);
        mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(DbParams.PersistentName.FIRST_DAY);
        try {
            mSAConfigOptions = configOptions.clone();
            mStoreManager = SAStoreManager.getInstance();
            mStoreManager.registerPlugins(mSAConfigOptions.getStorePlugins(), mContext);
            mStoreManager.upgrade();
            mTrackTaskManager = TrackTaskManager.getInstance();
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            SensorsDataExceptionHandler.init();
            initSAConfig(mSAConfigOptions.mServerUrl, packageName);
            mSAContextManager = new SAContextManager(mContext);
            mMessages = AnalyticsMessages.getInstance(mContext, (SensorsDataAPI) this);
            mRemoteManager = new SensorsDataRemoteManager((SensorsDataAPI) this);
            //先从缓存中读取 SDKConfig
            mRemoteManager.applySDKConfigFromCache();

            registerLifecycleCallbacks();
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, String.format(Locale.CHINA, "Initialized the instance of Sensors Analytics SDK with server"
                        + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mSAConfigOptions.mFlushInterval, debugMode));
            }
            SensorsDataUtils.initUniAppStatus();
        } catch (Throwable ex) {
            SALog.d(TAG, ex.getMessage());
        }
        registerDefaultPropertiesPlugin();
    }

    private void registerDefaultPropertiesPlugin() {
        SensorsDataPropertyPluginManager.getInstance().registerPropertyPlugin(new SAPresetPropertyPlugin(mContext));
    }

    protected AbstractSensorsDataAPI() {
        mContext = null;
        mMessages = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mSensorsDataEncrypt = null;
    }

    /**
     * 延迟初始化处理逻辑
     *
     * @param activity 延迟初始化 Activity 补充执行
     */
    protected void delayExecution(Activity activity) {
        if (mActivityLifecycleCallbacks != null) {
            mActivityLifecycleCallbacks.onActivityCreated(activity, null);   //延迟初始化处理唤起逻辑
            mActivityLifecycleCallbacks.onActivityStarted(activity);                 //延迟初始化补发应用启动逻辑
        }
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "SDK init success by：" + activity.getClass().getName());
        }
    }

    /**
     * 返回采集控制是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisabledByRemote() {
        boolean isSDKDisabled = SensorsDataRemoteManager.isSDKDisabledByRemote();
        if (isSDKDisabled) {
            SALog.i(TAG, "remote config: SDK is disabled");
        }
        return isSDKDisabled;
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(SAEventListener eventListener) {
        mSAContextManager.addEventListener(eventListener);
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(SAEventListener eventListener) {
        mSAContextManager.removeEventListener(eventListener);
    }

    public static SAConfigOptions getConfigOptions() {
        return mSAConfigOptions;
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
        trackInternal(eventName, properties, null);
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param viewNode ViewTree 中的 View 节点
     */
    public void trackInternal(final String eventName, final JSONObject properties, final ViewNode viewNode) {
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

    public BaseSensorsDataSDKRemoteManager getRemoteManager() {
        return mRemoteManager;
    }

    public void setRemoteManager(BaseSensorsDataSDKRemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    public SensorsDataEncrypt getSensorsDataEncrypt() {
        return mSensorsDataEncrypt;
    }

    /**
     * SDK 内部调用方法
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    public void trackAutoEvent(final String eventName, final JSONObject properties) {
        trackAutoEvent(eventName, properties, null);
    }

    /**
     * SDK 全埋点调用方法，支持可视化自定义属性
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    void trackAutoEvent(final String eventName, final JSONObject properties, final ViewNode viewNode) {
        //添加 $lib_method 属性
        JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
        trackInternal(eventName, eventProperties, viewNode);
    }

    public SAContextManager getSAContextManager() {
        return mSAContextManager;
    }

    void registerNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.registerNetworkListener(mContext);
            }
        });
    }

    void unregisterNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.unregisterNetworkListener(mContext);
            }
        });
    }

    protected void addTimeProperty(JSONObject jsonObject) {
        if (!jsonObject.has("$time")) {
            try {
                jsonObject.put("$time", new Date(System.currentTimeMillis()));
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
    }

    protected boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
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

    protected void trackItemEvent(String itemType, String itemId, String eventType, long time, JSONObject properties) {
        try {
            boolean isItemTypeValid = SADataHelper.assertPropertyKey(itemType);
            SADataHelper.assertPropertyTypes(properties);
            SADataHelper.assertItemId(itemId);
            // 禁用采集事件时，先计算基本信息存储到缓存中
            if (!mSAConfigOptions.isDataCollectEnable) {
                transformItemTaskQueue(itemType, itemId, eventType, time, properties);
                return;
            }

            String eventProject = null;
            if (properties != null && properties.has("$project")) {
                eventProject = (String) properties.get("$project");
                properties.remove("$project");
            }

            JSONObject libProperties = new JSONObject();
            libProperties.put("$lib", "Android");
            libProperties.put("$lib_version", VERSION);
            libProperties.put("$lib_method", "code");
            mSAContextManager.addKeyIfExist(libProperties, "$app_version");

            JSONObject superProperties = mSuperProperties.get();
            if (superProperties != null) {
                if (superProperties.has("$app_version")) {
                    libProperties.put("$app_version", superProperties.get("$app_version"));
                }
            }

            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                String libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
                if (!TextUtils.isEmpty(libDetail)) {
                    libProperties.put("$lib_detail", libDetail);
                }
            }

            JSONObject eventProperties = new JSONObject();
            if (isItemTypeValid) {
                eventProperties.put("item_type", itemType);
            }
            eventProperties.put("item_id", itemId);
            eventProperties.put("type", eventType);
            eventProperties.put("time", time);
            eventProperties.put("properties", TimeUtils.formatDate(properties));
            eventProperties.put("lib", libProperties);

            if (!TextUtils.isEmpty(eventProject)) {
                eventProperties.put("project", eventProject);
            }
            mMessages.enqueueEventMessage(eventType, eventProperties);
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventProperties.toString()));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    protected void trackEvent(final EventType eventType, String eventName, final JSONObject properties) {
        try {
            if (!TextUtils.isEmpty(eventName)) {
                if (eventName.endsWith("_SATimer") && eventName.length() > 45) {// Timer 计时交叉计算拼接的字符串长度 45
                    eventName = eventName.substring(0, eventName.length() - 45);
                }
            }

            if (eventType.isTrack()) {
                SADataHelper.assertEventName(eventName);
                //如果在线控制禁止了事件，则不触发
                if (!TextUtils.isEmpty(eventName) && mRemoteManager != null &&
                        mRemoteManager.ignoreEvent(eventName)) {
                    return;
                }
            }

            try {
                JSONObject sendProperties = new JSONObject();

                // 将属性插件的属性合并到 sendProperties
                sendProperties = SensorsDataUtils.mergeSuperJSONObject(
                        SensorsDataPropertyPluginManager.getInstance().properties(eventName, eventType, properties),
                        sendProperties);

                if (eventType.isTrack()) {

                    // 当前网络状况
                    String networkType = NetworkUtils.networkType(mContext);
                    sendProperties.put("$wifi", "WIFI".equals(networkType));
                    sendProperties.put("$network_type", networkType);

                    // GPS
                    try {
                        if (mGPSLocation != null) {
                            mGPSLocation.toJSON(sendProperties);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }

                } else {
                    if (!eventType.isProfile()) {
                        return;
                    }
                }

                // 禁用采集事件时，先计算基本信息存储到缓存中
                if (!mSAConfigOptions.isDataCollectEnable) {
                    if (SALog.isLogEnabled()) {
                        SALog.i(TAG, "track event, isDataCollectEnable = false, eventName = " + eventName + ",property = " + JSONUtils.formatJson(sendProperties.toString()));
                    }

                    transformEventTaskQueue(eventType, eventName, properties, sendProperties);
                    return;
                }
                trackEventInternal(eventType, eventName, properties, sendProperties);
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 在未同意合规时转换队列
     *
     * @param runnable 任务
     */
    public void transformTaskQueue(final Runnable runnable) {
        // 禁用采集事件时，先计算基本信息存储到缓存中
        if (!mSAConfigOptions.isDataCollectEnable) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mTrackTaskManager.transformTaskQueue(runnable);
                }
            });
            return;
        }

        mTrackTaskManager.addTrackEventTask(runnable);
    }

    protected void initSAConfig(String serverURL, String packageName) {
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mContext);
        if (mSAConfigOptions == null) {
            this.mSDKConfigInit = false;
            mSAConfigOptions = new SAConfigOptions(serverURL);
        } else {
            this.mSDKConfigInit = true;
        }

        if (mSAConfigOptions.mEnableEncrypt) {
            mSensorsDataEncrypt = new SensorsDataEncrypt(mContext, mSAConfigOptions.mPersistentSecretKey, mSAConfigOptions.getEncryptors());
        }

        DbAdapter.getInstance(mContext, packageName, mSensorsDataEncrypt);
        mTrackTaskManager.setDataCollectEnable(mSAConfigOptions.isDataCollectEnable);

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

        if (mSAConfigOptions.mFlushInterval == 0) {
            mSAConfigOptions.setFlushInterval(configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval",
                    15000));
        }

        if (mSAConfigOptions.mFlushBulkSize == 0) {
            mSAConfigOptions.setFlushBulkSize(configBundle.getInt("com.sensorsdata.analytics.android.FlushBulkSize",
                    100));
        }

        if (mSAConfigOptions.mMaxCacheSize == 0) {
            mSAConfigOptions.setMaxCacheSize(32 * 1024 * 1024L);
        }

        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean("com.sensorsdata.analytics.android.ShowDebugInfoView",
                true);

        if (mSAConfigOptions.isDataCollectEnable) {
            mIsMainProcess = AppInfoUtils.isMainProcess(mContext, configBundle);
        }
    }

    protected void applySAConfigOptions() {
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        }
    }

    private void showDebugModeWarning() {
        try {
            if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
                return;
            }
            if (TextUtils.isEmpty(mServerUrl)) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String info = null;
                    if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                        info = "现在您打开了 SensorsData SDK 的 'DEBUG_ONLY' 模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    } else if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        info = "现在您打开了神策 SensorsData SDK 的 'DEBUG_AND_TRACK' 模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    }
                    CharSequence appName = AppInfoUtils.getAppName(mContext);
                    if (!TextUtils.isEmpty(appName)) {
                        info = String.format(Locale.CHINA, "%s：%s", appName, info);
                    }
                    ToastUtil.showLong(mContext, info);
                }
            });
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * @param eventName 事件名
     * @param eventProperties 事件属性
     * @return 该事件是否入库
     */
    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mTrackEventCallBack != null) {
            SALog.i(TAG, "SDK have set trackEvent callBack");
            try {
                enterDb = mTrackEventCallBack.onTrackEvent(eventName, eventProperties);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (enterDb) {
                try {
                    Iterator<String> it = eventProperties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        Object value = eventProperties.opt(key);
                        if (value instanceof Date) {
                            eventProperties.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                        } else {
                            eventProperties.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return enterDb;
    }

    private void trackEventInternal(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties) throws JSONException, InvalidDataException {
        String libDetail = null;
        String lib_version = VERSION;
        String appEnd_app_version = null;
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
            try {
                // 单独处理 $AppStart 和 $AppEnd 的时间戳
                if ("$AppEnd".equals(eventName)) {
                    long appEndTime = properties.optLong("event_time");
                    // 退出时间戳不合法不使用，2000 为打点间隔时间戳
                    if (appEndTime > 2000) {
                        eventTime = appEndTime;
                    }
                    String appEnd_lib_version = properties.optString("$lib_version");
                    appEnd_app_version = properties.optString("$app_version");
                    if (!TextUtils.isEmpty(appEnd_lib_version)) {
                        lib_version = appEnd_lib_version;
                    } else {
                        properties.remove("$lib_version");
                    }

                    if (TextUtils.isEmpty(appEnd_app_version)) {
                        properties.remove("$app_version");
                    }

                    properties.remove("event_time");
                } else if ("$AppStart".equals(eventName)) {
                    long appStartTime = properties.optLong("event_time");
                    if (appStartTime > 0) {
                        eventTime = appStartTime;
                    }
                    properties.remove("event_time");
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
        if (TextUtils.isEmpty(appEnd_app_version)) {
            mSAContextManager.addKeyIfExist(libProperties, "$app_version");
        } else {
            libProperties.put("$app_version", appEnd_app_version);
        }

        //update lib $app_version from super properties
        JSONObject superProperties = mSuperProperties.get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

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

        if (eventType == EventType.TRACK || eventType == EventType.TRACK_ID_BIND || eventType == EventType.TRACK_ID_UNBIND) {
            dataObj.put("event", eventName);
            //是否首日访问
            sendProperties.put("$is_first_day", isFirstDay(eventTime));
        } else if (eventType == EventType.TRACK_SIGNUP) {
            dataObj.put("event", eventName);
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

        if (eventType.isTrack()) {
            if (mSAConfigOptions.isDisableDeviceId()) {
                //防止用户自定义事件以及公共属性可能会加 $device_id 属性，导致覆盖 sdk 原始的 $device_id 属性值
                if (sendProperties.has("$anonymization_id")) {//由于 profileSet 等类型事件没有 $device_id 属性，故加此判断
                    mSAContextManager.addKeyIfExist(sendProperties, "$anonymization_id");
                }
                sendProperties.remove("$device_id");
            } else {
                if (sendProperties.has("$device_id")) {
                    mSAContextManager.addKeyIfExist(sendProperties, "$device_id");
                }
                sendProperties.remove("$anonymization_id");
            }

            boolean isEnterDb = isEnterDb(eventName, sendProperties);
            if (!isEnterDb) {
                SALog.d(TAG, eventName + " event can not enter database");
                return;
            }
            if (!isTrackEventWithPluginVersion && !sendProperties.has("$lib_plugin_version")) {
                JSONArray libPluginVersion = getPluginVersion();
                if (libPluginVersion == null) {
                    isTrackEventWithPluginVersion = true;
                } else {
                    try {
                        sendProperties.put("$lib_plugin_version", libPluginVersion);
                        isTrackEventWithPluginVersion = true;
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
        }
        SADataHelper.assertPropertyTypes(sendProperties);
        dataObj.put("properties", sendProperties);

        try {
            if (mSAContextManager.getEventListenerList() != null && eventType.isTrack()) {
                for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                    eventListener.trackEvent(dataObj);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        try {
            if (eventType.isTrack()) {
                TrackMonitor.getInstance().callTrack(dataObj);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
        if ("$AppStart".equals(eventName)) {
            mSAContextManager.setAppStartSuccess(true);
        }
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
        }
    }

    /**
     * 如果没有授权时，需要将已执行的的缓存队列切换到真正的 TaskQueue 中
     */
    private void transformEventTaskQueue(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties) {
        try {
            if (!sendProperties.has("$time") && !("$AppStart".equals(eventName) || "$AppEnd".equals(eventName))) {
                sendProperties.put("$time", new Date(System.currentTimeMillis()));
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (eventType.isTrack()) {
                        JSONObject jsonObject = SensorsDataPropertyPluginManager.getInstance().properties(eventName, eventType, properties);
                        JSONUtils.mergeDistinctProperty(jsonObject, sendProperties);
                    }

                    if ("$SignUp".equals(eventName)) {// 如果是 "$SignUp" 则需要重新补上 originalId
                        trackEventInternal(eventType, eventName, properties, sendProperties);
                    } else {
                        trackEventInternal(eventType, eventName, properties, sendProperties);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private void transformItemTaskQueue(final String itemType, final String itemId, final String eventType, final long time, final JSONObject properties) {
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "track item, isDataCollectEnable = false, itemType = " + itemType + ",itemId = " + itemId);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    trackItemEvent(itemType, itemId, eventType, time, properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
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
                mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks((SensorsDataAPI) this, mFirstStart, mFirstDay, mContext);
                lifecycleCallbacks.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
                SensorsDataExceptionHandler.addExceptionListener(mActivityLifecycleCallbacks);
                FragmentTrackHelper.addFragmentCallbacks(new FragmentViewScreenCallbacks());

                if (mSAConfigOptions.isTrackPageLeave()) {
                    ActivityPageLeaveCallbacks pageLeaveCallbacks = new ActivityPageLeaveCallbacks(mSAConfigOptions.mIgnorePageLeave);
                    lifecycleCallbacks.addActivityLifecycleCallbacks(pageLeaveCallbacks);
                    SensorsDataExceptionHandler.addExceptionListener(pageLeaveCallbacks);
                    if (mSAConfigOptions.isTrackFragmentPageLeave()) {
                        FragmentPageLeaveCallbacks fragmentPageLeaveCallbacks = new FragmentPageLeaveCallbacks(mSAConfigOptions.mIgnorePageLeave);
                        FragmentTrackHelper.addFragmentCallbacks(fragmentPageLeaveCallbacks);
                        SensorsDataExceptionHandler.addExceptionListener(fragmentPageLeaveCallbacks);
                    }
                }

                /** 防止并发问题注册一定要在 {@link SensorsDataActivityLifecycleCallbacks#addActivityLifecycleCallbacks(SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks)} 之后执行 */
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 延迟初始化任务
     */
    protected void delayInitTask() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    registerNetworkListener();
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        });
    }

}
