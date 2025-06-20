package com.github.ktsr42.rsyncserver;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.github.ktsr42.yajsynclib.LibServer;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;


// Handler that receives messages from the thread
final class RsyncServer extends Handler {

    public static final String NOTIFICATION_CHANNEL_ID = "RsyncServerNotifications";
    public static final int NotificationId = 42;

    private class WifiNetworkCallback extends ConnectivityManager.NetworkCallback {
        private RsyncServerAppState pms = RsyncServerAppState.getInstance();

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            localaddr = null;
            stop();
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            // grab the first IPv4 address that is not loopback.

            for(LinkAddress la : linkProperties.getLinkAddresses()) {
                InetAddress ia = la.getAddress();
                if(ia.isLoopbackAddress()) continue;
                if(ia.getClass() != Inet4Address.class) continue;
                localaddr = ia;
                connectedToWifi();
                break;
            }
        }

    }

    private LibServer srv;
    private InetAddress localaddr;
    private boolean run = false;
    private RsyncServerAppState appstate = RsyncServerAppState.getInstance();
    private Context appContext;
    private long startRequestTime = 0;

    private static RsyncServer singleton_instance;

    private int port;
    private String moduleName;

    private void connectedToWifi() {
        if(!run) return;
        if(60000 > System.currentTimeMillis() - startRequestTime) start();  // if more than one minute elapsed since the user asked us to enable the rsync server and the
        else {                                                              // wifi becoming available, consider the request stale
            run = false;
            startRequestTime = 0;
        }
    }

    private void errorOpeningPort() {
        Toast.makeText(appContext,"Internal error opening listening port", Toast.LENGTH_LONG).show();
    }

    private void start() {
        if(!run) return;
        if(srv != null) return;

        if(localaddr == null) {
            startRequestTime = System.currentTimeMillis();
            Toast.makeText(appContext,"No Wifi service", Toast.LENGTH_LONG).show();
            return;  // no wifi
        }

        Log.d("RsyncServer", "Starting service");
        Object[] mnp = null;
        // FIXME: Environment.getExternalStorageDirectory() is deprecated in Android 11.
        // its replacement is Context.getExternalFilesDir (https://developer.android.com/reference/android/content/Context#getExternalFilesDir(java.lang.String))
        // This function accepts a null argument, but it points to a path that is not identical with
        // the return value of Environment.getExternalStorageDirectory().
        // We have to do more research, potentially switching to multiple rsync modules; one per
        // string argument accepted by getExternalFilesDir.
        String sharedStorage = Environment.getExternalStorageDirectory().toString();

        try {
            srv = new LibServer(moduleName, sharedStorage, port);
            mnp = srv.initServer(localaddr);
        } catch (BindException bex) {
            try {
                srv = new LibServer(moduleName, sharedStorage, 0);
                mnp = srv.initServer(localaddr);
            } catch( IOException e) {
                e.printStackTrace();
                errorOpeningPort();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            errorOpeningPort();
            return;
        }

        srv.run();

        appstate.localAddress.postValue(localaddr.toString());
        moduleName = (String)mnp[0];
        appstate.moduleName.postValue(moduleName);
        port = (Integer)mnp[1];
        appstate.portNum.postValue(port);

        displayNotification();
    }


    private void stop() {
        if(srv == null) return;

        appstate.localAddress.postValue(null);
        appstate.moduleName.postValue(null);
        appstate.portNum.postValue(null);

        Log.d("RsyncServer", "Stopping service");
        srv.stop();
        srv = null;
        cancelNotification();
    }

    private RsyncServer(Looper looper, Context appctx, ConnectivityManager cm, int p, String module) {
        super(looper);

        appContext = appctx;

        NetworkRequest.Builder nwrb = new NetworkRequest.Builder();
        nwrb.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        nwrb.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        cm.registerNetworkCallback(nwrb.build(), new WifiNetworkCallback());

        port = p;
        moduleName = module;
    }

    public static synchronized RsyncServer getRsyncServer(Context appctx, ConnectivityManager cm, int p, String module) {
        if(singleton_instance == null) {
            HandlerThread ht = new HandlerThread("Rsync Server Thread", Process.THREAD_PRIORITY_BACKGROUND);
            ht.start();
            singleton_instance = new RsyncServer(ht.getLooper(), appctx, cm, p, module);
        }

        return singleton_instance;
    }

    @Override
    public void handleMessage(Message msg) {
        if(msg.arg1 == 0) {
            run = false;
            stop();
        } else if(msg.arg1 == 1) {
            run = true;
            start();
        }
    }

    private void displayNotification() {
        Resources res = appContext.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.rsync_running)
            .setContentTitle(res.getString(R.string.notfTitle))
            .setContentText(res.getString(R.string.notfDescription))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOngoing(true);
        NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NotificationId, builder.build());
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationId);
    }
}
