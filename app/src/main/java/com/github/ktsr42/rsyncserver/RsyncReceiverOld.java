package com.github.ktsr42.rsyncserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.Nullable;

// Instantiate the service on startup, but do not run it
// convert service to a foreground service (tray icon)
// service registers a network callback
// rsync server is enabled when both the start button and the wifi connection are up
// rsync server is disabled when either is off
// take local address from the linkproperties network callback
//   find first address that is IPv4 and not loopback

public class RsyncReceiverOld extends Service {

    public static final String TGT_MODULE_NAME = "targetModuleName";
    public static final String TGT_PORT = "targetPort";

    private RsyncServer serviceHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        // either fork of new thread here or in onStartCommand()
        // need to send the port number back and forth
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        //serviceHandler = new RsyncServer(thread.getLooper(), (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "RsyncReceiverOld service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.obj = intent.getStringExtra(TGT_MODULE_NAME);
        msg.arg1 = intent.getIntExtra(TGT_PORT, 0);
        serviceHandler.sendMessage(msg);

        // Do not restart this if we get killed after returning from here
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "RsyncReceiverOld service stopping.", Toast.LENGTH_SHORT).show();

        RsyncServerAppState pms = RsyncServerAppState.getInstance();
        pms.localAddress.setValue("");
        pms.moduleName.setValue("");
        pms.portNum.setValue(null);
    }
}

