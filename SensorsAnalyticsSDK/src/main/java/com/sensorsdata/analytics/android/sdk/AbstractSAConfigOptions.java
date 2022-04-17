/*
 * Created by dengshiwei on 2019/04/18.
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

import com.sensorsdata.analytics.android.sdk.plugin.encrypt.StorePlugin;
import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

/**
 * SDK 配置抽象类
 */
abstract class AbstractSAConfigOptions {
    /**
     * 请求配置地址，默认从 ServerUrl 解析
     */
    public String mRemoteConfigUrl;

    /**
     * 远程配置请求最小间隔时长，单位：小时，默认 24
     */
    public int mMinRequestInterval = 24;

    /**
     * 远程配置请求最大间隔时长，单位：小时，默认 48
     */
    public int mMaxRequestInterval = 48;

    /**
     * 禁用随机时间请求远程配置
     */
    public boolean mDisableRandomTimeRequestRemoteConfig;

    /**
     * 设置 SSLSocketFactory
     */
    public SSLSocketFactory mSSLSocketFactory;

    /**
     * 数据上报服务器地址
     */
    String mServerUrl;

    /**
     * AutoTrack 类型
     */
    int mAutoTrackEventType;

    /**
     * 是否开启 TrackAppCrash
     */
    boolean mEnableTrackAppCrash;

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     */
    int mFlushInterval;

    /**
     * 本地缓存日志的最大条目数
     */
    int mFlushBulkSize;

    /**
     * 本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     */
    long mMaxCacheSize = 32 * 1024 * 1024L;

    /**
     * 是否开启打印日志
     */
    boolean mLogEnabled;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = SensorsNetworkType.TYPE_3G | SensorsNetworkType.TYPE_4G | SensorsNetworkType.TYPE_WIFI | SensorsNetworkType.TYPE_5G;

    /**
     * 是否子进程上报数据
     */
    boolean isSubProcessFlushData = false;

    /**
     * 是否开启加密
     */
    boolean mEnableEncrypt = false;

    /**
     * 密钥存储相关接口
     */
    IPersistentSecretKey mPersistentSecretKey;

    /**
     * 关闭数据采集，默认开启数据采集
     */
    boolean isDataCollectEnable = true;

    /**
     * 是否关闭 SDK
     */
    boolean isDisableSDK = false;

    /**
     * 是开启 session 采集开关
     */
    boolean mEnableSession = false;

    /**
     * 自定义加密实现接口
     */
    List<SAEncryptListener> mEncryptors = new ArrayList<>();

    /**
     * 自定义加密插件
     */
    List<StorePlugin> mStorePlugins;

    /**
     * 开启采集页面停留时长
     */
    protected boolean mIsTrackPageLeave = false;

    /**
     * 是否采集 Fragment 页面停留时长
     */
    protected boolean mIsTrackFragmentPageLeave = false;

    /**
     * 忽略页面停留时长采集 List
     */
    List<Class<?>> mIgnorePageLeave;

    /**
     * 自定义加密器
     */
    List<SAEncryptListener> mEncryptListeners;

    /**
     * 是否不采集 $device_id 属性
     */
    boolean mDisableDeviceId = false;

    /**
     * 是否开启数据采集
     *
     * @return true 开启，false 未开启
     */
    public boolean isDataCollectEnable() {
        return isDataCollectEnable;
    }

    /**
     * 是否允许多进程上报数据
     *
     * @return true 开启，false 未开启
     */
    public boolean isMultiProcessFlush() {
        return isSubProcessFlushData;
    }

    /**
     * 是否开启页面停留时长采集
     *
     * @return true 开启，false 未开启
     */
    public boolean isTrackPageLeave() {
        return mIsTrackPageLeave;
    }

    /**
     * 是否开启页面停留时长采集
     *
     * @return true 开启，false 未开启
     */
    public boolean isTrackFragmentPageLeave() {
        return mIsTrackPageLeave && mIsTrackFragmentPageLeave;
    }

    /**
     * 获取注册的加密插件列表
     *
     * @return 注册的加密插件列表
     */
    public List<SAEncryptListener> getEncryptors() {
        return mEncryptors;
    }

    /**
     * 是否禁止 SDK
     *
     * @return true 禁止了 SDK，false 未禁止
     */
    public boolean isDisableSDK() {
        return this.isDisableSDK;
    }

    /**
     * 是否开启 session_id 的采集
     *
     * @return true 开启，false 关闭
     */
    public boolean isEnableSession() {
        return this.mEnableSession;
    }


    public List<StorePlugin> getStorePlugins() {
        return mStorePlugins;
    }

    /**
     * 是否将 $device_id 修改为 $anonymization_id
     *
     * @return false 不替换、true 替换
     */
    public boolean isDisableDeviceId() {
        return this.mDisableDeviceId;
    }

    /**
     * 广告渠道请求 url
     * 目前为 deferred deepLink 请求 url
     */
    String mCustomADChannelUrl;

    public String getCustomADChannelUrl(){
        return mCustomADChannelUrl;
    }
}