package com.github.ktsr42.rsyncserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

// FIXME: Terminate service on application shutdown - but not on activity recreation (device flip)


public class MainActivity extends AppCompatActivity {

    private TextView tvwModuleName;
    private TextView tvwPortNumber;
    private TextView tvwAddress;

    private TextView tvwRsyncLine;

    private String ipaddress;
    private String portNum;
    private String module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvwModuleName = findViewById(R.id.tvwModuleName);
        tvwPortNumber = findViewById(R.id.tvwPortNumber);
        tvwAddress = findViewById(R.id.tvwAddress);
        tvwRsyncLine = findViewById(R.id.tvwRsyncLine);

        PortModuleSingleton pm = PortModuleSingleton.getInstance();
        final Observer<Integer> portNumObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                portNum = integer.toString();
                tvwPortNumber.setText(portNum);
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
        Intent intent = new Intent(this, RsyncReceiver.class);
        intent.putExtra(RsyncReceiver.TGT_MODULE_NAME, "testmod");
        intent.putExtra(RsyncReceiver.TGT_PORT, 12345);
        startService(intent);
    }

    public void stopRyncReceiver(View view) { stopService(new Intent(this, RsyncReceiver.class)); }

    private void setRsyncLine() {
        if(ipaddress == null || ipaddress == "") { tvwRsyncLine.setText(""); return; }
        if(portNum == null || portNum == "") { tvwRsyncLine.setText(""); return; }
        if(module == null || module == "") { tvwRsyncLine.setText(""); return; }

        String rsyncline = "rsync://" + ipaddress + ":" + portNum + "/" + module;
        tvwRsyncLine.setText(rsyncline);
    }
}
