package com.pbs.server.http;

import java.io.File;

/**
 * To update the Generated binding header: `javac -h . ServerUtil.java`
 */
public class ServerUtil {

  static {
    System.loadLibrary("server_rs");
    System.out.println("Successfully loaded native libraries");
  }

  private ServerUtil() {}

  private static native void startServer(String path);

  public static void startServer(File cacheDir) {
    startServer(cacheDir.getPath());
  }
}
