package org.md2k.motionsense.device.sensor;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by aungkon on 10/30/2017.
 */

public class HeartRate {
    private double[] data;
    private double resolution = .025;
    private int dft_length;
    private static int[] two_powers = {0,2,4,8,16,32,64,128,256,512,1024,2048,4096};
    private double[] channel1;
    private double[] channel2;
    private double[] channel3;
    private HashMap<Integer,HashMap<Integer,Double>> absfft = new HashMap<>();

    public HeartRate(ArrayList<DataQualityLed.Sample> datainput, int sampling_frequency){

        channel1 = new double[datainput.size()];
        channel2 = new double[datainput.size()];
        channel3 = new double[datainput.size()];
        for(int i=0;i<datainput.size();i++){
            channel1[i] = datainput.get(i).data[0];
            channel2[i] = datainput.get(i).data[1];
            channel3[i] = datainput.get(i).data[2];
        }
        this.dft_length = find_dft_length(sampling_frequency);
        data = new double[channel1.length];
        Complex[] signal = new Complex[this.dft_length];
        for(int i=0;i<3;i++){
            if(i==0){
                data = new Bandpass(channel1).output;
                Log.d("data_quality_led",""+data.length);
            }else if(i==1){
                data = new Bandpass(channel2).output;
            }else{
                data = new Bandpass(channel3).output;
            }
            normalize();
            signal = computeFFTInput();
            FFT f = new FFT();
            signal = f.fft(signal);
            absfft.put(i,new HashMap<>());
            for(int j=0;j<signal.length;j++){
                absfft.get(i).put(j,signal[j].abs());
            }
        }
        Log.d("data_quality_led","computing heart rate");
    }

    private int find_dft_length(int sampling_frequency){
        int temp = (int) ((double)sampling_frequency/resolution);
        for(int i=0;i<this.two_powers.length-1;i++){
            if(temp==this.two_powers[i]){
                return this.two_powers[i];
            }else{
                if(temp<this.two_powers[i+1] && temp>this.two_powers[i]){
                    return this.two_powers[i+1];
                }
            }

        }
        return 1024;
    }

    private Complex[] computeFFTInput(){
        Complex[] input;
        if(dft_length>data.length){
            input = new Complex[dft_length];
        }else{
            input = new Complex[dft_length];
        }
        for(int i=0;i<input.length;i++){
            if(i>data.length-1){
                input[i] = new Complex(0,0);
            }else{
                input[i] = new Complex(data[i],0);
            }
        }
        return input;
    }

    private void normalize(){
        double mean = getmean(data);
        System.out.println(mean);
        for(int i=0;i<data.length;i++){
            data[i] = data[i] - mean;
        }
    }
    private double getmean(double[] s){
        double sum = 0;
        for (int i=0;i<s.length;i++){
            sum = sum + s[i];
        }
        double mean = sum/s.length;
        return mean;
    }
}
