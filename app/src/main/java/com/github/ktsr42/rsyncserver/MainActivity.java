package com.github.ktsr42.rsyncserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.logging.Level;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static com.github.ktsr42.rsyncserver.RsyncServer.NOTIFICATION_CHANNEL_ID;

// New Design:
// Create HandlerThread in onCreate
// implement service as a handler
// hold handle to
// Use runOnUIThread or view.post to update activity from hand;er


public class MainActivity extends AppCompatActivity {

    private TextView tvwRsyncLine;

    private String ipaddress;
    private String portNum;
    private String module;

    private Boolean running = false;
    private int lastPortNum = 0;
    private String lastModule;

    private RsyncServer server;

    public static int STORAGE_ACCESS_REQUEST_ID = 0;
    public static String STATE_PORTNUM = "rsyncPort";
    public static String STATE_MODULENAME = "rsyncModule";
    public static String STATE_SAVE_TIME = "rsyncStateSaveTime";
    public static String STATE_RUNNING = "rsyncRunning";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvwRsyncLine = findViewById(R.id.tvwRsyncLine);

        initLogger();

        if(null != savedInstanceState) {
            if(10 * 60 * 1000 > System.currentTimeMillis() - savedInstanceState.getLong(STATE_SAVE_TIME)) {
                lastPortNum = savedInstanceState.getInt(STATE_PORTNUM);
                lastModule = savedInstanceState.getString(STATE_MODULENAME);
                running = savedInstanceState.getBoolean(STATE_RUNNING);
            }
        }

        server = RsyncServer.getRsyncServer(this.getApplicationContext(), (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), lastPortNum, lastModule);

        createNotificationChannel();

        RsyncServerAppState pm = RsyncServerAppState.getInstance();
        final Observer<Integer> portNumObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                String s = null;
                if(null == integer) s = "";
                else                s = integer.toString();
                portNum = s;
                try {
                    lastPortNum = Integer.parseInt(s);
                } catch (NumberFormatException nfx) {
                    // ignore
                }
                setRsyncLine();
            }
        };
        pm.portNum.observe(this, portNumObserver);

        final Observer<String> moduleNameObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                module = s;
                if(s != null && s.length() != 0) { lastModule = s; }
                lastModule = s;
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
                    } else if (addrparts[0].length() == 0 ) {
                        ipaddress = addrparts[1];      // no hostname string, just the ip
                    } else {
                        ipaddress = addrparts[0];      // we found a hostname against all odds
                    }
                }
                setRsyncLine();
            }
        };
        pm.localAddress.observe(this, addressObserver);

        if(running) startRsyncServer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PORTNUM, lastPortNum);
        outState.putString(STATE_MODULENAME, lastModule);
        outState.putLong(STATE_SAVE_TIME, System.currentTimeMillis());
        outState.putBoolean(STATE_RUNNING, running);
    }

    private void initLogger() {
      AndroidLoggingHandler.reset(new AndroidLoggingHandler());
      java.util.logging.Logger.getLogger("yajsync").setLevel(Level.FINEST);
    }

    public void startRsyncServerRequest(View view) {
        switch(haveWeStorageAccess()) {
            case 1: startRsyncServer(); break;
            case 0: break;
            case -1:
                Toast.makeText(this, "No storage access", Toast.LENGTH_LONG).show();
                break;
            default:
                Log.e("RsyncServer", "Unknown return value from haveStoragAccess()");
        }
    }

    private void startRsyncServer() {
        Message msg = server.obtainMessage();
        msg.arg1 = 1;
        server.sendMessage(msg);
        running = true;
    }

    public void stopRyncReceiver(View view) {
        Message msg = server.obtainMessage();
        msg.arg1 = 0;
        server.sendMessage(msg);
        running = false;
    }

    private void removeRsyncLine() {
        tvwRsyncLine.setText("");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setRsyncLine() {
        if(ipaddress == null || ipaddress == "") { removeRsyncLine(); return; }
        if(portNum == null || portNum == "") { removeRsyncLine(); return; }
        if(module == null || module == "") { removeRsyncLine(); return; }

        String rsyncline = "rsync://" + ipaddress + ":" + portNum + "/" + module;
        tvwRsyncLine.setText(rsyncline);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private int checkStoragePermission(String perm) {
        if(ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED ) return 1;

        if(ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) { return -1; }

        ActivityCompat.requestPermissions(this, new String[]{perm}, STORAGE_ACCESS_REQUEST_ID);
        return 0;
    }

    private int haveWeStorageAccess() {
        int read_access = checkStoragePermission(WRITE_EXTERNAL_STORAGE);
        int write_access = checkStoragePermission(READ_EXTERNAL_STORAGE);

        if(read_access < write_access) return read_access;
        else                           return write_access;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode != STORAGE_ACCESS_REQUEST_ID) {
            Log.d("RsyncServer", "Unexpected request code: " + Integer.toString(requestCode));
            return;
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRsyncServer();
        } else {
            if(grantResults.length == 0) {
                Log.d("RsyncServer", "No grant results?!?");
            } else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.d("RsyncServer", "Storage access denied by user");
            } else {
                Log.d("RsyncServer", "Unexpected permission code: " + Integer.toString(grantResults[0]));
            }

        }
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
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
