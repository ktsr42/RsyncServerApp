package com.github.ktsr42.rsyncserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static com.github.ktsr42.rsyncserver.RsyncServer.NOTIFICATION_CHANNEL_ID;

// New Design:
// Create HandlerThread in onCreate
// implement service as a handler
// hold handle to
// Use runOnUIThread or view.post to update activity from hand;er


public class MainActivity extends AppCompatActivity {

    private Button btnStartStop;
    private TextView tvwRsyncLine;
    private RadioButton rdbtnNoPassword;
    private RadioButton rdbtnRandomPassword;
    private RadioButton rdbtnCustomPassword;
    private EditText tedtCustomPassword;

    private String ipaddress;
    private String portNum;
    private String module;

    private Boolean running = false;
    private int lastPortNum = 0;
    private String lastModule;
    private String passwordSelector = "none";
    private String randomPassword;
    private String userPassword;

    private RsyncServer server;

    public static int STORAGE_ACCESS_REQUEST_ID = 0;
    public static String STATE_PORTNUM = "rsyncPort";
    public static String STATE_MODULENAME = "rsyncModule";
    public static String STATE_SAVE_TIME = "rsyncStateSaveTime";
    public static String STATE_RUNNING = "rsyncRunning";
    public static String STATE_PASSWORD = "rsyncPasswordSet";  // none, random or custom
    public static String STATE_PASSWORD_RANDOM = "rsyncPasswordRandom";
    public static String STATE_PASSWORD_CUSTOM = "rsyncPasswordCustom";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartStop = findViewById(R.id.btnStartStop);
        tvwRsyncLine = findViewById(R.id.tvwRsyncLine);
        rdbtnNoPassword = findViewById(R.id.password_none);
        rdbtnRandomPassword = findViewById(R.id.password_random);
        rdbtnCustomPassword = findViewById(R.id.password_enter);
        tedtCustomPassword = findViewById(R.id.password_entry);

        initLogger();

        passwordSelector = "none";
        randomPassword = new Random().ints(10, 48, 122).mapToObj(i -> String.valueOf((char)i)).collect(Collectors.joining());
        userPassword = "";

        if(null != savedInstanceState) {
            if(10 * 60 * 1000 > System.currentTimeMillis() - savedInstanceState.getLong(STATE_SAVE_TIME)) {
                lastPortNum = savedInstanceState.getInt(STATE_PORTNUM);
                lastModule = savedInstanceState.getString(STATE_MODULENAME);
                running = savedInstanceState.getBoolean(STATE_RUNNING);
                passwordSelector = savedInstanceState.getString(STATE_PASSWORD);
                randomPassword = savedInstanceState.getString(STATE_PASSWORD_RANDOM, randomPassword);
                userPassword = savedInstanceState.getString(STATE_PASSWORD_CUSTOM, "");
            }
        } else {
            btnStartStop.setText("Start");
        }

        rdbtnRandomPassword.setText(rdbtnRandomPassword.getText() + randomPassword); // append the random password to the label
        tedtCustomPassword.setText(userPassword);
        if(passwordSelector.equals("random")) {
            rdbtnRandomPassword.toggle();
        } else if(passwordSelector.equals("custom")) {
            rdbtnCustomPassword.toggle();
        } else {
            rdbtnNoPassword.toggle();
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
        userPassword = tedtCustomPassword.getText().toString();
        outState.putInt(STATE_PORTNUM, lastPortNum);
        outState.putString(STATE_MODULENAME, lastModule);
        outState.putLong(STATE_SAVE_TIME, System.currentTimeMillis());
        outState.putBoolean(STATE_RUNNING, running);
        outState.putString(STATE_PASSWORD, passwordSelector);
        outState.putString(STATE_PASSWORD_RANDOM, randomPassword);
        outState.putString(STATE_PASSWORD_CUSTOM, userPassword);
    }

    private void initLogger() {
      AndroidLoggingHandler.reset(new AndroidLoggingHandler());
      java.util.logging.Logger.getLogger("yajsync").setLevel(Level.FINEST);
    }

    public void startRsyncServerRequest(View view) {
        userPassword = tedtCustomPassword.getText().toString();
        if(!checkStorageAccess()) {
            Toast.makeText(this, "No storage access (yet?)", Toast.LENGTH_SHORT).show();
            return;
        }
        startRsyncServer();
    }

    public void setPasswordNone(View view) {
        passwordSelector = "none";
        tedtCustomPassword.setEnabled(false);
    }
    public void setPasswordRandom(View view) {
        passwordSelector = "random";
        tedtCustomPassword.setEnabled(false);
    }
    public void setPasswordCustom(View view){
        passwordSelector = "custom";
        tedtCustomPassword.setEnabled(true);
    }

    private void startRsyncServer() {
        Bundle b = new Bundle();
        String password = null;
        if("random".equals(passwordSelector))
            password = randomPassword;
        else if("custom".equals(passwordSelector))
            password = userPassword;
        b.putString("password", password);
        Message msg = server.obtainMessage();
        msg.arg1 = 1;
        msg.setData(b);
        server.sendMessage(msg);
        running = true;
    }

    public void stopRsyncReceiver(View view) {
        Message msg = server.obtainMessage();
        msg.arg1 = 0;
        server.sendMessage(msg);
        running = false;
    }

    private void removeRsyncLine() {
        tvwRsyncLine.setText("");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStartStop.setText("START");
    }

    private void setRsyncLine() {
        if(ipaddress == null || ipaddress == "") { removeRsyncLine(); return; }
        if(portNum == null || portNum == "") { removeRsyncLine(); return; }
        if(module == null || module == "") { removeRsyncLine(); return; }

        String rsyncline = "rsync://" + ipaddress + ":" + portNum + "/" + module;
        tvwRsyncLine.setText(rsyncline);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStartStop.setText("STOP");
    }

    // API Level 26 to 29
    private int checkStoragePermission(String perm) {
        if(ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED ) return 1;
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) { return -1; }
        return 0;
    }

    private boolean checkStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return false;
            }
            return true;
        } else {
            int read_access = checkStoragePermission(WRITE_EXTERNAL_STORAGE);
            int write_access = checkStoragePermission(READ_EXTERNAL_STORAGE);
            if(read_access == -1 | write_access == -1) return false; // no access
            if(read_access == 1 & write_access == 1) return true; // go ahead

            // we need to request the permissions from the user, figure out which ones
            String[] perms_required = null;
            if(read_access == 0 & write_access == 0) {
                perms_required = new String[] {READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};
            } else if(read_access == 0) {
                perms_required = new String[] {READ_EXTERNAL_STORAGE};
            } else {
                perms_required = new String[] {WRITE_EXTERNAL_STORAGE};
            }
            if(perms_required == null | perms_required.length == 0) {
                throw new RuntimeException("Sanity check for permission request has failed.");
            }
            ActivityCompat.requestPermissions(this, perms_required, STORAGE_ACCESS_REQUEST_ID);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != STORAGE_ACCESS_REQUEST_ID) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRsyncServer();
        } else {
            if (grantResults.length == 0) {
                Log.d("RsyncServer", "No grant results?!?");
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.d("RsyncServer", "Storage access denied by user");
            } else {
                Log.d("RsyncServer", "Unexpected permission code: " + Integer.toString(grantResults[0]));
            }
        }
    }

    private void createNotificationChannel() {
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
