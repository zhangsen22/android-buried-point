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
    int mFlushBulkSize = 100;

    /**
     * 是否开启打印日志
     */
    boolean mLogEnabled;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = SensorsNetworkType.TYPE_3G | SensorsNetworkType.TYPE_4G | SensorsNetworkType.TYPE_WIFI | SensorsNetworkType.TYPE_5G;

}