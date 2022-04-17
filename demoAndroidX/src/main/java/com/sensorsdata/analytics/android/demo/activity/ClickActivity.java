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
import com.sensorsdata.analytics.android.demo.entity.BindingEntity;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackAppViewScreenUrl;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEvent;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackViewOnClick;

@SensorsDataAutoTrackAppViewScreenUrl
public class ClickActivity extends BaseActivity{

    private ActivityClickBinding activityClickBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_click);
        activityClickBinding = DataBindingUtil.setContentView(this, R.layout.activity_click);
        this.setTitle("设置点击方式");
        initView();
    }

    private void initView() {
        type3();
        type5();
        type8();
    }

    /**
     * 3. View.OnClickListener 的实现类方式。
     */
    @SensorsDataTrackEvent(eventName = "someEventName", properties = "{provider:测试}")
    private void type3() {

    }

    /**
     * 5. lambda 方式。
     */
    @SensorsDataTrackViewOnClick
    private void type5() {
        findViewById(R.id.tv_click_5).setOnClickListener(v -> Toast.makeText(ClickActivity.this, "方式5(Lambda)", Toast.LENGTH_SHORT).show());
    }

    /**
     * 8. dataBinding 方式。
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
}
