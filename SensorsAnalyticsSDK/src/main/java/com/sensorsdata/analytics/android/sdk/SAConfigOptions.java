package com.sensorsdata.analytics.android.sdk;

public final class SAConfigOptions extends AbstractSAConfigOptions implements Cloneable {

    /**
     * 获取 SAOptionsConfig 实例
     */
    public SAConfigOptions() {

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
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return SAOptionsConfig
     */
    public SAConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
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