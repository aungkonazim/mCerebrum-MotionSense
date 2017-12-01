package org.md2k.motionsense.device.sensor;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.core.data_format.DATA_QUALITY;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.ServiceMotionSense;

import java.util.ArrayList;
import java.util.Iterator;

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
public class DataQualityLed extends Sensor{
    public final static double MINIMUM_EXPECTED_SAMPLES = 3 * (0.33) * 10.33;  //33% of a 3 second window with 10.33 sampling frequency
    public final static float MAGNITUDE_VARIANCE_THRESHOLD = (float) 0.0025;   //this threshold comes from the data we collect by placing the wrist sensor on table. It compares with the wrist accelerometer on-body from participant #11 (smoking pilot study)

    private ArrayList<Sample> samples;

    public DataQualityLed(DataSource dataSource) {
        super(dataSource);
        samples = new ArrayList<>();
    }
    private boolean[] isGood3Sec(ArrayList<Sample> values){
        double[] sum = new double[]{0,0,0};
        boolean[] res = new boolean[6];
        int[] count  = new int[3];
        for(int i=0;i<values.size();i++){
            sum[0]+=values.get(i).data[0];
            sum[1]+=values.get(i).data[1];
            sum[2]+=values.get(i).data[2];
            if(values.get(i).data[0]<14000 || values.get(i).data[0]>170000){
                count[0]++;
            }
            if(values.get(i).data[1]<100000 || values.get(i).data[1]>230000){
                count[1]++;
            }
            if(values.get(i).data[2]<800 || values.get(i).data[2]>20000){
                count[2]++;
            }
        }
        res[0]= count[0] < (int)(.34*values.size());
        res[1]= count[1] < (int)(.34*values.size());
        res[2]= count[2] < (int)(.34*values.size());
//        Log.d("data_quality_led","last 3 quality="+res[0]+" "+res[1]+" "+res[2]);
        return res;
    }

    private int[] getMean(ArrayList<Sample> values){
        int[] sum = new int[3];
        for(int i=0;i<values.size();i++) {
            sum[0] += values.get(i).data[0];
            sum[1] += values.get(i).data[1];
            sum[2] += values.get(i).data[2];
        }
        for(int i=0;i<3;i++){
            sum[i] = (int)(sum[i]/values.size());
        }
        return sum;

    }
    private ArrayList<Sample> getLast3Sec(){
        long curTime=DateTime.getDateTime();
        ArrayList<Sample> l=new ArrayList<>();
        for(int i=0;i<samples.size();i++){
            if(curTime-samples.get(i).timestamp<=3000)
                l.add(samples.get(i));
        }
        return l;
    }

    double[] getSample(int index){
        double[] d=new double[samples.size()];
        for(int i=0;i<samples.size();i++){
            d[i]=samples.get(i).data[index];
        }
        return d;
    }
    public synchronized int getStatus() {
        try {
            long curTime=DateTime.getDateTime();
            Iterator<Sample> i = samples.iterator();
            while (i.hasNext()) {
                if(curTime-i.next().timestamp>=8000)
                i.remove();
            }

            ArrayList<Sample> last3Sec=getLast3Sec();
//            Log.d("data_quality_led","last 3="+last3Sec.size());
            if(last3Sec.size()==0){
//                Log.d("data_quality_led","bad because size is zero");
                return DATA_QUALITY.BAND_OFF;
            }

            boolean[] sec3mean=isGood3Sec(samples);
            if(!sec3mean[0] && !sec3mean[1] && !sec3mean[2]){
//                Log.d("data_quality_led","bad because range is conditions are not fulfilled");
                return DATA_QUALITY.NOT_WORN;
            }

            int[] mean = getMean(samples);

            if(mean[0]<10000 && mean[1]<10000 && mean[2]<10000){
//                Log.d("data_quality_led","bad because all means are less than 10000");
                return DATA_QUALITY.NOT_WORN;
            }

            boolean check = mean[0]>mean[2] && mean[1]>mean[0] && mean[1]>mean[2];
//            Log.d("data_quality_led_mean1",""+check);
            if(!check){
//                Log.d("data_quality_led","bad because chronology is not maintained");
                return DATA_QUALITY.BAND_LOOSE;
            }

            int diff;
            if(mean[0]>140000 || mean[0]<30000){
                diff = 11000;
            }else{
                diff =30000;
            }
            boolean check1 = mean[0]-mean[2]>diff && mean[1]-mean[0] >diff;
//            Log.d("data_quality_led_mean2",""+check1);
            if(!check1){
//                Log.d("data_quality_led","bad because difference is not maintained");
                return DATA_QUALITY.BAND_LOOSE;
            }

            if(sec3mean[0] && new Bandpass(getSample(0)).getResult()) {
                return DATA_QUALITY.GOOD;
            }
            if(sec3mean[1] && new Bandpass(getSample(1)).getResult()) {
                return DATA_QUALITY.GOOD;
            }
            if(sec3mean[2] && new Bandpass(getSample(2)).getResult()) {

                return DATA_QUALITY.GOOD;
            }

            return DATA_QUALITY.NOT_WORN;

        }catch (Exception e){
            return DATA_QUALITY.GOOD;
        }
    }
    public synchronized void add(double[] sample) {
        samples.add(new Sample(DateTime.getDateTime(), sample));
    }

    public void insert(DataTypeInt dataTypeInt){
        int[] intArray=new int[7];
        for(int i=0;i<7;i++) intArray[i]=0;
        int value=dataTypeInt.getSample();
        intArray[value]=3000;
        try {
            DataKitAPI.getInstance(MyApplication.getContext()).insert(dataSourceClient, dataTypeInt);
            DataKitAPI.getInstance(MyApplication.getContext()).setSummary(dataSourceClient, new DataTypeIntArray(dataTypeInt.getDateTime(), intArray));
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
        }
    }
    class Sample{
        double[] data;
        long timestamp;

        public Sample(long dateTime, double[] sample) {
            this.timestamp = dateTime;
            this.data=sample;
        }
    }
}