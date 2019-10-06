package com.yx.demoservice;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
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
}
