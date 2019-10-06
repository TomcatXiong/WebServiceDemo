package com.yx.demoservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * 服务开机自启动
 *
 * @author yx
 * @date 2019/6/21 10:35
 */
public class JettyBootReceiver extends BroadcastReceiver {
    private static final String TAG = "JettyBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "JettyBootReceiver onReceive");

        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, " onReceive BOOT_COMPLETED");
            Intent service = new Intent("com.yx.demoservice.WebService");
            service.setPackage("com.yx.demoservice");
            service.setComponent(
                    new ComponentName("com.yx.demoservice", "com.yx.demoservice.WebService"));
            context.startService(service);
        }
    }

}
