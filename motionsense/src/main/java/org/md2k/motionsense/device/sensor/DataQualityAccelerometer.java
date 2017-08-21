package org.md2k.motionsense.device.sensor;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.motionsense.ApplicationWithBluetooth;
import org.md2k.motionsense.ServiceMotionSense;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.DATA_QUALITY;

import java.util.ArrayList;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
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
public class DataQualityAccelerometer extends Sensor{
    public final static double MINIMUM_EXPECTED_SAMPLES = 3 * (0.33) * 10.33;  //33% of a 3 second window with 10.33 sampling frequency
    public final static float MAGNITUDE_VARIANCE_THRESHOLD = (float) 0.0025;   //this threshold comes from the data we collect by placing the wrist sensor on table. It compares with the wrist accelerometer on-body from participant #11 (smoking pilot study)

    private static final String TAG = DataQualityAccelerometer.class.getSimpleName();
    private ACLQualityCalculation aclQualityCalculation = new ACLQualityCalculation();;

    private ArrayList<Double> samples;

    public DataQualityAccelerometer(DataSource dataSource) {
        super(dataSource);
        samples = new ArrayList<>();
        aclQualityCalculation = new ACLQualityCalculation();
    }

    public synchronized int getStatus() {
        try {
            int status;
            int size = samples.size();
            double samps[] = new double[size];
            for (int i = 0; i < size; i++)
                samps[i] = samples.get(i);
            samples.clear();
            status = currentQuality(samps);
            Log.d(TAG, "MOTION_SENSE_ACL: " + status);
            return status;
        }catch (Exception e){
            Log.d(TAG, "MOTION_SENSE_ACL: exception");
            return DATA_QUALITY.GOOD;
        }
    }
    public synchronized void add(double sample) {

        samples.add(sample);
    }

    public void insert(DataTypeInt dataTypeInt){
        try {
            DataKitAPI.getInstance(ApplicationWithBluetooth.getAppContext()).insert(dataSourceClient, dataTypeInt);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(ApplicationWithBluetooth.getAppContext()).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
        }
    }

    private double getMean(double[] data) {
        double sum = 0.0;
        for (double a : data)
            sum += a;
        return sum / data.length;
    }

    private double getVariance(double[] data) {
        double mean = getMean(data);
        double temp = 0;
        for (double a : data)
            temp += (mean - a) * (mean - a);
        return temp / data.length;
    }

    private double getStdDev(double[] data) {
        return Math.sqrt(getVariance(data));
    }

    private int currentQuality(double[] x) {       //just receive x axis, in fact it should work with any single axis.
        int len_x = x.length;
        if (len_x == 0) return DATA_QUALITY.BAND_OFF;

//		if(len_x<MINIMUM_EXPECTED_SAMPLES)
//			return DATA_QUALITY.BAND_OFF;

        double sd =getStdDev(x);
        Log.d(TAG, "MOTION_SENSE stdDev" + sd);

        if (sd < MAGNITUDE_VARIANCE_THRESHOLD)
            return DATA_QUALITY.NOT_WORN;

        return DATA_QUALITY.GOOD;
    }
}