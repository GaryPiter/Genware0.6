package com.genware.activity;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.genware.activity.R;
import com.genware.swipeback.SwipeBackActivity;

public class AboutActivity extends SwipeBackActivity implements OnClickListener {
	private TextView tv;
	private ImageView image_back;
	private TextView ivTitleName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		initView();
		initData();

		Linkify.addLinks(tv, Linkify.ALL);
	}

	private void initData() {
		ivTitleName.setText("关于");
		image_back.setVisibility(View.VISIBLE);
		image_back.setOnClickListener(this);
	}

	private void initView() {
		tv = (TextView) findViewById(R.id.app_information);
		image_back = (ImageView) findViewById(R.id.image_back);// 返回按钮
		ivTitleName = (TextView) findViewById(R.id.ivTitleName);// 标题
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
