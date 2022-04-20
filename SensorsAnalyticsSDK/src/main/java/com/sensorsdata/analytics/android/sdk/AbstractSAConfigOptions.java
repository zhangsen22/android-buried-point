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

import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;

import java.util.ArrayList;
import java.util.List;


/**
 * SDK 配置抽象类
 */
abstract class AbstractSAConfigOptions {

    /**
     * 数据上报服务器地址
     */
    String mServerUrl;

    /**
     * 是否开启 TrackAppCrash
     */
    boolean mEnableTrackAppCrash;

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
     * 是否开启加密
     */
    boolean mEnableEncrypt = false;

    /**
     * 密钥存储相关接口
     */
    IPersistentSecretKey mPersistentSecretKey;

    /**
     * 自定义加密实现接口
     */
    List<SAEncryptListener> mEncryptors = new ArrayList<>();

    /**
     * 开启采集页面停留时长
     */
    protected boolean mIsTrackPageLeave = false;

    /**
     * 是否采集 Fragment 页面停留时长
     */
    protected boolean mIsTrackFragmentPageLeave = false;

    /**
     * 自定义加密器
     */
    List<SAEncryptListener> mEncryptListeners;

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

}