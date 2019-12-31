package com.github.ktsr42.rsyncserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.TextView;

import static com.github.ktsr42.rsyncserver.RsyncServer.NOTIFICATION_CHANNEL_ID;

// New Design:
// Create HandlerThread in onCreate
// implement service as a handler
// hold handle to
// Use runOnUIThread or view.post to update activity from hand;er


public class MainActivity extends AppCompatActivity {

    private TextView tvwModuleName;
    private TextView tvwPortNumber;
    private TextView tvwAddress;

    private TextView tvwRsyncLine;

    private String ipaddress;
    private String portNum;
    private String module;

    private RsyncServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvwModuleName = findViewById(R.id.tvwModuleName);
        tvwPortNumber = findViewById(R.id.tvwPortNumber);
        tvwAddress = findViewById(R.id.tvwAddress);
        tvwRsyncLine = findViewById(R.id.tvwRsyncLine);

        HandlerThread ht = new HandlerThread("Rsync Server Thread", Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();

        server = new RsyncServer(ht.getLooper(), this.getApplicationContext(), (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));

        createNotificationChannel();

        RsyncServerAppState pm = RsyncServerAppState.getInstance();
        final Observer<Integer> portNumObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                String s = null;
                if(null == integer) s = "";
                else                s = integer.toString();
                portNum = s;
                tvwPortNumber.setText(s);
                setRsyncLine();
            }
        };
        pm.portNum.observe(this, portNumObserver);

        final Observer<String> moduleNameObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                module = s;
                tvwModuleName.setText(s);
                setRsyncLine();
            }
        };
        pm.moduleName.observe(this, moduleNameObserver);

        final Observer<String> addressObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s == null) {
                    ipaddress = "";
                } else {
                    String[] addrparts = s.split("/", 2);
                    if (addrparts.length == 1) {
                        ipaddress = s;                 // no slash in the address string
                    } else if (addrparts[0].length() ==0 ) {
                        ipaddress = addrparts[1];      // no hostname string, just the ip
                    } else {
                        ipaddress = addrparts[0];      // we found a hostname against all odds
                    }
                }
                tvwAddress.setText(ipaddress);
                setRsyncLine();
            }
        };
        pm.localAddress.observe(this, addressObserver);
    }

    public void startRsyncServer(View view) {
        Message msg = server.obtainMessage();
        msg.arg1 = 1;
        server.sendMessage(msg);
    }

    public void stopRyncReceiver(View view) {
        Message msg = server.obtainMessage();
        msg.arg1 = 0;
        server.sendMessage(msg);
    }

    private void setRsyncLine() {
        if(ipaddress == null || ipaddress == "") { tvwRsyncLine.setText(""); return; }
        if(portNum == null || portNum == "") { tvwRsyncLine.setText(""); return; }
        if(module == null || module == "") { tvwRsyncLine.setText(""); return; }

        String rsyncline = "rsync://" + ipaddress + ":" + portNum + "/" + module;
        tvwRsyncLine.setText(rsyncline);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.nchName);
            String description = getString(R.string.nchDescription);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
