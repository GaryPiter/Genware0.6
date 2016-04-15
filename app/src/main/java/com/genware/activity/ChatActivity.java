package com.genware.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.genware.adapter.ChatAdapter;
import com.genware.adapter.FaceAdapter;
import com.genware.adapter.FacePageAdeapter;
import com.genware.app.XXApp;
import com.genware.db.ChatProvider;
import com.genware.db.ChatProvider.ChatConstants;
import com.genware.db.RosterProvider;
import com.genware.service.IConnectionStatusCallback;
import com.genware.service.XXService;
import com.genware.swipeback.SwipeBackActivity;
import com.genware.util.BitmapUtil;
import com.genware.util.L;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.StatusMode;
import com.genware.util.T;
import com.genware.util.TranslationUtil;
import com.genware.util.XMPPHelper;
import com.genware.view.CirclePageIndicator;
import com.genware.xlistview.MsgListView;
import com.genware.xlistview.MsgListView.IXListViewListener;

import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ChatActivity extends SwipeBackActivity
		implements OnTouchListener, OnClickListener, IXListViewListener, IConnectionStatusCallback {

	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class.getName() + ".username";// 昵称对应的key
	private MsgListView mMsgListView;// 对话ListView
	private ViewPager mFaceViewPager;// 表情选择ViewPager
	private int mCurrentPage = 0;// 当前表情页
	private boolean mIsFaceShow = false;// 是否显示表情
	private boolean mIsMoreShow = false;// 是否显示更多
	private ImageButton mFaceSwitchBtn;// 切换键盘和表情的button
	private ImageButton moreBtn;// 更多选项button
	private TextView mTitleNameView;// 标题栏
	private ImageView mTitleStatusView;
	private EditText mChatEditText;// 消息输入框
	private LinearLayout mFaceRoot;// 表情父容器
	private LinearLayout mMoreRoot;// 更多父容器
	private WindowManager.LayoutParams mWindowNanagerParams;
	private InputMethodManager mInputMethodManager;
	private Button mSendMsgBtn;// 发送按钮
	private List<String> mFaceMapKeys;// 表情对应的字符串数组
	private String mWithJabberID = null;// 当前聊天用户的ID
	private TextView chatImageBtn;// 相册选择按钮
	private TextView chatFileBtn;// 拍照选择按钮
	private TextView chatCameraBtn;// 文件选择按钮
	private Intent intent;

	public static final String IMAGE_UNSPECIFIED = "image/*";
	public static final int NONE = 0;
	public static final int PHOTOHRAPH = 1;// 拍照
	public static final int PHOTOZOOM = 2; // 缩放
	public static final int PHOTORESOULT = 3;// 结果
	public static final int SDRESOULT = 4;// 文件

	private static final String[] PROJECTION_FROM = new String[] { ChatProvider.ChatConstants._ID,
			ChatProvider.ChatConstants.DATE, ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.JID,
			ChatProvider.ChatConstants.MESSAGE, ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段

	private ContentObserver mContactObserver = new ContactObserver();// 联系人数据监听，主要是监听对方在线状态
	private XXService mXxService;// Main服务

	ServiceConnection mServiceConnection = new ServiceConnection() {
		private String usrName;

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(ChatActivity.this);
			// 如果没有连接上，则重新连接xmpp服务器
			if (!mXxService.isAuthenticated()) {
				usrName = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usrName, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}
	};

	/**
	 * 解绑服务
	 */
	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			L.e("Service wasn't bound!");
		}
	}

	/**
	 * 绑定服务
	 */
	private void bindXMPPService() {
		Intent mServiceIntent = new Intent(this, XXService.class);
		Uri chatURI = Uri.parse(mWithJabberID);
		mServiceIntent.setData(chatURI);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		initData();// 初始化数据
		initView();// 初始化view
		initFacePage();// 初始化表情
		setChatWindowAdapter();// 初始化对话数据
		getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI, true, mContactObserver);// 开始监听联系人数据库
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateContactStatus();// 更新联系人状态
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	// 查询联系人数据库字段
	private static final String[] STATUS_QUERY = new String[] { RosterProvider.RosterConstants.STATUS_MODE,
			RosterProvider.RosterConstants.STATUS_MESSAGE, };

	private void updateContactStatus() {
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI, STATUS_QUERY,
				RosterProvider.RosterConstants.JID + " = ?", new String[] { mWithJabberID }, null);
		int MODE_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			L.d("contact status changed: " + status_mode + " " + status_message);
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(getIntent().getStringExtra(INTENT_EXTRA_USERNAME)));
			int statusId = StatusMode.values()[status_mode].getDrawableId();
			if (statusId != -1) {// 如果对应离线状态
				// Drawable icon = getResources().getDrawable(statusId);
				// mTitleNameView.setCompoundDrawablesWithIntrinsicBounds(icon,
				// null,
				// null, null);
				mTitleStatusView.setImageResource(statusId);
				mTitleStatusView.setVisibility(View.VISIBLE);
			} else {
				mTitleStatusView.setVisibility(View.GONE);
			}
		}
		cursor.close();
	}

	/**
	 * 联系人数据库变化监听
	 * 
	 */
	private class ContactObserver extends ContentObserver {
		public ContactObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			L.d("ContactObserver.onChange: " + selfChange);
			updateContactStatus();// 联系人状态变化时，刷新界面
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (hasWindowFocus())
			unbindXMPPService();// 解绑服务
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// 窗口获取到焦点时绑定服务，失去焦点将解绑
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	private void initData() {
		mWithJabberID = getIntent().getDataString().toLowerCase();// 获取聊天对象的id
		Log.i("mWithJabberID", mWithJabberID);
		// 将表情map的key保存在数组中
		Set<String> keySet = XXApp.getInstance().getFaceMap().keySet();
		mFaceMapKeys = new ArrayList<String>();
		mFaceMapKeys.addAll(keySet);
	}

	/**
	 * 设置聊天的Adapter
	 */
	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + mWithJabberID + "'";
		// 异步查询数据库
		new AsyncQueryHandler(getContentResolver()) {

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				// ListAdapter adapter = new ChatWindowAdapter(cursor,
				// PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
				ListAdapter adapter = new ChatAdapter(ChatActivity.this, cursor, PROJECTION_FROM);
				mMsgListView.setAdapter(adapter);
				mMsgListView.setSelection(adapter.getCount() - 1);
			}

		}.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM, selection, null, null);
	}

	private void initView() {
		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mWindowNanagerParams = getWindow().getAttributes();
		mMsgListView = (MsgListView) findViewById(R.id.msg_listView);

		// 显示返回按钮
		ImageView image = (ImageView) findViewById(R.id.image_back);
		image.setVisibility(View.VISIBLE);
		image.setOnClickListener(this);

		// 触摸ListView隐藏表情和输入法
		mMsgListView.setOnTouchListener(this);
		mMsgListView.setPullLoadEnable(false);
		mMsgListView.setXListViewListener(this);

		// 发送扩展功能
		chatImageBtn = (TextView) findViewById(R.id.chat_image_btn);
		chatCameraBtn = (TextView) findViewById(R.id.chat_camera_btn);
		chatFileBtn = (TextView) findViewById(R.id.chat_file_btn);

		mSendMsgBtn = (Button) findViewById(R.id.send);
		moreBtn = (ImageButton) findViewById(R.id.more_btn);
		mFaceSwitchBtn = (ImageButton) findViewById(R.id.face_switch_btn);
		mChatEditText = (EditText) findViewById(R.id.input);
		mFaceRoot = (LinearLayout) findViewById(R.id.face_ll);
		mMoreRoot = (LinearLayout) findViewById(R.id.more_ll);
		mFaceViewPager = (ViewPager) findViewById(R.id.face_pager);
		mChatEditText.setOnTouchListener(this);
		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleStatusView = (ImageView) findViewById(R.id.ivTitleStatus);

		mChatEditText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mWindowNanagerParams.softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
							|| mIsFaceShow || mIsMoreShow) {
						mFaceRoot.setVisibility(View.GONE);
						mMoreRoot.setVisibility(View.GONE);
						mIsFaceShow = false;
						mIsMoreShow = false;
						return true;
					}
				}
				return false;
			}
		});
		moreBtn.setOnClickListener(this);
		mFaceSwitchBtn.setOnClickListener(this);
		mSendMsgBtn.setOnClickListener(this);
		chatImageBtn.setOnClickListener(this);
		chatCameraBtn.setOnClickListener(this);
		chatFileBtn.setOnClickListener(this);
	}

	@Override
	public void onRefresh() {
		mMsgListView.stopRefresh();
	}

	@Override
	public void onLoadMore() {
		// do nothing
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.face_switch_btn:
			if (!mIsFaceShow) {
				if (mIsMoreShow) {
					mIsMoreShow = false;
					mMoreRoot.setVisibility(View.GONE);
				}
				mInputMethodManager.hideSoftInputFromWindow(mChatEditText.getWindowToken(), 0);
				try {
					Thread.sleep(80);// 解决此时会黑一下屏幕的问题
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mFaceRoot.setVisibility(View.VISIBLE);
				mFaceSwitchBtn.setImageResource(R.drawable.aio_keyboard);
				mIsFaceShow = true;
			} else {
				mFaceRoot.setVisibility(View.GONE);
				mInputMethodManager.showSoftInput(mChatEditText, 0);
				mFaceSwitchBtn.setImageResource(R.drawable.qzone_edit_face_drawable);
				mIsFaceShow = false;
			}
			break;
		case R.id.more_btn:
			if (!mIsMoreShow) {
				mInputMethodManager.hideSoftInputFromWindow(mChatEditText.getWindowToken(), 0);
				if (mIsFaceShow) {
					mFaceSwitchBtn.setImageResource(R.drawable.qzone_edit_face_drawable);
					mIsFaceShow = false;
					mFaceRoot.setVisibility(View.GONE);
				}
				try {
					Thread.sleep(80);// 解决此时会黑一下屏幕的问题
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mMoreRoot.setVisibility(View.VISIBLE);
				mIsMoreShow = true;
			} else {
				mMoreRoot.setVisibility(View.GONE);
				mInputMethodManager.showSoftInput(mChatEditText, 0);
				mIsMoreShow = false;
			}
			break;
		case R.id.send:
			sendMessageIfNotNull();// 发送聊天、表情
			break;
		case R.id.chat_image_btn:// 发送图片<本地>
			sendImageIfNotNull();
			break;
		case R.id.chat_camera_btn:// 发送图片<相机>
			sendCamearIfNotNull();
			break;
		case R.id.chat_file_btn:// 发送文件
			sendFileIfNotNull();
			break;
		case R.id.image_back:
			this.finish();
			break;
		default:
			break;
		}
	}

	/**
	 * 发送文件
	 */
	private void sendFileIfNotNull() {
		Intent intent = new Intent(this, SendFileActivity.class);// 先跳转到文件选择界面
		startActivityForResult(intent, SDRESOULT);// 加参数获取返回选择文件路径
	}

	/**
	 * 发送拍照图片
	 */
	private void sendCamearIfNotNull() {
		intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, PHOTOHRAPH);
	}

	/**
	 * 发送本地图片
	 */
	private void sendImageIfNotNull() {
		Intent intent;
		if (Build.VERSION.SDK_INT < 19) {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType(IMAGE_UNSPECIFIED);
		} else {
			intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		}
		startActivityForResult(intent, PHOTOZOOM);
		// intent = new Intent();
		// /* 开启Pictures画面Type设定为image */
		// intent.setType(IMAGE_UNSPECIFIED);
		// /* 使用Intent.ACTION_GET_CONTENT这个Action */
		// intent.setAction(Intent.ACTION_GET_CONTENT);
		// /* 取得相片后返回本画面 */
		// startActivityForResult(intent, PHOTOZOOM);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == NONE)
			return;
		// 拍照
		if (requestCode == PHOTOHRAPH) {
			String sdState = Environment.getExternalStorageState();
			if (!sdState.equals(Environment.MEDIA_MOUNTED)) {
				Log.i("内存卡为null", "sd card unmount");
				return;
			}
			new DateFormat();
			String name = DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";
			Bundle bundle = data.getExtras();
			// 获取相机返回的数据，并转换为图片格式
			Bitmap bitmap = (Bitmap) bundle.get("data");
			FileOutputStream fout = null;
			File file = new File("/sdcard/pintu/");
			file.mkdirs();
			String filename = file.getPath() + name;
			try {
				fout = new FileOutputStream(filename);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					fout.flush();
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Log.i("bitmap.getWidth()+bitmap.getHeight()",
					bitmap.getWidth() + "---www----hhhhh---" + bitmap.getHeight());
			String msg = TranslationUtil.convertIcon2String(bitmap);
			if (mXxService != null) {
				mXxService.sendMessage(mWithJabberID, "imag" + msg);
				if (!mXxService.isAuthenticated())
					T.showShort(this, "图片已经保存随后发送");
			}
		}
		if (data == null)
			return;
		// 读取相册缩放图片
		if (requestCode == PHOTOZOOM) {
			startPhotoZoom(data.getData());
		}

		// 处理结果
		if (requestCode == PHOTORESOULT) {
			Bundle extras = data.getExtras();
			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				// Bitmap photo =
				// BitmapFactory.decodeResource(this.getBaseContext().getResources(),
				// R.drawable.bubble_popup_arrow_down);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
																		// 100)压缩文件
				String msg = TranslationUtil.convertIcon2String(photo);
				if (mXxService != null) {
					mXxService.sendMessage(mWithJabberID, "imag" + msg);
					if (!mXxService.isAuthenticated())
						T.showShort(this, "图片已经保存随后发送");
				}

			}
		}
		// 读取相册缩放图片
		if (requestCode == SDRESOULT) {
			String result = data.getExtras().getString("sdResult");// 返回的文件类型
			String sdFile = data.getExtras().getString("sdFile");// 文件路径
			Log.i("sdresult", result);
			Log.i("sdFile", sdFile);
			if (result.equals("image")) {// 选择图片，调用发送图片方法
				Bitmap bitmap = BitmapUtil.convertToBitmap(sdFile);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
																			// 100)压缩文件
				String msg = TranslationUtil.convertIcon2String(bitmap);
				if (mXxService != null) {
					mXxService.sendMessage(mWithJabberID, "imag" + msg);
					if (!mXxService.isAuthenticated())
						T.showShort(this, "图片已经保存随后发送");
				}
			} else if (result.equals("txt")) {// 选择txt文本 因为对方显示图片所以发送一个新的拼接数据
				String txt = obtainTxt(sdFile);
				Log.i("txt", txt);
				if (mXxService != null) {
					mXxService.sendMessage(mWithJabberID, "mtxt" + txt);
					if (!mXxService.isAuthenticated())
						T.showShort(this, "消息已经保存随后发送");
				}
			} else if (result.equals("other")) {
				String other = obtainTxt(sdFile);
				String fileName = data.getExtras().getString("fileName");// 文件路径
				String username = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
				Log.i("other", other);
				Log.i("fileName", fileName);
				if (mXxService != null) {
					mXxService.sendMessage(mWithJabberID, "othe" + fileName + "/" + username + "/" + other);
					if (!mXxService.isAuthenticated())
						T.showShort(this, "消息已经保存随后发送");
				}

			}
			// else if (result.equals("music")) {// 选择音乐文本 因为对方显示图片所以发送一个新的拼接数据
			// Intent intent = new Intent("android.intent.action.VIEW");
			// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			// intent.putExtra ("oneshot",0);
			// intent.putExtra ("configchange",0);
			// Uri uri = Uri.fromFile(new File(sdFile));
			// intent.setDataAndType (uri, "audio/*");
			// this.startActivity(intent);
			// }
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 调用系统剪裁图片
	 */
	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, IMAGE_UNSPECIFIED);
		intent.putExtra("crop", "true");
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

		int width = wm.getDefaultDisplay().getWidth();
		int height = wm.getDefaultDisplay().getHeight();
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", width);
		intent.putExtra("aspectY", height);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra("outputX", 190);
		intent.putExtra("outputY", 336);
		intent.putExtra("return-data", true);
		startActivityForResult(intent, PHOTORESOULT);
	}

	/**
	 * @param txt文本编码格式
	 * @return
	 */
	private String obtainTxt(String path) {
		StringBuilder sb = new StringBuilder();
		// 判断是否有sdcard
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			try {
				// 获取SDcard对应的路径
				File file = new File(path);
				InputStream inputStream = new FileInputStream(file);
				int len = 0;
				byte[] buffer = new byte[1024];
				while ((len = inputStream.read(buffer)) != -1) {
					sb.append(new String(buffer, 0, len));
				}
				// 关闭流
				inputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/**
	 * 发送表情消息
	 */
	private void sendMessageIfNotNull() {
		if (mChatEditText.getText().length() >= 1) {
			if (mXxService != null) {
				Log.i("asssssssss", "as" + mChatEditText.getText().toString());
				mXxService.sendMessage(mWithJabberID, "text" + mChatEditText.getText().toString());
				if (!mXxService.isAuthenticated())
					T.showShort(this, "消息已经保存随后发送");
			}
			mChatEditText.setText(null);
			// mSendMsgBtn.setEnabled(false);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.msg_listView:
			mInputMethodManager.hideSoftInputFromWindow(mChatEditText.getWindowToken(), 0);
			mFaceSwitchBtn.setImageResource(R.drawable.qzone_edit_face_drawable);
			mFaceRoot.setVisibility(View.GONE);
			mIsFaceShow = false;
			mMoreRoot.setVisibility(View.GONE);
			mIsMoreShow = false;
			break;
		case R.id.input:
			mInputMethodManager.showSoftInput(mChatEditText, 0);
			mFaceSwitchBtn.setImageResource(R.drawable.qzone_edit_face_drawable);
			mFaceRoot.setVisibility(View.GONE);
			mIsFaceShow = false;
			mMoreRoot.setVisibility(View.GONE);
			mIsMoreShow = false;
			break;

		default:
			break;
		}
		return false;
	}

	private void initFacePage() {
		// TODO Auto-generated method stub
		List<View> lv = new ArrayList<View>();
		for (int i = 0; i < XXApp.NUM_PAGE; ++i)
			lv.add(getGridView(i));
		FacePageAdeapter adapter = new FacePageAdeapter(lv);
		mFaceViewPager.setAdapter(adapter);
		mFaceViewPager.setCurrentItem(mCurrentPage);
		CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mFaceViewPager);
		adapter.notifyDataSetChanged();
		mFaceRoot.setVisibility(View.GONE);
		indicator.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				mCurrentPage = arg0;
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// do nothing
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// do nothing
			}
		});

	}

	private GridView getGridView(int i) {
		// TODO Auto-generated method stub
		GridView gv = new GridView(this);
		gv.setNumColumns(7);
		gv.setSelector(new ColorDrawable(Color.TRANSPARENT));// 屏蔽GridView默认点击效果
		gv.setBackgroundColor(Color.TRANSPARENT);
		gv.setCacheColorHint(Color.TRANSPARENT);
		gv.setHorizontalSpacing(1);
		gv.setVerticalSpacing(1);
		gv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		gv.setGravity(Gravity.CENTER);
		gv.setAdapter(new FaceAdapter(this, i));
		gv.setOnTouchListener(forbidenScroll());
		gv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				if (arg2 == XXApp.NUM) {// 删除键的位置
					int selection = mChatEditText.getSelectionStart();
					String text = mChatEditText.getText().toString();
					if (selection > 0) {
						String text2 = text.substring(selection - 1);
						if ("]".equals(text2)) {
							int start = text.lastIndexOf("[");
							int end = selection;
							mChatEditText.getText().delete(start, end);
							return;
						}
						mChatEditText.getText().delete(selection - 1, selection);
					}
				} else {
					int count = mCurrentPage * XXApp.NUM + arg2;
					// 注释的部分，在EditText中显示字符串
					// String ori = msgEt.getText().toString();
					// int index = msgEt.getSelectionStart();
					// StringBuilder stringBuilder = new StringBuilder(ori);
					// stringBuilder.insert(index, keys.get(count));
					// msgEt.setText(stringBuilder.toString());
					// msgEt.setSelection(index + keys.get(count).length());

					// 下面这部分，在EditText中显示表情
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
							(Integer) XXApp.getInstance().getFaceMap().values().toArray()[count]);
					if (bitmap != null) {
						int rawHeigh = bitmap.getHeight();
						int rawWidth = bitmap.getHeight();
						int newHeight = 40;
						int newWidth = 40;
						// 计算缩放因子
						float heightScale = ((float) newHeight) / rawHeigh;
						float widthScale = ((float) newWidth) / rawWidth;
						// 新建立矩阵
						Matrix matrix = new Matrix();
						matrix.postScale(heightScale, widthScale);
						// 设置图片的旋转角度
						// matrix.postRotate(-30);
						// 设置图片的倾斜
						// matrix.postSkew(0.1f, 0.1f);
						// 将图片大小压缩
						// 压缩后图片的宽和高以及kB大小均会变化
						Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, rawWidth, rawHeigh, matrix, true);
						ImageSpan imageSpan = new ImageSpan(ChatActivity.this, newBitmap);
						String emojiStr = mFaceMapKeys.get(count);
						SpannableString spannableString = new SpannableString(emojiStr);
						spannableString.setSpan(imageSpan, emojiStr.indexOf('['), emojiStr.indexOf(']') + 1,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						mChatEditText.append(spannableString);
					} else {
						String ori = mChatEditText.getText().toString();
						int index = mChatEditText.getSelectionStart();
						StringBuilder stringBuilder = new StringBuilder(ori);
						stringBuilder.insert(index, mFaceMapKeys.get(count));
						mChatEditText.setText(stringBuilder.toString());
						mChatEditText.setSelection(index + mFaceMapKeys.get(count).length());
					}
				}
			}
		});
		return gv;
	}

	// 防止乱pageview乱滚动
	private OnTouchListener forbidenScroll() {
		return new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE) {
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {

	}

	/*
	 * 监听软键盘发送按钮
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			/* 隐藏软键盘 */
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputMethodManager.isActive()) {
				inputMethodManager.hideSoftInputFromWindow(ChatActivity.this.getCurrentFocus().getWindowToken(), 0);
			}

			sendMessageIfNotNull();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

}
