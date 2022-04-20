/*
 * Created by dengshiwei on 2019/03/11.
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

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.plugin.encrypt.StorePlugin;
import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

public final class SAConfigOptions extends AbstractSAConfigOptions implements Cloneable {

    /**
     * 是否设置打印日志
     */
    boolean mInvokeLog;

    /**
     * 私有构造函数
     */
    private SAConfigOptions() {
    }

    /**
     * 获取 SAOptionsConfig 实例
     *
     * @param serverUrl，数据上报服务器地址
     */
    public SAConfigOptions(String serverUrl) {
        this.mServerUrl = serverUrl;
    }

    /**
     * 设置数据上报地址
     *
     * @param serverUrl，数据上报地址
     * @return SAOptionsConfig
     */
    public SAConfigOptions setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    /**
     * 设置是否开启 AppCrash 采集，默认是关闭的
     *
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableTrackAppCrash() {
        this.mEnableTrackAppCrash = true;
        return this;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     * @return SAOptionsConfig
     */
    public SAConfigOptions setFlushBulkSize(int flushBulkSize) {
        this.mFlushBulkSize = Math.max(50, flushBulkSize);
        return this;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     * @return SAOptionsConfig
     */
    public SAConfigOptions setMaxCacheSize(long maxCacheSize) {
        this.mMaxCacheSize = Math.max(16 * 1024 * 1024, maxCacheSize);
        return this;
    }

    /**
     * 是否打印日志
     *
     * @param enableLog 是否开启打印日志
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableLog(boolean enableLog) {
        this.mLogEnabled = enableLog;
        this.mInvokeLog = true;
        return this;
    }

    /**
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return SAOptionsConfig
     */
    public SAConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
        return this;
    }

    /**
     * 是否开启加密
     *
     * @param enableEncrypt 是否开启加密
     * @return SAConfigOptions
     */
    public SAConfigOptions enableEncrypt(boolean enableEncrypt) {
        this.mEnableEncrypt = enableEncrypt;
        return this;
    }

    /**
     * 密钥回调监听
     *
     * @param persistentSecretKey 密钥回调监听
     * @return SAConfigOptions
     */
    public SAConfigOptions persistentSecretKey(IPersistentSecretKey persistentSecretKey) {
        mPersistentSecretKey = persistentSecretKey;
        return this;
    }

    /**
     * 禁用数据采集
     *
     * @return SAConfigOptions
     */
    @Deprecated
    public SAConfigOptions disableDataCollect() {
        this.isDataCollectEnable = false;
        return this;
    }

    /**
     * 设置 SSLSocketFactory，HTTPS 请求连接时需要使用
     *
     * @param SSLSocketFactory 证书
     * @return SAConfigOptions
     */
    public SAConfigOptions setSSLSocketFactory(SSLSocketFactory SSLSocketFactory) {
        this.mSSLSocketFactory = SSLSocketFactory;
        return this;
    }

    /**
     * 是否开启页面停留时长
     *
     * @param isTrackPageLeave 是否开启页面停留时长
     * @return SAConfigOptions
     */
    @Deprecated
    public SAConfigOptions enableTrackPageLeave(boolean isTrackPageLeave) {
        return enableTrackPageLeave(isTrackPageLeave, false);
    }

    /**
     * 是否开启页面停留时长
     *
     * @param isTrackPageLeave 是否开启页面停留时长
     * @param isTrackFragmentPageLeave 是否采集 Fragment 页面停留时长，需开启页面停留时长采集
     * @return SAConfigOptions
     */
    public SAConfigOptions enableTrackPageLeave(boolean isTrackPageLeave, boolean isTrackFragmentPageLeave) {
        this.mIsTrackPageLeave = isTrackPageLeave;
        this.mIsTrackFragmentPageLeave = isTrackFragmentPageLeave;
        return this;
    }

    /**
     * 注册自定义加密插件
     *
     * @param encryptListener 自定义加密实现接口
     * @return SAConfigOptions
     */
    public SAConfigOptions registerEncryptor(SAEncryptListener encryptListener) {
        if (encryptListener == null
                || TextUtils.isEmpty(encryptListener.asymmetricEncryptType())
                || TextUtils.isEmpty(encryptListener.symmetricEncryptType())) {
            return this;
        }
        if (!mEncryptors.contains(encryptListener)) {
            mEncryptors.add(0, encryptListener);
        }
        return this;
    }

    /**
     * 注册自定义插件，适用于 SP 加密
     *
     * @param plugin 自定义插件
     * @return SAConfigOptions
     */
    public SAConfigOptions registerStorePlugin(StorePlugin plugin) {
        if (mStorePlugins == null) {
            mStorePlugins = new ArrayList<>();
        }
        mStorePlugins.add(plugin);
        return this;
    }

    /**
     * 将 $device_id 修改为 $anonymization_id
     *
     * @return SAConfigOptions
     */
    public SAConfigOptions disableDeviceId() {
        this.mDisableDeviceId = true;
        return this;
    }

    @Override
    protected SAConfigOptions clone() {
        SAConfigOptions copyObject = this;
        try {
            copyObject = (SAConfigOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            SALog.printStackTrace(e);
        }
        return copyObject;
    }

}