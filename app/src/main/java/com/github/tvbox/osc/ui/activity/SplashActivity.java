package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.R;

/**
 * 开屏页
 */
public class SplashActivity extends Activity {

    private static final long SPLASH_DURATION = 2500;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mGoHomeRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mHandler.postDelayed(mGoHomeRunnable, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mGoHomeRunnable);
        super.onDestroy();
    }
}
