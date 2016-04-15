package com.genware.activity;

import com.genware.activity.R;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.widget.AutoUpgrade;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.TextView;

public class SplashActivity extends FragmentActivity {

	private Context mcontext;
	private Handler mHandler;
	private TextView tv_versions;
	private String password;

	private final static int MESSAGE_TASK_UPDATE = 1;
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_TASK_UPDATE:
				if (isNetworkAvailable(mcontext)) {
					// 检测更新
					AutoUpgrade update = new AutoUpgrade(SplashActivity.this);
					update.check();
				} else {
					// 无网络连接则启动主程序
					mHandler.postDelayed(gotoMainAct, 500);
				}
				break;
			}
			return false;
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		mHandler = new Handler();
		mcontext = SplashActivity.this;

		password = PreferenceUtils.getPrefString(this, PreferenceConstants.PASSWORD, "");
		//判断是否第一次登录
		if (!TextUtils.isEmpty(password)) {
//			mHandler.postDelayed(gotoMainAct, 1000);
			initView();
		} else {
			mHandler.postDelayed(gotoLoginAct, 1000);
		}
	}

	/**
	 * 检测版本更新
	 */
	private void initView() {
		tv_versions = (TextView) findViewById(R.id.version_name);
		try {
			// 显示版本号
			PackageManager pm = getPackageManager();
			PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);
			tv_versions.setText(pi.versionName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.sendEmptyMessageDelayed(MESSAGE_TASK_UPDATE, 1000); // 检测更新下载
	}

	/**
	 * 启动登录界面
	 */
	private Runnable gotoLoginAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, LoginActivity.class));
			finish();
		}
	};

	/**
	 * 启动主程序
	 */
	private Runnable gotoMainAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, MainActivity.class));
			finish();
		}
	};

	/**
	 * 检测是否有可用网络
	 * 
	 * @param context
	 * @return 网络连接状态
	 */
	private boolean isNetworkAvailable(Context context) {
		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			// 获取网络信息
			NetworkInfo info = cm.getActiveNetworkInfo();
			// 返回检测的网络状态
			return (info != null && info.isConnected());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
