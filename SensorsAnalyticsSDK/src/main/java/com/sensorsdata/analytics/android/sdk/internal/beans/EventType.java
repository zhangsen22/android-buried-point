/*
 * Created by dengshiwei on 2021/03/26.
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

package com.sensorsdata.analytics.android.sdk.internal.beans;

public enum EventType {
    TRACK("track", true, false),
    TRACK_ID_BIND("track_id_bind", true, false),
    TRACK_ID_UNBIND("track_id_unbind", true, false),
    DEFAULT("default", false, false),

    /**
     * 特殊枚举对象用于标记所有事件类型，不可用于触发事件设置类型
     */
    ALL("all", false, false);

    private String eventType;
    private boolean track;
    private boolean profile;

    EventType(String eventType, boolean isTrack, boolean isProfile) {
        this.eventType = eventType;
        this.track = isTrack;
        this.profile = isProfile;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isTrack() {
        return track;
    }

    public boolean isProfile() {
        return profile;
    }
}
