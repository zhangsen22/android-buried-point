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
import android.view.View;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISensorsDataAPI{

    /**
     * 获取当前 serverUrl
     *
     * @return 当前 serverUrl
     */
    String getServerUrl();

    /**
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     */
    void setServerUrl(String serverUrl);

    /**
     * 设置是否开启 log
     *
     * @param enable boolean
     */
    void enableLog(boolean enable);

    /**
     * 获取本地缓存上限制
     *
     * @return 字节
     */
    long getMaxCacheSize();

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     */
    void setMaxCacheSize(long maxCacheSize);

    /**
     * 是否是开启 debug 模式
     *
     * @return true：是，false：不是
     */
    boolean isDebugMode();

    /**
     * 是否请求网络，默认是 true
     *
     * @return 是否请求网络
     */
    boolean isNetworkRequestEnable();

    /**
     * 设置是否允许请求网络，默认是 true
     *
     * @param isRequest boolean
     */
    void enableNetworkRequest(boolean isRequest);

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、5G、WI-FI 环境下都会尝试 flush
     *
     * @param networkType int 网络类型
     */
    void setFlushNetworkPolicy(int networkType);

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     * 默认值为 15 * 1000 毫秒
     * 在每次调用 track、signUp 以及 profileSet 等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * 1. 是否是 WIFI/3G/4G 网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存 20MB 数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    int getFlushInterval();

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    void setFlushInterval(int flushInterval);

    /**
     * 返回本地缓存日志的最大条目数
     * 默认值为 100 条
     * 在每次调用 track、signUp 以及 profileSet 等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * 1. 是否是 WIFI/3G/4G 网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存 32MB 数据。
     *
     * @return 返回本地缓存日志的最大条目数
     */
    int getFlushBulkSize();

    /**
     * 设置本地缓存日志的最大条目数，最小 50 条
     *
     * @param flushBulkSize 缓存数目
     */
    void setFlushBulkSize(int flushBulkSize);

    /**
     * 调用 track 接口，追踪一个带有属性的事件
     *
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    void track(String eventName, JSONObject properties);

    /**
     * 与 {@link #track(String, JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    void track(String eventName);

    /**
     * Track 进入页面事件 ($AppViewScreen)，该接口需要在 properties 中手动设置 $screen_name 和 $title 属性。
     *
     * @param url String
     * @param properties JSONObject
     */
    void trackViewScreen(String url, JSONObject properties);

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     *
     * @param activity activity Activity，当前 Activity
     */
    void trackViewScreen(Activity activity);

    /**
     * Track  Fragment 进入页面事件 ($AppViewScreen)
     *
     * @param fragment Fragment
     */
    void trackViewScreen(Object fragment);

    /**
     * Track 控件点击事件 ($AppClick)
     *
     * @param view View
     */
    void trackViewAppClick(View view);

    /**
     * Track 控件点击事件 ($AppClick)
     *
     * @param view View
     * @param properties 事件属性
     */
    void trackViewAppClick(View view, JSONObject properties);

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flush();

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flushSync();

    /**
     * 以轮询形式将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flushScheduled();

    /**
     * 设置 track 事件回调
     *
     * @param trackEventCallBack track 事件回调接口
     */
    void setTrackEventCallBack(SensorsDataTrackEventCallBack trackEventCallBack);

    /**
     * 设置用户的一个或多个 Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    void profileSet(JSONObject properties);

    /**
     * 设置用户的一个 Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link Boolean}, {@link org.json.JSONArray}
     */
    void profileSet(String property, Object value);

    /**
     * 给一个列表类型的 Profile 增加一个元素
     *
     * @param property 属性名称
     * @param value 新增的元素
     */
    void profileAppend(String property, String value);

    /**
     * 给一个列表类型的 Profile 增加一个或多个元素
     *
     * @param property 属性名称
     * @param values 新增的元素集合
     */
    void profileAppend(String property, Set<String> values);

    /**
     * 删除用户的一个 Profile
     *
     * @param property 属性名称
     */
    void profileUnset(String property);
    /**
     * 更新 GPS 位置信息
     *
     * @param latitude 纬度
     * @param longitude 经度
     */
    void setGPSLocation(double latitude, double longitude);

    /**
     * 更新 GPS 位置信息及对应坐标系
     *
     * @param latitude 纬度
     * @param longitude 经度
     * @param coordinate 坐标系，坐标系类型请参照 {@link SensorsDataGPSLocation.CoordinateType}
     */
    void setGPSLocation(double latitude, double longitude, final String coordinate);

    /**
     * 清除 GPS 位置信息
     */
    void clearGPSLocation();

    /**
     * 删除本地缓存的全部事件
     */
    void deleteAll();

    /**
     * 设置 item
     *
     * @param itemType item 类型
     * @param itemId item ID
     * @param properties item 相关属性
     */
    void itemSet(String itemType, String itemId, JSONObject properties);

    /**
     * 删除 item
     *
     * @param itemType item 类型
     * @param itemId item ID
     */
    void itemDelete(String itemType, String itemId);

}
