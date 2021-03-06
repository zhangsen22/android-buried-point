/*
 * Created by chenru on 2019/06/20.
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

package com.sensorsdata.analytics.android.demo.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.databinding.ActivityClickBinding;
import com.sensorsdata.analytics.android.sdk.PropertyBuilder;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackAppViewScreenUrl;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEvent;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackViewOnClick;

import java.util.HashSet;
import java.util.Set;

@SensorsDataAutoTrackAppViewScreenUrl
public class ClickActivity extends BaseActivity{

    private ActivityClickBinding activityClickBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityClickBinding = DataBindingUtil.setContentView(this, R.layout.activity_click);
        this.setTitle("设置点击方式");
        initView();
    }

    private void initView() {
        type1();
        type2();
        type3();
        type5();
        type8();
        type10();
    }

    /**
     * 1. 手动开启浏览页面功能 activity / fragment
     */
    private void type1() {
        findViewById(R.id.track_view_screen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorsDataAPI.getInstance().trackViewScreen(ClickActivity.this);
            }
        });
    }

    /**
     * 2. 手动自定义一个事件并且带参数
     */
    private void type2() {
        findViewById(R.id.track_a_event).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorsDataAPI.getInstance().track("你好",
                        PropertyBuilder.newInstance()
                                .append("Pricevvv", 100)
                                .append("Namevvv", "Apple").toJSONObject());
            }
        });
    }

    /**
     * 3. 通过注解自定义一个事件并且支持带参数
     */
    @SensorsDataTrackEvent(eventName = "someEventName", properties = "{provider:测试}")
    private void type3() {

    }

    /**
     * 5. lambda 方式的点击事件。
     */
    @SensorsDataTrackViewOnClick
    private void type5() {
        findViewById(R.id.tv_click_5).setOnClickListener(v -> Toast.makeText(ClickActivity.this, "方式5(Lambda)", Toast.LENGTH_SHORT).show());
    }

    /**
     * 8. dataBinding 方式的点击事件。
     */
    private void type8() {
        BindingEntity bindingEntity = new BindingEntity("dataBinding点击");
        activityClickBinding.setData(bindingEntity);
        activityClickBinding.setClickListener(new View.OnClickListener() {
            @Override
            @SensorsDataTrackViewOnClick
            public void onClick(View v) {
                Toast.makeText(ClickActivity.this, "方式8(dataBinding)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 10. 上报crash
     */
    private void type10() {
        findViewById(R.id.crash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a = 1 / 0;
            }
        });
    }
}
