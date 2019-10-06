package com.yx.demoservice;

import android.content.Context;
import android.util.Log;

import com.yx.demoservice.constants.Constants;

import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.handler.ContextHandlerCollection;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import android.net.EthernetManager;

import java.io.IOException;

/**
 * DefaultHandler
 * 主要处理ip+端口访问方式
 *
 * @author yx
 * @date 2019/5/16 20:35
 */
public class DefaultHandler extends ContextHandlerCollection {
    private static final String TAG = "DefaultHandler";

    private EthernetManager mEthManager = null;

    public DefaultHandler(Context mContext) {
        super();
        mEthManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
    }

    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch) throws IOException, ServletException {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        Log.w(TAG, "method =" + method + " ,uri =" + uri);
        if (method.equals(HttpMethods.GET) && Constants.WEB_SERVICE_EMPTY.equals(uri)) {
            if (mEthManager != null) {
                String ip = mEthManager.getIpAddress();
                if (ip != null) {
                    String targetUrl = "http://" + ip + ":" + Constants.WEB_SERVICE_PORT +
                            Constants.WEB_SERVICE_NAME + "/" + Constants.WEB_INDEX;
                    Log.w(TAG, "targetUrl =" + targetUrl);
                    response.sendRedirect(targetUrl);
                    return;
                }
            }
        }

    }
}
