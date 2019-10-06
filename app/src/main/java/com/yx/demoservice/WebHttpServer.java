package com.yx.demoservice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.Log;

import com.yx.demoservice.constants.Constants;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * WebHttpServer
 *
 * @author yx
 * @date 2019/6/19 13:04
 */
public class WebHttpServer {
    private static final String TAG = "WebHttpServer";

    private Context mContext;
    private static final String JETTY = "jetty";

    private static final File __JETTY_DIR;

    private static final String __WEBAPP_DIR = "webapps";
    private static final String __ETC_DIR = "etc";
    private static final String __CONTEXTS_DIR = "contexts";

    private static final String __TMP_DIR = "tmp";
    private static final String __WORK_DIR = "work";

    private Thread progressThread;

    static {
        __JETTY_DIR = new File(Environment.getExternalStorageDirectory(), JETTY);

        if (!__JETTY_DIR.exists()) {
            __JETTY_DIR.mkdirs();
        }
    }

    public WebHttpServer(Context mContext) {
        this.mContext = mContext;
    }

    public void start() {
        Log.w("IJettyBootReceiver", "-----WebHttpServer start ... ------------");
        initIJetty();
        startIJetty();
        Log.w("IJettyBootReceiver", "-----WebHttpServer started ------------");
    }

    private void startIJetty() {
        if (isUpdateNeeded()) {
            setupJetty();
        }
        startJettyService();
    }

    private void startJettyService() {
        try {
            Server server = new Server();
            Connector connector = new SelectChannelConnector();
            connector.setPort(Constants.WEB_SERVICE_PORT);
            server.setConnectors(new Connector[]{connector});

            String webapp = __JETTY_DIR + File.separator + __WEBAPP_DIR + File.separator +
                    Constants.WEB_SERVICE_PACKAGE_NAME + File.separator;
            WebAppContext root = new WebAppContext(webapp, Constants.WEB_SERVICE_NAME);
            root.setResourceBase(webapp);
            root.setClassLoader(Thread.currentThread().getContextClassLoader());
            HandlerCollection handlers = new HandlerCollection();
            handlers.setHandlers(new Handler[]{root, new DefaultHandler(mContext)});
            server.addHandler(handlers);

            Log.w(TAG, "-----startJettyService webapp dir = " + webapp);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupJetty() {
        progressThread = new ProgressThread();
        progressThread.start();
    }

    private boolean isUpdateNeeded() {
        // if no previous version file, assume update is required
        int storedVersion = getStoredJettyVersion();
        if (storedVersion <= 0) {
            Log.w(TAG, "storedVersion <=0 ");
            return true;
        }
        try {
            // if different previous version, update is required
            PackageInfo pi =
                    mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            if (pi == null) {
                Log.w(TAG, "pi is null ");
                return true;
            }
            if (pi.versionCode != storedVersion) {
                Log.w(TAG, "pi is not match ");
                return true;
            }

            // if /sdcard/jetty/.update file exists, then update is required
            File alwaysUpdate = new File(__JETTY_DIR, ".update");
            if (alwaysUpdate.exists()) {
                Log.i(TAG, "Always Update tag found " + alwaysUpdate);
                return true;
            }
        } catch (Exception e) {
            // if any of these tests go wrong, best to assume update is true?
            Log.w(TAG, "these tests go wrong ");
            return true;
        }

        return false;
    }

    private int getStoredJettyVersion() {
        File jettyDir = __JETTY_DIR;
        if (!jettyDir.exists()) {
            return -1;
        }
        File versionFile = new File(jettyDir, "version.code");
        if (!versionFile.exists()) {
            return -1;
        }
        int val = -1;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(versionFile));
            val = ois.readInt();
            return val;
        } catch (Exception e) {
            Log.e(TAG, "Problem reading version.code", e);
            return -1;
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e) {
                    Log.d(TAG, "Error closing version.code input stream", e);
                }
            }
        }
    }

    private void initIJetty() {
        try {
            extract(mContext.getResources().openRawResource(R.raw.wms));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Extract the war.
     *
     * @param warStream
     * @throws IOException
     */
    private void extract(InputStream warStream) throws IOException {

        if (warStream == null) {
            throw new IllegalArgumentException("No war file found");
        }

        File jettyDir = getJettyInstallDir();
        if (jettyDir == null) {
            throw new IllegalStateException(mContext.getString(R.string.jetty_not_installed));
        }

        File webappsDir = new File(jettyDir, __WEBAPP_DIR);
        if (!webappsDir.exists()) {
            webappsDir.mkdirs();
            // throw new IllegalStateException (getString(R.string.jettyNotInstalled));
        }

        File webapp = new File(webappsDir, Constants.WEB_SERVICE_PACKAGE_NAME);
        if (!webapp.exists()) {
            webapp.mkdirs();
        }
        JarInputStream jin = new JarInputStream(warStream);
        JarEntry entry;
        while ((entry = jin.getNextJarEntry()) != null) {
            String entryName = entry.getName();
            File file = new File(webapp, entryName);
            if (entry.isDirectory()) {
                // Make directory
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                // make directory (some jars don't list dirs)
                File dir = new File(file.getParent());
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Make file
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(file);
                    IO.copy(jin, fout);
                } finally {
                    IO.close(fout);
                }

                // touch the file.
                if (entry.getTime() >= 0) {
                    file.setLastModified(entry.getTime());
                }
            }
        }
        IO.close(jin);
    }

    /**
     * Check to see if jetty has been installed.
     *
     * @return File of jetty install dir or null if not installed
     */
    private File getJettyInstallDir() {
        File jettyDir = new File(Environment.getExternalStorageDirectory(), JETTY);
        if (!jettyDir.exists()) {
            return null;
        }
        return jettyDir;
    }

    class ProgressThread extends Thread {

        private ProgressThread() {
        }

        @Override
        public void run() {
            boolean updateNeeded = isUpdateNeeded();

            // create the jetty dir structure
            File jettyDir = __JETTY_DIR;
            if (!jettyDir.exists()) {
                boolean made = jettyDir.mkdirs();
                Log.i(TAG, "Made " + __JETTY_DIR + ": " + made);
            }

            // Do not make a work directory to preserve unpacked
            // webapps - this seems to clash with Android when
            // out-of-date webapps are deleted and then re-unpacked
            // on a jetty restart: Android remembers where the dex
            // file of the old webapp was installed, but it's now
            // been replaced by a new file of the same name. Strangely,
            // this does not seem to affect webapps unpacked to tmp?
            // Original versions of i-jetty created a work directory. So
            // we will delete it here if found to ensure webapps can be
            // updated successfully.
            File workDir = new File(jettyDir, __WORK_DIR);
            if (workDir.exists()) {
                delete(workDir);
                Log.i(TAG, "removed work dir");
            }

            // make jetty/tmp
            File tmpDir = new File(jettyDir, __TMP_DIR);
            if (!tmpDir.exists()) {
                boolean made = tmpDir.mkdirs();
                Log.i(TAG, "Made " + tmpDir + ": " + made);
            } else {
                Log.i(TAG, tmpDir + " exists");
            }

            // make jetty/webapps
            File webappsDir = new File(jettyDir, __WEBAPP_DIR);
            if (!webappsDir.exists()) {
                boolean made = webappsDir.mkdirs();
                Log.i(TAG, "Made " + webappsDir + ": " + made);
            } else {
                Log.i(TAG, webappsDir + " exists");
            }

            // make jetty/etc
            File etcDir = new File(jettyDir, __ETC_DIR);
            if (!etcDir.exists()) {
                boolean made = etcDir.mkdirs();
                Log.i(TAG, "Made " + etcDir + ": " + made);
            } else {
                Log.i(TAG, etcDir + " exists");
            }

            File webdefaults = new File(etcDir, "webdefault.xml");
            if (!webdefaults.exists() || updateNeeded) {
                // get the webdefaults.xml file out of resources
                try {
                    InputStream is = mContext.getResources().openRawResource(R.raw.webdefault);
                    OutputStream os = new FileOutputStream(webdefaults);
                    IO.copy(is, os);
                    Log.i(TAG, "Loaded webdefault.xml");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading webdefault.xml", e);
                }
            }

            File realm = new File(etcDir, "realm.properties");
            if (!realm.exists() || updateNeeded) {
                try {
                    // get the realm.properties file out resources
                    InputStream is =
                            mContext.getResources().openRawResource(R.raw.realm_properties);
                    OutputStream os = new FileOutputStream(realm);
                    IO.copy(is, os);
                    Log.i(TAG, "Loaded realm.properties");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading realm.properties", e);
                }
            }

            // make jetty/contexts
            File contextsDir = new File(jettyDir, __CONTEXTS_DIR);
            if (!contextsDir.exists()) {
                boolean made = contextsDir.mkdirs();
                Log.i(TAG, "Made " + contextsDir + ": " + made);
            } else {
                Log.i(TAG, contextsDir + " exists");
            }

            try {
                PackageInfo pi =
                        mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                if (pi != null) {
                    setStoredJettyVersion(pi.versionCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to get PackageInfo for i-jetty");
            }

            // if there was a .update file indicating an update was needed, remove it now
            // we've updated
            File update = new File(__JETTY_DIR, ".update");
            if (update.exists()) {
                update.delete();
            }
        }

        private void delete(File webapp) {
            if (webapp.isDirectory()) {
                File[] files = webapp.listFiles();
                for (File f : files) {
                    delete(f);
                }
                webapp.delete();
            } else {
                webapp.delete();
            }
        }
    }

    private void setStoredJettyVersion(int version) {
        File jettyDir = __JETTY_DIR;
        if (!jettyDir.exists()) {
            return;
        }
        File versionFile = new File(jettyDir, "version.code");
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fos = new FileOutputStream(versionFile);
            oos = new ObjectOutputStream(fos);
            oos.writeInt(version);
            oos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Problem writing jetty version", e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    Log.d(TAG, "Error closing version.code output stream", e);
                }
            }
        }
    }
}
