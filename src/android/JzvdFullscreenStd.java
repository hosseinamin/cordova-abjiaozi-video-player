package com.aminbros.abjiaozi;

import android.util.AttributeSet;
import android.content.Context;
import android.view.View;
import android.util.Log;

import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

public class JzvdFullscreenStd extends JzvdStd {

  public JzvdFullscreenStd (Context context) {
    super(context);
    fullscreenButton.setVisibility(View.GONE);
  }

  public JzvdFullscreenStd (Context context, AttributeSet attrs) {
    super(context, attrs);
    fullscreenButton.setVisibility(View.GONE);
  }

  @Override
  public void onClick(View v) {
    if (v == backButton || v == tinyBackImageView) { 
      if (getContext() instanceof FullscreenVideoPlayerActivity) {
        ((FullscreenVideoPlayerActivity)getContext()).finish();
        return;
      }
    }
    super.onClick(v);
  }

}
