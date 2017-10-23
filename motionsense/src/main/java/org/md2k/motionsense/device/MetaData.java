package org.md2k.motionsense.device;
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

import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.motionsense.configuration.Configuration;

import java.util.ArrayList;

public class MetaData {
    private ArrayList<DataSource> metaData;
    MetaData(){
        metaData = Configuration.readMetaData();
    }
    public DataSource getDataSource(String dataSourceType, String dataSourceId, String platformType) {
        for(int i=0;i<metaData.size();i++){
            if(!metaData.get(i).getType().equals(dataSourceType)) continue;
            if(!metaData.get(i).getPlatform().getType().equals(platformType)) continue;
            if(dataSourceId==null && metaData.get(i).getId()==null) return metaData.get(i);
            if(dataSourceId!=null && metaData.get(i).getId()!=null && dataSourceId.equals(metaData.get(i).getId())) return metaData.get(i);
        }
        return null;
    }

    ArrayList<DataSource> getDataSources(String type) {
        ArrayList<DataSource> dataSources=new ArrayList<>();
        for(int i=0;i<metaData.size();i++){
            if(metaData.get(i).getPlatform().getType().equals(type)){
                dataSources.add(metaData.get(i));
            }
        }
        return dataSources;
    }
}
