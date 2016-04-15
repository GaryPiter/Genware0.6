package com.genware.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smackx.muc.MultiUserChat;

import com.genware.adapter.FaceAdapter;
import com.genware.adapter.FacePageAdeapter;
import com.genware.app.XXApp;
import com.genware.service.XXService;
import com.genware.util.GroupChatUtils;
import com.genware.util.T;
import com.genware.view.CirclePageIndicator;
import com.genware.xlistview.MsgListView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
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
import android.widget.TextView;

public class RoomChatActivity extends Activity implements OnClickListener, OnTouchListener {

	private Context context;
	private WindowManager.LayoutParams mWindowNanagerParams;
	private InputMethodManager mInputMethodManager;
	private TextView mTitleNameView;// 标题栏
	private List<String> mFaceMapKeys;// 表情对应的字符串数组
	private TextView chatImageBtn, chatFileBtn, chatCameraBtn;// 扩展功能
	private Button mSendMsgBtn;// 发送按钮
	private EditText mChatEditText;// 消息输入框
	private ImageButton moreBtn;// 更多选项button
	private ImageButton mFaceSwitchBtn;// 切换键盘和表情的button
	/**
	 * 表情控件
	 */
	private LinearLayout mFaceRoot;// 表情父容器
	private LinearLayout mMoreRoot;// 更多父容器
	private ViewPager mFaceViewPager;// 表情选择ViewPager
	private boolean mIsFaceShow = false;// 是否显示表情
	private boolean mIsMoreShow = false;// 是否显示更多
	private int mCurrentPage = 0;// 当前表情页
	private MsgListView mMsgListView;// 对话ListView
	/**
	 * 服务
	 */
	private XXService mXxService;
	private MultiUserChat muc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_room_chat);
		context = RoomChatActivity.this;
		initData();
		initView();
		initFacePage();
		setChatAdapter();
	}

	/**
	 * 将表情map的key保存在数组中
	 */
	private void initData() {
		Set<String> keySet = XXApp.getInstance().getFaceMap().keySet();
		mFaceMapKeys = new ArrayList<String>();
		mFaceMapKeys.addAll(keySet);
	}

	/**
	 * 控件初始化
	 */
	private void initView() {
		//设置标题
		mTitleNameView = (TextView) this.findViewById(R.id.ivTitleName);
		muc = GroupChatUtils.getInstance().getMuc();
		mTitleNameView.setText(muc.getRoom().toString());
		// 显示返回按钮
		ImageView image = (ImageView) this.findViewById(R.id.image_back);
		image.setVisibility(View.VISIBLE);
		image.setOnClickListener(this);
		// 发送扩展功能
		chatImageBtn = (TextView) this.findViewById(R.id.chat_image_btn);
		chatCameraBtn = (TextView) this.findViewById(R.id.chat_camera_btn);
		chatFileBtn = (TextView) this.findViewById(R.id.chat_file_btn);
		//
		mChatEditText = (EditText) this.findViewById(R.id.input);
		moreBtn = (ImageButton) this.findViewById(R.id.more_btn);
		mFaceSwitchBtn = (ImageButton) this.findViewById(R.id.face_switch_btn);
		mSendMsgBtn = (Button) this.findViewById(R.id.send);
		mFaceRoot = (LinearLayout) this.findViewById(R.id.face_ll);
		mMoreRoot = (LinearLayout) this.findViewById(R.id.more_ll);
		mFaceViewPager = (ViewPager) this.findViewById(R.id.face_pager);
		mChatEditText.setOnTouchListener(this);
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
		//按钮点击事件
		moreBtn.setOnClickListener(this);
		mFaceSwitchBtn.setOnClickListener(this);
		mSendMsgBtn.setOnClickListener(this);
		chatImageBtn.setOnClickListener(this);
		chatFileBtn.setOnClickListener(this);
		chatCameraBtn.setOnClickListener(this);
	}

	/**
	 * 适配器填充数据
	 */
	private void setChatAdapter() {
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_back:
			this.finish();
			break;
		case R.id.chat_camera_btn://照相
			break;
		case R.id.chat_file_btn://发文件
			break;
		case R.id.chat_image_btn://发图片
			break;
		case R.id.more_btn://扩展功能
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
		case R.id.face_switch_btn://表情
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
		case R.id.send:
			sendMessageIfNotNull();
			break;
		}
	}

	/**
	 * 发送消息，表情
	 */
	private void sendMessageIfNotNull() {
		if (mChatEditText.getText().length() >= 1) {
			if (mXxService != null) {
//				mXxService.sendMessage(mWithJabberID, "text" + mChatEditText.getText().toString());
				if (!mXxService.isAuthenticated()){
					T.showShort(this, "消息已经保存随后发送");
				}
			}
			mChatEditText.setText(null);
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
		}
		return false;
	}
	
	/**
	 * 初始化表情图像
	 */
	private void initFacePage() {
		List<View> lv = new ArrayList<View>();
		for (int i = 0; i < XXApp.NUM_PAGE; ++i){
			lv.add(getGridView(i));
		}
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

	/** 
	 * 加载表情
	 * @param i
	 * @return
	 */
	private GridView getGridView(int i) {
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
						// 将图片大小压缩,压缩后图片的宽和高以及kB大小均会变化
						Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, rawWidth, rawHeigh, matrix, true);
						ImageSpan imageSpan = new ImageSpan(context, newBitmap);
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

	/**
	 * @return 防止乱pageview乱滚动
	 */
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
	
	
}
