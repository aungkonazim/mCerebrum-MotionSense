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
    private static int[] two_powers = {0,2,4,8,16,32,64,128,1024,2048,4096};
    private double[] channel1;
    private double[] channel2;
    private double[] channel3;
    private HashMap<Integer,HashMap<Integer,Double>> absfft = new HashMap<>();
    private static int initial_check = 15; //finds the spectral frequency range first
    private static int iteration = 1;
    private static int ch_num;
    private static int N_prev;
    private int sampling_frequency;

    public HeartRate(ArrayList<DataQualityLed.Sample> datainput, int sampling_frequency){
        this.sampling_frequency = sampling_frequency;
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
        if(this.iteration<this.initial_check){
            iteration++;
            computeHRForInitialValues();
        }else if(this.iteration == initial_check+1){

        }else {

        }
    }

    private void computeHRForInitialValues(){
        ArrayList<Integer> N_range = new ArrayList<>();
        for(int i=(int)((.6*1000)/this.sampling_frequency)-1;i<(int)((3*1000)/this.sampling_frequency);i++){
            N_range.add(i);
        }
        double[] N_cur = new double[3];
        double[] val_max = new double[3];
        for(int i=0;i<3;i++){
            N_cur[i] = N_range.get(0) + find_index_of_max_arraylist(N_range,i);
            val_max[i] = absfft.get(i).get((int)N_cur[i]);
        }
        this.ch_num = find_index_of_max_double(val_max);
        double hr = (60*this.sampling_frequency*N_cur[this.ch_num])/this.dft_length;
        this.N_prev = (int)N_cur[this.ch_num];
        Log.d("data_quality_led","heart rate = "+hr);

    }
    private int find_index_of_max_double(double[] val_max){
        int max_index = 0;
        double max_value = val_max[0];
        for(int i=0;i<val_max.length;i++){
            if(val_max[i]> max_value){
                max_value = val_max[i];
                max_index = i;
            }
        }
        return max_index;
    }

    private int find_index_of_max_arraylist(ArrayList<Integer> N_range,int channel){
        int max_index = 0;
        double max_value = absfft.get(channel).get(N_range.get(0));
        for(int i=0;i<N_range.size();i++){
             if(absfft.get(channel).get(N_range.get(i))> max_value){
                 max_value = absfft.get(channel).get((int)N_range.get(i));
                 max_index = i;
             }
        }
        return max_index;
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
