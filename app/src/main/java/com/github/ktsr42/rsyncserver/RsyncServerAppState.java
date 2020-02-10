package com.github.ktsr42.rsyncserver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import androidx.lifecycle.MutableLiveData;

public class RsyncServerAppState {
  private static RsyncServerAppState _instance;
  private static RsyncServer _rsInstance;

  public static synchronized RsyncServerAppState getInstance() {
    if(null == _instance) {
      _instance = new RsyncServerAppState();
    }
    return _instance;
  }

  public static synchronized RsyncServer getRsyncServer(Context appctx, ConnectivityManager cm, int p, String module) {
    if(_rsInstance == null) {
      HandlerThread ht = new HandlerThread("Rsync Server Thread", Process.THREAD_PRIORITY_BACKGROUND);
      ht.start();
      _rsInstance = new RsyncServer(ht.getLooper(), appctx, cm, p, module);
    }

    return _rsInstance;
  }

  MutableLiveData<Integer> portNum = new MutableLiveData<>();
  MutableLiveData<String> moduleName = new MutableLiveData<>();
  MutableLiveData<String> localAddress = new MutableLiveData<>();
}
