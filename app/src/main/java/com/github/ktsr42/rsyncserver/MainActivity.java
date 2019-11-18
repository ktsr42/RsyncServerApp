package com.github.ktsr42.rsyncserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tvwModuleName;
    private TextView tvwPortNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvwModuleName = findViewById(R.id.tvwModuleName);
        tvwPortNumber = findViewById(R.id.tvwPortNumber);
    }

    public void startRsyncServer(View view) {
        Intent intent = new Intent(this, RsyncReceiver.class);
        intent.putExtra(RsyncReceiver.TGT_MODULE_NAME, "testmod");
        intent.putExtra(RsyncReceiver.TGT_PORT, 12345);
        startService(intent);
    }


    public void stopRyncReceiver(View view) {
        stopService(new Intent(this, RsyncReceiver.class));
    }
}
