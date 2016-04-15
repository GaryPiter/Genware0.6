package com.genware.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.T;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author 注册新用户
 * 
 */
public class RegisterActivity extends Activity implements OnClickListener {

	private EditText account, password, surePassword;
	private Button registerBtn;
	private String userName;
	private String userPassword;
	private String userPasswordRepet;
	private Context context = RegisterActivity.this;
	private ImageView image_back;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		initView();
	}

	/**
	 * 初始化控件
	 */
	private void initView() {
		account = (EditText) this.findViewById(R.id.account_rg);
		password = (EditText) this.findViewById(R.id.password_rg);
		surePassword = (EditText) this.findViewById(R.id.sure_password_rg);
		registerBtn = (Button) this.findViewById(R.id.register_btn);
		image_back = (ImageView) this.findViewById(R.id.image_back);
		image_back.setVisibility(View.VISIBLE);
		image_back.setOnClickListener(this);
		registerBtn.setOnClickListener(this);
		// 默认进入弹出软键盘
		account.setFocusable(true);
		account.setFocusableInTouchMode(true);
		account.requestFocus();
		InputMethodManager inputManager = (InputMethodManager) account.getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.showSoftInput(account, 0);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_back:
			this.finish();
			break;
		case R.id.register_btn:
			InputMethodManager inputMethodManager1 = (InputMethodManager) getSystemService(
					Context.INPUT_METHOD_SERVICE);
			inputMethodManager1.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);// 关闭软键盘
			// 对表单数据进行初始化
			userName = account.getText().toString().trim();
			userPassword = password.getText().toString().trim();
			userPasswordRepet = surePassword.getText().toString().trim();
			// 对得到的数据进行判断，如果为空，给出提示
			if ("".equals(userName) || "".equals(userPassword) || "".equals(userPasswordRepet)) {
				Toast.makeText(context, context.getResources().getString(R.string.reg_error_empty_form_message),
						Toast.LENGTH_LONG).show();
			} else if (!userPassword.equals(userPasswordRepet)) {
				Toast.makeText(context, context.getResources().getString(R.string.reg_error_psw_not_same),
						Toast.LENGTH_LONG).show();
			} else {
				// 调用注册线程
				new Thread(regRunnable).start();
			}
			break;
		}
	}

	/**
	 * 注册线程
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private final Timer returnLogintimer = new Timer();
	private Runnable regRunnable = new Runnable() {

		@Override
		public void run() {
			android.os.Message msg = android.os.Message.obtain();
			// 初始化ClientConService，用于注册connection
			try {
				ConnectionConfiguration config = new ConnectionConfiguration(PreferenceConstants.IP,
						PreferenceConstants.DEFAULT_PORT_INT);
				// 允许自动连接
				config.setReconnectionAllowed(true);
				// 允许登陆成功后更新在线状态
				config.setSendPresence(true);
				// 收到好友邀请后manual表示需要经过同意,accept_all表示不经同意自动为好友
				Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
				// config.setDebuggerEnabled(true);
				// // 关闭安全模式
				// config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
				// config.setSASLAuthenticationEnabled(false);// 安全验证
				XMPPConnection connection = new XMPPConnection(config);
				connection.connect();

				int result = register1(connection, userName, userPassword);
				// Map<String, String> attributes = new HashMap<String,
				// String>();
				// attributes.put("date", sdf.format(new Date()));
				// // 账户管理类注册
				// AccountManager accountmanger =
				// connection.getAccountManager();
				// if (regAccount(userName, userPassword, attributes,
				// accountmanger)) {
				// msg.obj = true;
				// } else {
				// msg.obj = false;
				// }
				regHandler.sendEmptyMessage(result);
			} catch (XMPPException e) {
				Log.e("异常信息", e.getMessage()+"");
				regHandler.sendEmptyMessage(4);
			}
		}
	};

	/**
	 * 注册
	 * 
	 * @param account
	 *            注册帐号
	 * @param password
	 *            注册密码
	 * @return 1、注册成功 0、服务器没有返回结果2、这个账号已经存在3、注册失败
	 */
	public static int register1(XMPPConnection mXMPPConnection, String account, String password) {
		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(mXMPPConnection.getServiceName());
		// 注意这里createAccount注册时，参数是UserName，不是jid，是"@"前面的部分。
		reg.setUsername(account);
		reg.setPassword(password);
		// 这边addAttribute不能为空，否则出错。所以做个标志是android手机创建的吧！！！！！
		reg.addAttribute("android", "geolo_createUser_android");
		PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = mXMPPConnection.createPacketCollector(filter);
		mXMPPConnection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		// Stop queuing results停止请求results（是否成功的结果）
		collector.cancel();
		if (result == null) {
			return 0;
		} else if (result.getType() == IQ.Type.RESULT) {
			return 1;
		} else {
			if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
				return 2;
			} else {
				return 3;
			}
		}
	}

	/**
	 * 注册结果Handler，更新UI提示
	 */
	private Handler regHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				T.showLong(context, "注册失败");
				break;
			case 1:
				T.showLong(context, "注册成功，请牢记您的账号和密码");
				// 启动定时器，两秒后返回登陆界面
				returnLogintimer.schedule(returnToLoginTask, 3000);
				// PreferencesUtils.putSharePre(mContext, "username", account);
				// PreferencesUtils.putSharePre(mContext, "pwd", password);
				// finish();
				break;
			case 2:
				T.showLong(context, "该昵称已被注册");
				break;
			case 3:
				T.showLong(context, "注册失败");
				break;
			case 4:
				T.showLong(context, "注册失败,请检查您的网络");
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 注册新用户
	 * 
	 * @param _username
	 *            用户名
	 * @param _password
	 *            密码
	 * @param attributes
	 *            附加值，比如邮箱等
	 * @return 注册是否成功
	 */
	public boolean regAccount(String _username, String _password, Map<String, String> attributes,
			AccountManager accountmanger) {
		// 注册消息返回信息，用于显示给用户的提示
		boolean regmsg = false;
		// 这里有点疑惑，这里使用AccountManger中的createAccount方法和使用Registration的区别是什么
		try {
			accountmanger.createAccount(_username, _password, attributes);
			regmsg = true;
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return regmsg;
	}

	/**
	 * 返回主界面定时任务
	 */
	TimerTask returnToLoginTask = new TimerTask() {
		@Override
		public void run() {
			// 启动登陆界面
			Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
//			SharedTools.putStringValue(getApplicationContext(), "username", userName);
//			SharedTools.putStringValue(getApplicationContext(), "pwd", userPassword);
			PreferenceUtils.setPrefString(context, PreferenceConstants.ACCOUNT, userName);
			PreferenceUtils.setPrefString(context, PreferenceConstants.PASSWORD, userPassword);
			startActivity(intent);
			// 销毁当前Activity
			RegisterActivity.this.finish();
		}
	};

}
