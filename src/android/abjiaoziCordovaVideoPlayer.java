package com.aminbros.abjiaozi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.app.Activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class abjiaoziCordovaVideoPlayer extends CordovaPlugin {

  private static final String TAG = "abjiaoziCordovaVideoPlayer";

  private Activity mActivity;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    mActivity = cordova.getActivity();
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if (action.equals("startFullscreen")) {
        startFullscreen(args, callbackContext);
        return true;
      }
      return false; // invalid action
    } catch (Exception ex) {
          
      callbackContext.error(ex.getMessage());
    }
    return true;
  }

  private void startFullscreen (JSONArray args, CallbackContext callback) throws JSONException {
    final String videoUrl = args.getString(0);
    final String title = args.getString(1);
    boolean _useCache = false;
    JSONObject opts = args.length() > 2 ? args.getJSONObject(2) : null;
    if (opts != null) {
      if (opts.has("usecache")) {
        _useCache = opts.getBoolean("usecache");
      }
    }
    final boolean useCache = _useCache;
    mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Intent intent = new Intent(mActivity, FullscreenVideoPlayerActivity.class);
          intent.setData(Uri.parse(videoUrl));
          Bundle gextras = new Bundle();
          gextras.putString("title", title);
          gextras.putBoolean("usecache", useCache);
          intent.putExtras(gextras);
          mActivity.startActivity(intent);
          callback.success();
        }
      });
  }
}
