package com.github.ktsr42.rsyncserver;

import androidx.lifecycle.MutableLiveData;

import java.net.InetAddress;

public class PortModuleSingleton {
  private static PortModuleSingleton _instance;
  public static synchronized PortModuleSingleton getInstance() {
    if(null == _instance) {
      _instance = new PortModuleSingleton();
    }
    return _instance;
  }

  MutableLiveData<Integer> portNum = new MutableLiveData<>();
  MutableLiveData<String> moduleName = new MutableLiveData<>();
  MutableLiveData<String> localAddress = new MutableLiveData<>();
  MutableLiveData<Boolean> onWifi = new MutableLiveData<>();
}
