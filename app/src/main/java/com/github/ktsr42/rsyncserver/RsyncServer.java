package com.github.ktsr42.rsyncserver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.ktsr42.yajsynclib.LibServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

// Handler that receives messages from the thread
final class RsyncServer extends Handler {

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
                start();
                break;
            }
            // send a message to the background task?
        }

    }

    private LibServer srv;
    private InetAddress localaddr;
    private boolean run = false;
    private RsyncServerAppState appstate = RsyncServerAppState.getInstance();
    private Context appContext;

    private void start() {
        if(!run) return;
        if(localaddr == null) return; // no wifi

        if(srv != null) return;

        Toast.makeText(appContext,"Rsync Service Starting", Toast.LENGTH_LONG).show();
        Log.d("RsyncServer", "Starting service");
        srv = new LibServer(null, Environment.getExternalStorageDirectory().toString());
        Object[] mnp = new Object[0];
        try {
            mnp = srv.initServer(localaddr);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        srv.run();

        appstate.localAddress.postValue(localaddr.toString());
        appstate.moduleName.postValue((String)mnp[0]);
        appstate.portNum.postValue((Integer) mnp[1]);
    }


    private void stop() {
        if(srv == null) return;

        appstate.localAddress.postValue(null);
        appstate.moduleName.postValue(null);
        appstate.portNum.postValue(null);

        Toast.makeText(appContext,"rsync service stopping", Toast.LENGTH_SHORT).show();
        Log.d("RsyncServer", "Stopping service");
        srv.stop();
        srv = null;
    }

    public RsyncServer(Looper looper, Context appctx, ConnectivityManager cm) {
        super(looper);

        appContext = appctx;

        NetworkRequest.Builder nwrb = new NetworkRequest.Builder();
        nwrb.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        nwrb.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        cm.registerNetworkCallback(nwrb.build(), new WifiNetworkCallback());
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

}
