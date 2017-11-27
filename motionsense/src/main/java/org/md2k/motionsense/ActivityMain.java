package org.md2k.motionsense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDouble;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeFloat;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.mcerebrum.core.access.appinfo.AppInfo;
import org.md2k.motionsense.device.DeviceManager;
import org.md2k.motionsense.device.sensor.Sensor;
import org.md2k.motionsense.plot.ActivityPlotChoice;

import java.util.HashMap;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class ActivityMain extends AppCompatActivity {
    public static final String INTENT_NAME = "motionsense_data";
    public static final int OPERATION_RUN = 0;
    public static final int OPERATION_SETTINGS = 1;
    public static final int OPERATION_PLOT = 2;
    public static final int OPERATION_START_FOREGROUND = 5;
    public static final int OPERATION_START_BACKGROUND = 3;
    public static final int OPERATION_STOP_BACKGROUND = 4;
    public static final String OPERATION = "operation";

    int operation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadCrashlytics();
        readIntent();
        if(!Permission.hasPermission(ActivityMain.this))
            checkRequirement();
        else
            load();
    }

    private void checkRequirement() {
        Intent intent = new Intent(this, ActivityPermission.class);
        startActivityForResult(intent, 1111);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1111) {
            if (resultCode != RESULT_OK) {
                finish();
            } else {
                load();
            }
        }
    }

    void readIntent() {
        if (getIntent().getExtras() != null) {
            operation = getIntent().getExtras().getInt(OPERATION, 0);
        } else operation = 0;
    }

    private void loadCrashlytics() {
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        Fabric.with(this, crashlyticsKit, new Crashlytics());
    }

    void load() {
        Intent intent;
        switch (operation) {
            case OPERATION_RUN:
                initializeUI();
                break;
            case OPERATION_START_BACKGROUND:
                intent = new Intent(ActivityMain.this, ServiceMotionSense.class);
                startService(intent);
                finish();
                break;
            case OPERATION_START_FOREGROUND:
                intent = new Intent(ActivityMain.this, ServiceMotionSense.class);
                startService(intent);
                break;
            case OPERATION_STOP_BACKGROUND:
                intent = new Intent(ActivityMain.this, ServiceMotionSense.class);
                stopService(intent);
                finish();
                break;
            case OPERATION_PLOT:
                intent = new Intent(this, ActivityPlotChoice.class);
                intent.putExtra("datasourcetype", getIntent().getStringExtra("datasourcetype"));
                startActivity(intent);
                finish();
                break;
            case OPERATION_SETTINGS:
                intent = new Intent(this, ActivitySettings.class);
                startActivity(intent);
                finish();
                break;
            default:
//                Toasty.error(getApplicationContext(), "Invalid argument. Operation = " + operation, Toast.LENGTH_SHORT).show();
                initializeUI();
        }
    }

    void initializeUI() {
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Button buttonService = (Button) findViewById(R.id.button_app_status);
        prepareTable();
        buttonService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServiceMotionSense.class);
                if (AppInfo.isServiceRunning(getBaseContext(), ServiceMotionSense.class.getName())) {
                    stopService(intent);
                } else {
                    operation=OPERATION_START_FOREGROUND;
//                    checkRequirement();
                    startService(intent);
                }
            }
        });

    }

    private HashMap<String, TextView> hashMapData = new HashMap<>();
    private Handler mHandler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                long time = AppInfo.serviceRunningTime(ActivityMain.this, Constants.SERVICE_NAME);
                if (time < 0) {
                    ((Button) findViewById(R.id.button_app_status)).setText("START");
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityMain.this, R.drawable.button_status_off));

                } else {
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityMain.this, R.drawable.button_status_on));
                    ((Button) findViewById(R.id.button_app_status)).setText(DateTime.convertTimestampToTimeStr(time));

                }
                mHandler.postDelayed(this, 1000);
            }
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTable(intent);
        }
    };

    private TableRow createDefaultRow() {
        TableRow row = new TableRow(this);
        TextView tvSensor = new TextView(this);
        tvSensor.setText("sensor");
        tvSensor.setTypeface(null, Typeface.BOLD);
        tvSensor.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvCount = new TextView(this);
        tvCount.setText("count");
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvFreq = new TextView(this);
        tvFreq.setText("freq.");
        tvFreq.setTypeface(null, Typeface.BOLD);
        tvFreq.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvSample = new TextView(this);
        tvSample.setText("samples");
        tvSample.setTypeface(null, Typeface.BOLD);
        tvSample.setTextColor(getResources().getColor(R.color.teal_A700));
        row.addView(tvSensor);
        row.addView(tvCount);
        row.addView(tvFreq);
        row.addView(tvSample);
        return row;
    }

    private void prepareTable() {
        DeviceManager deviceManager = new DeviceManager();
        TableLayout ll = (TableLayout) findViewById(R.id.tableLayout);
        ll.removeAllViews();
        ll.addView(createDefaultRow());
        for (int i = 0; i < deviceManager.size(); i++) {
            for (Sensor sensor : deviceManager.get(i).getSensors().values()) {
                String id = deviceManager.get(i).getId() + ":" + sensor.getDataSource().getType();
                if (sensor.getDataSource().getId() != null) id += sensor.getDataSource().getId();
                TableRow row = new TableRow(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(lp);
                TextView tvSensor = new TextView(this);
                tvSensor.setPadding(5, 0, 0, 0);
                String sname;
                if(sensor.getDataSource().getType().equalsIgnoreCase(DataSourceType.DATA_QUALITY)) {
                    sname = deviceManager.get(i).getType().toLowerCase() + "(" + deviceManager.get(i).getId().substring(0, 1) + ")\n" + sensor.getDataSource().getType().toLowerCase()+"("+sensor.getDataSource().getId().charAt(0)+")";
                }else
                    sname = deviceManager.get(i).getType().toLowerCase() + "(" + deviceManager.get(i).getId().substring(0, 1) + ")\n" + sensor.getDataSource().getType().toLowerCase();
                tvSensor.setText(sname);
                TextView tvCount = new TextView(this);
                tvCount.setText("0");
                hashMapData.put(id + "_count", tvCount);
                TextView tvFreq = new TextView(this);
                tvFreq.setText("0");
                hashMapData.put(id + "_freq", tvFreq);
                TextView tvSample = new TextView(this);
                tvSample.setText("0");
                hashMapData.put(id + "_sample", tvSample);
                row.addView(tvSensor);
                row.addView(tvCount);
                row.addView(tvFreq);
                row.addView(tvSample);
                row.setBackgroundResource(R.drawable.border);
                ll.addView(row);
            }
        }
    }

    private void updateTable(Intent intent) {
        try {
            String sampleStr = "";
            String key = intent.getStringExtra("key");
            String platformId = intent.getStringExtra("platformid");

            String id = platformId + ":" + key;
            int count = intent.getIntExtra("count", 0);
            if (hashMapData.containsKey(id + "_count"))
                hashMapData.get(id + "_count").setText(String.valueOf(count));

            double time = (intent.getLongExtra("timestamp", 0) - intent.getLongExtra("starttimestamp", 0)) / 1000.0;
            double freq = (double) count / time;
            if (hashMapData.containsKey(id + "_freq"))
                hashMapData.get(id + "_freq").setText(String.format(Locale.getDefault(), "%.1f", freq));


            DataType data = intent.getParcelableExtra("data");
            if (data instanceof DataTypeFloat) {
                sampleStr = String.format(Locale.getDefault(), "%.1f", ((DataTypeFloat) data).getSample());
            } else if (data instanceof DataTypeFloatArray) {
                float[] sample = ((DataTypeFloatArray) data).getSample();
                for (int i = 0; i < sample.length; i++) {
                    if (i != 0) sampleStr += ",";
                    if (i % 3 == 0 && i != 0) sampleStr += "\n";
                    sampleStr = sampleStr + String.format(Locale.getDefault(), "%.1f", sample[i]);
                }
            } else if (data instanceof DataTypeDouble) {
                sampleStr = String.format(Locale.getDefault(), "%.1f", ((DataTypeDouble) data).getSample());
            } else if (data instanceof DataTypeDoubleArray) {
                double[] sample = ((DataTypeDoubleArray) data).getSample();
                for (int i = 0; i < sample.length; i++) {
                    if (i != 0) sampleStr += ",";
                    if (i % 3 == 0 && i != 0) sampleStr += "\n";
                    sampleStr = sampleStr + String.format(Locale.getDefault(), "%.1f", sample[i]);
                }
            } else if (data instanceof DataTypeInt) {
                sampleStr = String.format(Locale.getDefault(), "%d", ((DataTypeInt) data).getSample());
            } else if (data instanceof DataTypeIntArray) {
                int[] sample = ((DataTypeIntArray) data).getSample();
                for (int i = 0; i < sample.length; i++) {
                    if (i != 0) sampleStr += ",";
                    if (i % 3 == 0 && i != 0) sampleStr += "\n";
                    sampleStr = sampleStr + String.format(Locale.getDefault(), "%d", sample[i]);
                }
            }
            if (hashMapData.containsKey(id + "_sample"))
                hashMapData.get(id + "_sample").setText(sampleStr);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_NAME));
        mHandler.post(runnable);
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(runnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                break;
            case R.id.action_settings:
                intent = new Intent(this, ActivitySettings.class);
                startActivity(intent);
                break;
            case R.id.action_plot:
                intent = new Intent(this, ActivityPlotChoice.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
