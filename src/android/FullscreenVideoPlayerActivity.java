package com.aminbros.abjiaozi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.app.Activity;
import android.widget.RelativeLayout;
import android.support.v7.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import cn.jzvd.JZUtils;
import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

public class FullscreenVideoPlayerActivity extends AppCompatActivity {
  String mVideoUrl;
  String mTitle;
  boolean mUseCache;
  String mRunningVideoUrl;
  BroadcastReceiver mHTTPDBR = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra("success", true)) {
          // start the video
          final String hostname = intent.getStringExtra("hostname");
          final int port = intent.getIntExtra("port", 0);
          runOnUiThread(new Runnable() {
              @Override
              public void run() {
                try {
                  String videoUrl = "http://" + hostname + ":" + port + "/?__u__=" + URLEncoder.encode(mVideoUrl, "UTF-8");
                  if (mRunningVideoUrl == null || !mRunningVideoUrl.equals(videoUrl)) {
                    mRunningVideoUrl = videoUrl;
                    Jzvd.startFullscreenDirectly(FullscreenVideoPlayerActivity.this, JzvdFullscreenStd.class, videoUrl, mTitle);
                  }
                } catch (UnsupportedEncodingException e) {
                  throw new RuntimeException(e);
                }
              }
            });
        } else {
          AlertDialog alertDialog = new AlertDialog.Builder(FullscreenVideoPlayerActivity.this).create();
          alertDialog.setTitle("ERROR");
          alertDialog.setMessage("Could not start httpd proxy!");
          alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                  public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                  }
                                });
          alertDialog.show();
        }
      }
    };
  BroadcastReceiver mHTTPDStoppedBR = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          startService(new Intent(FullscreenVideoPlayerActivity.this, EPNanoHTTPDService.class));

        }
      });
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent iIntent = getIntent();
    mVideoUrl = iIntent.getData().toString();
    if (!(mVideoUrl.startsWith("http://") || mVideoUrl.startsWith("https://"))) {
      throw new RuntimeException("Only http(s) urls are accepted for input video");
    }
    mRunningVideoUrl = null;
    mUseCache = false;
    Bundle iExtras = iIntent.getExtras();
    if (iExtras != null) {
      mUseCache = iExtras.getBoolean("usecache", false);
      mTitle = iExtras.getString("title");
    }
    if (mTitle == null) {
      mTitle = ""; // Empty title
    }
    RelativeLayout layout = new RelativeLayout(this);
    layout.setBackgroundColor(0xFF000000);
    setContentView(layout);
    JZUtils.hideStatusBar(this);
    JZUtils.setRequestedOrientation(this, Jzvd.FULLSCREEN_ORIENTATION);
    if (!mUseCache) {
      Jzvd.startFullscreenDirectly(this, JzvdFullscreenStd.class, mVideoUrl, mTitle);
      mRunningVideoUrl = mVideoUrl;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mUseCache) {
      registerReceiver(mHTTPDBR, new IntentFilter(EPNanoHTTPDService.ACTION_INITIAL_RESPONSE));
      registerReceiver(mHTTPDStoppedBR, new IntentFilter(EPNanoHTTPDService.ACTION_HTTPD_STOPPED));
      startService(new Intent(this, EPNanoHTTPDService.class));
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mUseCache) {
      unregisterReceiver(mHTTPDBR);
      unregisterReceiver(mHTTPDStoppedBR);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    Jzvd.resetAllVideos();
  }


  @Override
  public void onBackPressed() {
    if (Jzvd.backPress()) {
      return;
    }
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      finish();
      break;
    }
    return super.onOptionsItemSelected(item);
  }
}


