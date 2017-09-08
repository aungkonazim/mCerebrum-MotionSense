package org.md2k.motionsense;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.md2k.mcerebrum.commons.app_info.AppInfo;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.mcerebrum.commons.permission.PermissionCallback;
import org.md2k.mcerebrum.core.access.AbstractServiceMCerebrum;
import org.md2k.motionsense.configuration.Configuration;
import org.md2k.motionsense.plot.ActivityPlotChoice;

public class ServiceMCerebrum extends AbstractServiceMCerebrum {
    public ServiceMCerebrum() {
    }


    @Override
    public void initialize() {
/*
        Permission.requestPermission(this, new PermissionCallback() {
            @Override
            public void OnResponse(boolean isGranted) {
                if (isGranted) {
                }
            }
        });
*/
    }

    @Override
    public void launch() {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void startBackground() {
        Intent intent=new Intent(this, ServiceMotionSense.class);
        startService(intent);
    }

    @Override
    public void stopBackground() {
        Intent intent=new Intent(this, ServiceMotionSense.class);
        stopService(intent);
    }

    @Override
    public void report() {
        Intent intent = new Intent(this, ActivityPlotChoice.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean hasReport() {
        return true;
    }

    @Override
    public boolean isRunInBackground() {
        return true;
    }

    @Override
    public long getRunningTime() {
        return AppInfo.serviceRunningTime(this, ServiceMotionSense.class.getName());
    }

    @Override
    public boolean isRunning() {
        return AppInfo.isServiceRunning(this, ServiceMotionSense.class.getName());
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
    public void configure() {
        Intent intent = new Intent(this, ActivitySettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
