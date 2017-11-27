package org.md2k.motionsense.configuration;

import android.content.Context;
import android.os.Environment;

import org.md2k.datakitapi.source.AbstractObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.mcerebrum.commons.storage.Storage;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.Constants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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
public class Configuration {
    public static final String CONFIG_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mCerebrum/org.md2k.motionsense/";
    public static final String DEFAULT_CONFIG_FILENAME = "default_config.json";
    public static final String CONFIG_FILENAME = "config.json";

    public static ArrayList<DataSource> read(String directory, String fileName) {
        try {
            return Storage.readJsonArrayList(directory + fileName, DataSource.class);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static ArrayList<DataSource> readMetaData() {
        try {
            return Storage.readJsonArrayFromAsset(MyApplication.getContext(), Constants.FILENAME_ASSET_METADATA, DataSource.class);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static boolean isConfigured() {
        ArrayList<DataSource> dataSources;
        dataSources = read(CONFIG_DIRECTORY, CONFIG_FILENAME);
        if (dataSources == null || dataSources.size() == 0) return false;
        return true;
    }

    public static void write(String directory, String fileName, ArrayList<DataSource> dataSources) throws IOException {
        Storage.writeJsonArray(directory + fileName, dataSources);
    }

    private static int getPlatformNo(ArrayList<DataSource> dataSources) {
        boolean flag;
        ArrayList<String> d = new ArrayList<>();
        for (int i = 0; i < dataSources.size(); i++) {
            String platformId = dataSources.get(i).getPlatform().getId();
            flag = false;
            for (int j = 0; j < d.size(); j++)
                if (d.get(j).equalsIgnoreCase(platformId)) {
                    flag = true;
                    break;
                }
            if (!flag)
                d.add(platformId);
        }
        return d.size();
    }

    public static boolean isEqualDefault() {
        ArrayList<DataSource> dataSources;
        ArrayList<DataSource> dataSourcesDefault;
        dataSources = read(CONFIG_DIRECTORY, CONFIG_FILENAME);
        dataSourcesDefault = read(CONFIG_DIRECTORY, DEFAULT_CONFIG_FILENAME);
        if (dataSourcesDefault == null) return true;
        if (dataSources == null) return false;
        if (getPlatformNo(dataSources) != getPlatformNo(dataSourcesDefault)) return false;
        return true;
    }
}
