package com.github.ktsr42.rsyncserver;

public class PortModuleSingleton {
  private static PortModuleSingleton _instance;
  public static PortModuleSingleton getInstance() {
    if(null == _instance) {
      _instance = new PortModuleSingleton();
    }
    return _instance;
  }

  private int _port = -1;
  private String _moduleName;

  public int getPort() { return _port; }
  public String getModuleName() { return _moduleName; }

  public void setState(int port, String moduleName) {
    _port = port;
    _moduleName = moduleName;
  }
}
