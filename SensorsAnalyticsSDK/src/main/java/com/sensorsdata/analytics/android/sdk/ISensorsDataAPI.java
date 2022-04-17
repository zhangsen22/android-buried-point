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
import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.internal.api.IFragmentAPI;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ISensorsDataAPI extends IFragmentAPI {
    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    JSONObject getPresetProperties();

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
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     * @param isRequestRemoteConfig 是否立即请求当前 serverUrl 的远程配置
     */
    void setServerUrl(String serverUrl, boolean isRequestRemoteConfig);

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
     * 设置 App 切换到后台与下次事件的事件间隔
     * 默认值为 30*1000 毫秒
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30s
     */
    int getSessionIntervalTime();

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     * 默认值为 30*1000 毫秒
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @param sessionIntervalTime int
     */
    void setSessionIntervalTime(int sessionIntervalTime);

    /**
     * 打开 SDK 自动追踪
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    void enableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的部分事件
     *
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    void disableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的某个事件
     *
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    void disableAutoTrack(SensorsDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 是否开启 AutoTrack
     *
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    boolean isAutoTrackEnabled();

    /**
     * 指定哪些 activity 不被 AutoTrack
     * 指定 activity 的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList activity 列表
     */
    void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activitiesList List
     */
    void resumeAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 指定某个 activity 不被 AutoTrack
     *
     * @param activity Activity
     */
    void ignoreAutoTrackActivity(Class<?> activity);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activity Class
     */
    void resumeAutoTrackActivity(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     *
     * @param activity Activity
     * @return Activity 是否被采集
     */
    boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    boolean isActivityAutoTrackAppClickIgnored(Class<?> activity);

    /**
     * 判断某个 AutoTrackEventType 是否被忽略
     *
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType);

    /**
     * 判断某个 AutoTrackEventType 是否被忽略
     *
     * @param autoTrackEventType SensorsAnalyticsAutoTrackEventType 中的事件类型，可通过 '|' 进行连接传递
     * @return true 被忽略; false 没有被忽略
     */
    boolean isAutoTrackEventTypeIgnored(int autoTrackEventType);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(View view, String viewID);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(android.app.Dialog view, String viewID);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(Object view, String viewID);

    /**
     * 设置 View 所属 Activity
     *
     * @param view 要设置的 View
     * @param activity Activity View 所属 Activity
     */
    void setViewActivity(View view, Activity activity);

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view 要设置的 View
     * @param fragmentName String View 所属 Fragment 名称
     */
    void setViewFragmentName(View view, String fragmentName);

    /**
     * 忽略 View
     *
     * @param view 要忽略的 View
     */
    void ignoreView(View view);

    /**
     * 忽略View
     *
     * @param view View
     * @param ignore 是否忽略
     */
    void ignoreView(View view, boolean ignore);

    /**
     * 设置View属性
     *
     * @param view 要设置的 View
     * @param properties 要设置的 View 的属性
     */
    void setViewProperties(View view, JSONObject properties);

    /**
     * 获取忽略采集 View 的集合
     *
     * @return 忽略采集的 View 集合
     */
    List<Class> getIgnoredViewTypeList();

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    void ignoreViewType(Class viewType);

    /**
     * activity 是否开启了可视化全埋点
     *
     * @param activity activity 类的对象
     * @return true 代表 activity 开启了可视化全埋点，false 代表 activity 关闭了可视化全埋点
     */
    boolean isVisualizedAutoTrackActivity(Class<?> activity);

    /**
     * 开启某个 activity 的可视化全埋点
     *
     * @param activity activity 类的对象
     */
    void addVisualizedAutoTrackActivity(Class<?> activity);

    /**
     * 开启多个 activity 的可视化全埋点
     *
     * @param activitiesList activity 类的对象集合
     */
    void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 是否开启可视化全埋点
     *
     * @return true 代表开启了可视化全埋点， false 代表关闭了可视化全埋点
     */
    boolean isVisualizedAutoTrackEnabled();

    /**
     * activity 是否开启了点击图
     *
     * @param activity activity 类的对象
     * @return true 代表 activity 开启了点击图， false 代表 activity 关闭了点击图
     */
    boolean isHeatMapActivity(Class<?> activity);

    /**
     * 开启某个 activity 的点击图
     *
     * @param activity activity 类的对象
     */
    void addHeatMapActivity(Class<?> activity);

    /**
     * 开启多个 activity 的点击图
     *
     * @param activitiesList activity 类的对象集合
     */
    void addHeatMapActivities(List<Class<?>> activitiesList);

    /**
     * 是否开启点击图
     *
     * @return true 代表开启了点击图，false 代表关闭了点击图
     */
    boolean isHeatMapEnabled();

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
     * 获取 LastScreenUrl
     *
     * @return String
     */
    String getLastScreenUrl();

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
    void clearReferrerWhenAppEnd();

    /**
     * 清除 LastScreenUrl
     */
    void clearLastScreenUrl();

    /**
     * 获取 LastScreenTrackProperties
     *
     * @return JSONObject
     */
    JSONObject getLastScreenTrackProperties();

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
     * 首次设置用户的一个或多个 Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    void profileSetOnce(JSONObject properties);

    /**
     * 首次设置用户的一个 Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link Boolean}, {@link org.json.JSONArray}
     */
    void profileSetOnce(String property, Object value);

    /**
     * 给一个或多个数值类型的 Profile 增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为 0
     *
     * @param properties 一个或多个属性集合
     */
    void profileIncrement(Map<String, ? extends Number> properties);

    /**
     * 给一个数值类型的 Profile 增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为 0
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为 {@link Number}
     */
    void profileIncrement(String property, Number value);

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
     * 删除用户所有 Profile
     */
    void profileDelete();

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
     * 开启/关闭采集屏幕方向
     *
     * @param enable true：开启 false：关闭
     */
    void enableTrackScreenOrientation(boolean enable);

    /**
     * 恢复采集屏幕方向
     */
    void resumeTrackScreenOrientation();

    /**
     * 暂停采集屏幕方向
     */
    void stopTrackScreenOrientation();

    /**
     * 获取当前屏幕方向
     *
     * @return portrait:竖屏 landscape:横屏
     */
    String getScreenOrientation();

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

    /**
     * 停止事件采集，注意不要随便调用，调用后会造成数据丢失。
     */
    void stopTrackThread();

    /**
     * 开启事件采集
     */
    void startTrackThread();

    /**
     * 开启数据采集
     */
    void enableDataCollect();
}
