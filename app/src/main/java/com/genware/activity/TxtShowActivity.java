package com.genware.activity;

import com.genware.activity.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class TxtShowActivity extends Activity {

	private ImageView backImage;
	private TextView ivTitleName;
	private TextView txt_show;
	private String txt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_txt_show);

		initView();
		
		initData();
	}

	private void initView() {
		// 显示返回按钮
		backImage = (ImageView) findViewById(R.id.image_back);
		backImage.setVisibility(View.VISIBLE);
		// 标题
		ivTitleName = (TextView) findViewById(R.id.ivTitleName);
		// 显示txt文本
		txt_show = (TextView) findViewById(R.id.txt_show);
		txt_show.setMovementMethod(ScrollingMovementMethod.getInstance());
		// 获取文本
		txt = (String) getIntent().getExtras().getString("txt");
	}

	private void initData() {
		txt_show.setText(txt);
		ivTitleName.setVisibility(View.GONE);// 隐藏标题
		backImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

}
