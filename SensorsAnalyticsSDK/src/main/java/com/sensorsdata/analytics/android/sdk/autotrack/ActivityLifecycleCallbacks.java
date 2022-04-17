/*
 * Created by dengshiwei on 2021/07/29.
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

package com.sensorsdata.analytics.android.sdk.autotrack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.sdk.SessionRelatedManager;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.visual.HeatMapService;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ActivityLifecycleCallbacks implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks, SensorsDataExceptionHandler.SAExceptionListener {
    private static final String TAG = "SA.ActivityLifecycleCallbacks";
    private static final String EVENT_TIME = "event_time";
    private static final String EVENT_DURATION = "event_duration";
    private static final String LIB_VERSION = "$lib_version";
    private static final String APP_VERSION = "$app_version";
    private final SensorsDataAPI mSensorsDataInstance;
    private final Context mContext;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private boolean resumeFromBackground = false;
    private final DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private final JSONObject endDataProperty = new JSONObject();
    private JSONObject mDeepLinkProperty = new JSONObject();
    private int mStartActivityCount;
    private int mStartTimerCount;
    private long mStartTime;
    // $AppStart 事件的时间戳
    private final String APP_START_TIME = "app_start_time";
    // $AppEnd 事件属性
    private final String APP_END_DATA = "app_end_data";
    // App 是否重置标记位
    private final String APP_RESET_STATE = "app_reset_state";
    private final String TIME = "time";
    private final String ELAPSE_TIME = "elapse_time";
    private Handler mHandler;
    /* 兼容由于在魅族手机上退到后台后，线程会被休眠，导致 $AppEnd 无法触发，造成再次打开重复发送。*/
    private long messageReceiveTime = 0L;
    private final int MESSAGE_CODE_START = 100;
    private final int MESSAGE_CODE_STOP = 200;
    private final int MESSAGE_CODE_TIMER = 300;
    /**
     * 打点时间间隔：2000 毫秒
     */
    private static final int TIME_INTERVAL = 2000;
    private boolean mDataCollectState;

    private Set<Integer> hashSet = new HashSet<>();

    public ActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart,
                                      PersistentFirstDay firstDay, Context context) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mDbAdapter = DbAdapter.getInstance();
        this.mContext = context;
        mDataCollectState = SensorsDataAPI.getConfigOptions().isDataCollectEnable();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            buildScreenProperties(activity);
            if (mSensorsDataInstance.isAutoTrackEnabled() ) {
                JSONObject properties = new JSONObject();
                SensorsDataUtils.mergeJSONObject(activityProperty, properties);
                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                    }
                }
                JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
                mSensorsDataInstance.trackViewScreen(SensorsDataUtils.getScreenUrl(activity), eventProperties);
            }
        } catch (Throwable e) {
            SALog.i(TAG, e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (hasActivity(activity)) {
            sendActivityHandleMessage(MESSAGE_CODE_STOP);
            removeActivity(activity);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }


    private void initHandler() {
        try {
            HandlerThread handlerThread = new HandlerThread("SENSORS_DATA_THREAD");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    int code = msg.what;
                }
            };
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 发送处理 Activity 生命周期的 Message
     *
     * @param type 消息类型
     */
    private void sendActivityHandleMessage(int type) {
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putLong(TIME, System.currentTimeMillis());
        bundle.putLong(ELAPSE_TIME, SystemClock.elapsedRealtime());
        message.what = type;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
    private void buildScreenProperties(Activity activity) {
        activityProperty = AopUtil.buildTitleNoAutoTrackerProperties(activity);
        SensorsDataUtils.mergeJSONObject(activityProperty, endDataProperty);
        if (!SensorsDataAPI.getConfigOptions().isDisableSDK()) {
            // 合并渠道信息到 $AppStart 事件中
            if (mDeepLinkProperty == null) {
                mDeepLinkProperty = new JSONObject();
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (SensorsDataAPI.getConfigOptions().isMultiProcessFlush()) {
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }

        // 注意这里要重置为 0，对于跨进程的情况，如果子进程崩溃，主进程但是没崩溃，造成统计个数异常，所以要重置为 0。
        DbAdapter.getInstance().commitActivityCount(0);
    }

    void addActivity(Activity activity) {
        if (activity != null) {
            hashSet.add(activity.hashCode());
        }
    }

    boolean hasActivity(Activity activity) {
        if (activity != null) {
            return hashSet.contains(activity.hashCode());
        }
        return false;
    }

    void removeActivity(Activity activity) {
        if (activity != null) {
            hashSet.remove(activity.hashCode());
        }
    }
}
