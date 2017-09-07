package org.md2k.motionsense;

import android.content.Intent;
import android.os.Bundle;

import com.blankj.utilcode.util.ServiceUtils;

import org.md2k.mcerebrum.commons.app_info.AppInfo;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.mcerebrum.commons.permission.PermissionCallback;
import org.md2k.mcerebrum.core.access.AbstractActivityMCerebrumAccess;
import org.md2k.motionsense.configuration.Configuration;
import org.md2k.motionsense.plot.ActivityPlotChoice;

public class ActivityMCerebrumAccess extends AbstractActivityMCerebrumAccess {

    @Override
    public boolean isRunning() {
        return AppInfo.isServiceRunning(this, ServiceMotionSense.class.getName());
    }
    @Override
    public boolean plot(){
        Intent intent = new Intent(this, ActivityPlotChoice.class);
        startActivity(intent);
        return true;
    }

    @Override
    public long runningTime() {
        return AppInfo.serviceRunningTime(this, ServiceMotionSense.class.getName());
    }

    @Override
    public boolean initialize() {
        Permission.requestPermission(this, new PermissionCallback() {
            @Override
            public void OnResponse(boolean isGranted) {
                if (isGranted) {
                    prepareConfig();
                }
            }
        });
        return true;
    }


    void prepareConfig() {

    }
    @Override
    public boolean hasReport(){
        return true;
    }
    @Override
    public boolean IsRunInBackground() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return Configuration.isEqual();
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean configure() {
        Intent intent = new Intent(this, ActivitySettings.class);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean start() {
        Intent intent = new Intent(this, ServiceMotionSense.class);
        startService(intent);
        return true;
    }

    @Override
    public boolean stop() {
        Intent intent = new Intent(this, ServiceMotionSense.class);
        stopService(intent);
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
