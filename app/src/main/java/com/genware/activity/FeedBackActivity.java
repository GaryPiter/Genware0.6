package com.genware.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.genware.activity.R;
import com.genware.swipeback.SwipeBackActivity;
import com.genware.util.T;

public class FeedBackActivity extends SwipeBackActivity implements OnClickListener {
	
	private EditText mFeedBackEt;
	private Button mSendBtn;
	private ImageView image_back;
	private TextView ivTitleName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_back_view);
		initView();
		initData();

	}

	private void initData() {
		ivTitleName.setText("反馈信息");
		image_back.setVisibility(View.VISIBLE);
		image_back.setOnClickListener(this);

		mSendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String content = mFeedBackEt.getText().toString();
				if (!TextUtils.isEmpty(content)) {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT, "推聊Android客户端 - 信息反馈");
					intent.putExtra(Intent.EXTRA_TEXT, content);
					intent.setData(Uri.parse("mailto:genware_ies@163.com"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					FeedBackActivity.this.startActivity(intent);
				} else {
					T.showShort(FeedBackActivity.this, "请输入一点点内容嘛！");
				}
			}
		});
	}

	private void initView() {
		image_back = (ImageView) findViewById(R.id.image_back);// 返回按钮
		ivTitleName = (TextView) findViewById(R.id.ivTitleName);// 标题
		mFeedBackEt = (EditText) findViewById(R.id.fee_back_edit);
		mSendBtn = (Button) findViewById(R.id.feed_back_btn);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_back:
			this.finish();
			break;
		default:
			break;
		}
	}
}
