package com.yx.demoservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * WebService
 *
 * @author yx
 * @date 2019/5/16 20:35
 */
public class WebService extends Service {

    private static final String TAG = "WebService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service create");
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void init() {
        Log.d(TAG, "init");
        startHttpServer();
    }

    /**
     * 启动http server
     */
    private void startHttpServer() {
        WebHttpServer server = new WebHttpServer(this);
        server.start();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service on destroy");
        super.onDestroy();
    }
}
