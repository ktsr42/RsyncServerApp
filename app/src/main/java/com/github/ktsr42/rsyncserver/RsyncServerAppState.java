package com.github.ktsr42.rsyncserver;

import android.net.ConnectivityManager;
import android.os.HandlerThread;

import androidx.lifecycle.MutableLiveData;

public class RsyncServerAppState {
  private static RsyncServerAppState _instance;
  public static synchronized RsyncServerAppState getInstance() {
    if(null == _instance) {
      _instance = new RsyncServerAppState();
    }
    return _instance;
  }

  MutableLiveData<Integer> portNum = new MutableLiveData<>();
  MutableLiveData<String> moduleName = new MutableLiveData<>();
  MutableLiveData<String> localAddress = new MutableLiveData<>();
}
