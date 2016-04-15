package com.genware.activity;

import com.genware.activity.R;
import com.genware.service.IConnectionStatusCallback;
import com.genware.service.XXService;
import com.genware.util.DialogUtil;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.SharedTools;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends FragmentActivity implements OnClickListener, IConnectionStatusCallback {
	/** 配置文件标识 */
	public static final String LOGIN_ACTION = "com.way.xx.action.LOGIN";
	/**  */
	private static final int LOGIN_OUT_TIME = 0;
	private EditText mAccountEt, mPasswordEt;
	private ConnectionOutTimeProcess mLoginOutTimeProcess;
	private Dialog mLoginDialog;
	private XXService mXxService;
	private String mAccount;
	private String mPassword;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOGIN_OUT_TIME:
				Toast.makeText(LoginActivity.this, "网络连接超时，请重试", Toast.LENGTH_SHORT).show();
				if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running)
					mLoginOutTimeProcess.stop();
				if (mLoginDialog != null && mLoginDialog.isShowing())
					mLoginDialog.dismiss();
				break;
			}
		}
	};// end

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(LoginActivity.this, XXService.class));
		bindXMPPService();
		setContentView(R.layout.loginpage);
		initView();
	}

	private void initView() {
		mAccountEt = (EditText) findViewById(R.id.et_username_login);
		mPasswordEt = (EditText) findViewById(R.id.et_password_login);
		findViewById(R.id.btn_login).setOnClickListener(this);
		findViewById(R.id.btn_register_login).setOnClickListener(this);

		String account = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(this, PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(account)) {
			mAccountEt.setText(account);
		}
		if (!TextUtils.isEmpty(password)) {
			mPasswordEt.setText(password);
		}
		mLoginDialog = DialogUtil.getLoginDialog(this);
		mLoginOutTimeProcess = new ConnectionOutTimeProcess();
	}
	
	protected void onStart() {
		super.onStart();
		String username=SharedTools.getStringValue(getApplicationContext(), "username", "");//用户名
		String pwd=SharedTools.getStringValue(getApplicationContext(), "pwd", "");//密码
		if(!TextUtils.isEmpty(username)){
			mAccountEt.setText(username);
		}
		if(!TextUtils.isEmpty(pwd)){
			mPasswordEt.setText(pwd);
		}
	};

	private void bindXMPPService() {
		Intent mServiceIntent = new Intent(this, XXService.class);
		mServiceIntent.setAction(LOGIN_ACTION);
		bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_register_login:
			startActivity(new Intent(this, RegisterActivity.class));
			break;
		case R.id.btn_login:
			// 清空数据库？
			mAccount = mAccountEt.getText().toString();
			mPassword = mPasswordEt.getText().toString();
			// mAccount = splitAndSaveServer(mAccount);
			if (TextUtils.isEmpty(mAccount)) {
				Toast.makeText(LoginActivity.this, "请输入帐号", Toast.LENGTH_SHORT).show();
				return;
			}
			if (TextUtils.isEmpty(mPassword)) {
				Toast.makeText(LoginActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
				return;
			}
			if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running) {
				mLoginOutTimeProcess.start();
			}
			if (mLoginDialog != null && !mLoginDialog.isShowing()) {
				mLoginDialog.show();
			}
			if (mXxService != null) {
				mXxService.Login(mAccount, mPassword);
			}
			break;
		}
	}

	/**
	 * @param account
	 * @return 通过邮件发送错误日志
	 */
	private String splitAndSaveServer(String account) {
		if (!account.contains("@")) {
			return account;
		}
		String customServer = PreferenceUtils.getPrefString(this, PreferenceConstants.CUSTOM_SERVER, "");
		String[] res = account.split("@");
		String userName = res[0];
		String server = res[1];
		// 发送邮件check for gmail.com and other google hosted jabber accounts
		if ("gmail.com".equals(server) || "googlemail.com".equals(server)
				|| PreferenceConstants.GMAIL_SERVER.equals(customServer)) {
			userName = account;
		}
		PreferenceUtils.setPrefString(this, PreferenceConstants.Server, server);
		return userName;
	}

	/**
	 * 建立连接
	 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// 开始连接xmpp服务器
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(LoginActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}
	};

	/**
	 * @author 登录超时处理线程
	 *
	 */
	class ConnectionOutTimeProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		ConnectionOutTimeProcess() {
		}

		public void run() {
			while (true) {
				if (!this.running)
					return;
				if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
					mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
				}
				try {
					Thread.sleep(10L);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		public void start() {
			this.thread = new Thread(this);
			this.running = true;
			this.startTime = System.currentTimeMillis();
			this.thread.start();
		}

		public void stop() {
			this.running = false;
			this.thread = null;
			this.startTime = 0L;
		}
	}

	/** 回调函数 */
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		if (mLoginDialog != null && mLoginDialog.isShowing())
			mLoginDialog.dismiss();
		if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
		if (connectedState == XXService.CONNECTED) {
			save2Preferences();
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} else if (connectedState == XXService.DISCONNECTED) {
			Toast.makeText(LoginActivity.this, "登录失败:" + reason, Toast.LENGTH_SHORT).show();
		}
	}

	private void save2Preferences() {
		// 帐号是一直保存的
		PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT, mAccount);
		// 记住密码
		PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD, mPassword);
		// 在线
		PreferenceUtils.setPrefString(this, PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 关闭服务连接
		unbindService(mServiceConnection);
		if (mLoginOutTimeProcess != null) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
	}

}
