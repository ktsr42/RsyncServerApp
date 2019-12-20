package com.github.ktsr42.rsyncserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.TextView;

// FIXME: Terminate service on application shutdown - but not on activity recreation (device flip)

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


        RsyncServerAppState pm = RsyncServerAppState.getInstance();
        final Observer<Integer> portNumObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(null == integer) tvwPortNumber.setText("");
                else                tvwPortNumber.setText(integer.toString());
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
                ipaddress = s;
                tvwAddress.setText(s);
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
}
