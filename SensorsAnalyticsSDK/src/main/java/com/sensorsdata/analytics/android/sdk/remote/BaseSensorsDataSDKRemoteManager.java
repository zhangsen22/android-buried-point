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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class BaseSensorsDataSDKRemoteManager {

    protected static final String TAG = "SA.SensorsDataSDKRemoteConfigBase";
    protected Context mContext;
    protected SAConfigOptions mSAConfigOptions;
    protected SensorsDataEncrypt mSensorsDataEncrypt;

    protected static SensorsDataSDKRemoteConfig mSDKRemoteConfig;
    protected SensorsDataAPI mSensorsDataAPI;

    protected BaseSensorsDataSDKRemoteManager(SensorsDataAPI sensorsDataAPI) {
        this.mSensorsDataAPI = sensorsDataAPI;
        this.mContext = sensorsDataAPI.getContext();
        this.mSAConfigOptions = sensorsDataAPI.getConfigOptions();
        this.mSensorsDataEncrypt = sensorsDataAPI.getSensorsDataEncrypt();
    }

    public abstract void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV);

    public abstract void resetPullSDKConfigTimer();

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    public abstract void applySDKConfigFromCache();

    protected abstract void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig);

    public boolean ignoreEvent(String eventName) {
        if (mSDKRemoteConfig != null && mSDKRemoteConfig.getEventBlacklist() != null) {
            try {
                int size = mSDKRemoteConfig.getEventBlacklist().length();
                for (int i = 0; i < size; i++) {
                    if (eventName.equals(mSDKRemoteConfig.getEventBlacklist().get(i))) {
                        SALog.i(TAG, "remote config: " + eventName + " is ignored by remote config");
                        return true;
                    }
                }
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
        return false;
    }

    /**
     * 将 json 格式的字符串转成 SensorsDataSDKRemoteConfig 对象，并处理默认值
     *
     * @param config String
     * @return SensorsDataSDKRemoteConfig
     */
    protected SensorsDataSDKRemoteConfig toSDKRemoteConfig(String config) {
        SensorsDataSDKRemoteConfig sdkRemoteConfig = new SensorsDataSDKRemoteConfig();
        try {
            if (!TextUtils.isEmpty(config)) {
                JSONObject jsonObject = new JSONObject(config);
                sdkRemoteConfig.setOldVersion(jsonObject.optString("v"));

                String configs = jsonObject.optString("configs");
                SecreteKey secreteKey = new SecreteKey("", -1, "", "");
                if (!TextUtils.isEmpty(configs)) {
                    JSONObject configObject = new JSONObject(configs);
                    sdkRemoteConfig.setDisableDebugMode(configObject.optBoolean("disableDebugMode", false));
                    sdkRemoteConfig.setDisableSDK(configObject.optBoolean("disableSDK", false));
                    sdkRemoteConfig.setAutoTrackMode(configObject.optInt("autoTrackMode", -1));
                    sdkRemoteConfig.setEventBlacklist(configObject.optJSONArray("event_blacklist"));
                    sdkRemoteConfig.setNewVersion(configObject.optString("nv", ""));
                    sdkRemoteConfig.setEffectMode(configObject.optInt("effect_mode", 0));
                    if (mSAConfigOptions.getEncryptors() != null && !mSAConfigOptions.getEncryptors().isEmpty()) {
                        JSONObject keyObject = configObject.optJSONObject("key_v2");
                        if (keyObject != null) {
                            String[] types = keyObject.optString("type").split("\\+");
                            if (types.length == 2) {
                                String asymmetricType = types[0];
                                String symmetricType = types[1];
                                for (SAEncryptListener encryptListener : mSAConfigOptions.getEncryptors()) {
                                    if (asymmetricType.equals(encryptListener.asymmetricEncryptType())
                                            && symmetricType.equals(encryptListener.symmetricEncryptType())) {
                                        secreteKey.key = keyObject.optString("public_key");
                                        secreteKey.version = keyObject.optInt("pkv");
                                        secreteKey.asymmetricEncryptType = asymmetricType;
                                        secreteKey.symmetricEncryptType = symmetricType;
                                    }
                                }
                            }
                        }
                        if (TextUtils.isEmpty(secreteKey.key)) {
                            parseSecreteKey(configObject.optJSONObject("key"), secreteKey);
                        }
                        sdkRemoteConfig.setSecretKey(secreteKey);
                    }
                } else {
                    //默认配置
                    sdkRemoteConfig.setDisableDebugMode(false);
                    sdkRemoteConfig.setDisableSDK(false);
                    sdkRemoteConfig.setAutoTrackMode(-1);
                    sdkRemoteConfig.setSecretKey(secreteKey);
                    sdkRemoteConfig.setEventBlacklist(new JSONArray());
                    sdkRemoteConfig.setNewVersion("");
                    sdkRemoteConfig.setEffectMode(0);
                }
                return sdkRemoteConfig;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return sdkRemoteConfig;
    }

    private void parseSecreteKey(JSONObject keyObject, SecreteKey secreteKey) {
        if (keyObject != null) {
            try {
                if (keyObject.has("key_ec") && SensorsDataEncrypt.isECEncrypt()) {
                    String key_ec = keyObject.optString("key_ec");
                    if (!TextUtils.isEmpty(key_ec)) {
                        keyObject = new JSONObject(key_ec);
                    }
                }

                secreteKey.key = keyObject.optString("public_key");
                secreteKey.symmetricEncryptType = "AES";
                if (keyObject.has("type")) {
                    String type = keyObject.optString("type");
                    secreteKey.key = type + ":" + secreteKey.key;
                    secreteKey.asymmetricEncryptType = type;
                } else {
                    secreteKey.asymmetricEncryptType = "RSA";
                }
                secreteKey.version = keyObject.optInt("pkv");
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    public static boolean isSDKDisabledByRemote() {
        if (mSDKRemoteConfig == null) {
            return false;
        }
        return mSDKRemoteConfig.isDisableSDK();
    }



    public enum RandomTimeType {
        RandomTimeTypeWrite, // 创建分散请求时间
        RandomTimeTypeClean, // 移除分散请求时间
        RandomTimeTypeNone    // 不处理分散请求时间
    }
}
