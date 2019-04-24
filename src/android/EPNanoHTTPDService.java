package com.aminbros.abjiaozi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.aminbros.epnanohttpd.EPNanoHTTPD;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;

import fi.iki.elonen.NanoHTTPD;


public class EPNanoHTTPDService extends IntentService {
  public final static String TAG = "EPNanoHttpdService";
  public final static String ACTION_INITIAL_RESPONSE = "EPNanoHTTPD_INITIAL_RESPONSE";
  public final static String ACTION_HTTPD_STOPPED = "EPNanoHTTPD_HTTPD_STOPPED";
  public final static long DEFAULT_TIMEOUT = 10 * 1000;
  EPNanoHTTPD mHttpd;
  String mHostName;
  int mPort;
  String mProxyUrl;
  boolean mAlreadyStarted;
  Timer mCleanupTimer;


  /**
   * A constructor is required, and must call the super <code><a href="/reference/android/app/IntentService.html#IntentService(java.lang.String)">IntentService(String)</a></code>
   * constructor with a name for the worker thread.
   */
  public EPNanoHTTPDService() {
    super(TAG);
    mPort = 0;
    mCleanupTimer = null;
    mHostName = null;
    mProxyUrl = null;
  }

  /**
   * The IntentService calls this method from the default worker thread with
   * the intent that started the service. When this method returns, IntentService
   * stops the service, as appropriate.
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      try {
        ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        if (mHttpd == null) {
          if (ai.metaData != null) {
            mProxyUrl = ai.metaData.getString("HTTPD_PROXY_URL");
            mHostName = ai.metaData.getString("HTTPD_HOST");
          }
          if (mPort == 0) {
            mPort = getFreePort();
          }
          File cachedir = getDir("httpcache", Context.MODE_PRIVATE);
          if (mHostName == null) {
            mHostName = "localhost";
          }
          mHttpd = new EPNanoHTTPD(mHostName, mPort, cachedir, mProxyUrl);
        }
        // notify success
        final Intent successIntent = new Intent(ACTION_INITIAL_RESPONSE);
        Bundle extra = new Bundle();
        extra.putBoolean("success", true);
        extra.putString("hostname", mHostName);
        extra.putInt("port", mPort);
        extra.putString("proxyurl", mProxyUrl);
        successIntent.putExtras(extra);
        sendBroadcast(successIntent);
        // start the daemon
        if (!mAlreadyStarted) {
          mAlreadyStarted = true;
          mHttpd.setAsyncRunner(new NanoHTTPD.DefaultAsyncRunner() {
              @Override
              public void exec(NanoHTTPD.ClientHandler clientHandler) {
                super.exec(clientHandler);
                synchronized (EPNanoHTTPDService.this) {
                  // cancel cleanup timer if exists
                  if (mCleanupTimer != null) {
                    mCleanupTimer.cancel();
                    mCleanupTimer = null;
                  }
                }
              }

              @Override
              public void closed(NanoHTTPD.ClientHandler clientHandler) {
                super.closed(clientHandler);
                synchronized (EPNanoHTTPDService.this) {
                  // cancel cleanup timer if exists
                  if (mCleanupTimer != null) {
                    mCleanupTimer.cancel();
                    mCleanupTimer = null;
                  }
                  if (this.getRunning().size() == 0) {
                    mCleanupTimer = new Timer();
                    mCleanupTimer.schedule(new CleanupTimerTask(), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
                  }
                }
              }
            });
          if (mCleanupTimer == null) {
            mCleanupTimer = new Timer();
            mCleanupTimer.schedule(new CleanupTimerTask(), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
          }
          mHttpd.start();
        }
      } catch (PackageManager.NameNotFoundException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (RuntimeException e) {
      Log.e(TAG, e.getMessage());
      Intent errorIntent = new Intent(ACTION_INITIAL_RESPONSE);
      Bundle extra = new Bundle();
      extra.putBoolean("success", false);
      extra.putString("error_message", e.getMessage());
      errorIntent.putExtras(extra);
      sendBroadcast(errorIntent);
      // notify the error
    }
  }

  protected int getFreePort () {
    try {
      ServerSocket s = new ServerSocket(0);
      int freeport = s.getLocalPort();
      s.close();
      return freeport;
    } catch (IOException e) {
      throw new RuntimeException("Could not generate port", e);
    }
  }
  public class CleanupTimerTask extends TimerTask {
    @Override
    public void run() {
      synchronized (EPNanoHTTPDService.this) {
        // trigger once
        if (mCleanupTimer == null) {
          return; // mCleanupTimer does not exists!
        }
        Log.w(TAG, "cleanup!");
        mCleanupTimer.cancel();
        mCleanupTimer = null;
        if (mHttpd != null) {
          if (mHttpd.isAlive()) {
            mHttpd.stop();
          }
          mAlreadyStarted = false;
          mHttpd = null;
          Intent errorIntent = new Intent(ACTION_HTTPD_STOPPED);
          sendBroadcast(errorIntent);
        }
      }
    }
  }
}
