package com.github.ktsr42.rsyncserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tvwModuleName;
    private TextView tvwPortNumber;
    private TextView tvwAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvwModuleName = findViewById(R.id.tvwModuleName);
        tvwPortNumber = findViewById(R.id.tvwPortNumber);
        tvwAddress = findViewById(R.id.tvwAddress);

        PortModuleSingleton pm = PortModuleSingleton.getInstance();
        final Observer<Integer> portNumObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                tvwPortNumber.setText(integer.toString());
            }
        };
        pm.portNum.observe(this, portNumObserver);

        final Observer<String> moduleNameObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvwModuleName.setText(s);
            }
        };
        pm.moduleName.observe(this, moduleNameObserver);

        final Observer<String> addressObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvwAddress.setText(s);
            }
        };
        pm.localAddress.observe(this, addressObserver);
    }

    // FIXME: switch to service binding
    public void startRsyncServer(View view) {
        Intent intent = new Intent(this, RsyncReceiver.class);
        intent.putExtra(RsyncReceiver.TGT_MODULE_NAME, "testmod");
        intent.putExtra(RsyncReceiver.TGT_PORT, 12345);
        startService(intent);
    }

    public void stopRyncReceiver(View view) { stopService(new Intent(this, RsyncReceiver.class)); }
}
