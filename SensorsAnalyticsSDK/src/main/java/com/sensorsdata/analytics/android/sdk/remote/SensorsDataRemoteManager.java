/*
 * Created by yuejianzhong on 2020/11/04.
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

package com.sensorsdata.analytics.android.sdk.remote;

import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;

import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * SDK 初始化及线上使用时，采集控制管理类
 */
public class SensorsDataRemoteManager extends BaseSensorsDataSDKRemoteManager {
    private static final String SHARED_PREF_REQUEST_TIME = "sensorsdata.request.time";
    private static final String SHARED_PREF_REQUEST_TIME_RANDOM = "sensorsdata.request.time.random";
    private static final String TAG = "SA.SensorsDataRemoteManager";


    private final SAStoreManager mStorageManager;
    private volatile boolean mIsInit = true;

    public SensorsDataRemoteManager(
            SensorsDataAPI sensorsDataAPI) {
        super(sensorsDataAPI);
        mStorageManager = SAStoreManager.getInstance();
        SALog.i(TAG, "Construct a SensorsDataRemoteManager");
    }

    /**
     * 清除远程控制随机时间的本地缓存
     */
    private void cleanRemoteRequestRandomTime() {
        mStorageManager.remove(SHARED_PREF_REQUEST_TIME);
        mStorageManager.remove(SHARED_PREF_REQUEST_TIME_RANDOM);
    }

    @Override
    public void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV) {
        if (mSensorsDataAPI != null && !mSensorsDataAPI.isNetworkRequestEnable()) {
            SALog.i(TAG, "Close network request");
            return;
        }

        if (mDisableDefaultRemoteConfig) {
            SALog.i(TAG, "disableDefaultRemoteConfig is true");
            return;
        }

        switch (randomTimeType) {
            case RandomTimeTypeClean:
                cleanRemoteRequestRandomTime();
                break;
            default:
                break;
        }

    }

    @Override
    public void resetPullSDKConfigTimer() {

    }

    /**
     * 更新 SensorsDataSDKRemoteConfig
     *
     * @param sdkRemoteConfig SensorsDataSDKRemoteConfig 在线控制 SDK 的配置
     */
    @Override
    protected void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            //版本号不一致时，才会返回数据，此时上报事件
            JSONObject eventProperties = new JSONObject();
            String remoteConfigString = sdkRemoteConfig.toJson().toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            SensorsDataAPI.sharedInstance().trackInternal("$AppRemoteConfigChanged", eventProperties);
            SensorsDataAPI.sharedInstance().flush();
            DbAdapter.getInstance().commitRemoteConfig(remoteConfigString);
            SALog.i(TAG, "Save remote data");
            //值为 1 时，表示在线控制立即生效
            if (1 == sdkRemoteConfig.getEffectMode()) {
                applySDKConfigFromCache();
                SALog.i(TAG, "The remote configuration takes effect immediately");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    @Override
    public void applySDKConfigFromCache() {
        try {
            String remoteConfig;
            if (mIsInit) {
                remoteConfig = DbAdapter.getInstance().getRemoteConfigFromLocal();
                mIsInit = false;
            } else {
                remoteConfig = DbAdapter.getInstance().getRemoteConfig();
            }
            SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(remoteConfig);
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, "Cache remote config is " + sdkRemoteConfig.toString());
            }
            if (mSensorsDataAPI != null) {
                //关闭 debug 模式
                if (sdkRemoteConfig.isDisableDebugMode()) {
                    mSensorsDataAPI.setDebugMode(SensorsDataAPI.DebugMode.DEBUG_OFF);
                    SALog.i(TAG, "Set DebugOff Mode");
                }

                if (sdkRemoteConfig.isDisableSDK()) {
                    try {
                        mSensorsDataAPI.flush();
                        SALog.i(TAG, "DisableSDK is true");
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
