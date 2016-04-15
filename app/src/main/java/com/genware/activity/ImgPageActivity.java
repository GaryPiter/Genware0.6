package com.genware.activity;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.polites.android.GestureImageView;
import com.genware.activity.R;
import com.genware.swipeback.SwipeBackActivity;
import com.nostra13.universalimageloader.core.assist.FailReason;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ImgPageActivity extends SwipeBackActivity {

	private GestureImageView img;
	private RelativeLayout loading_progress;
	private TextView loadingText;
	private ImageLoader imageLoader = ImageLoader.getInstance();
	DisplayImageOptions options;
	String url;
	private Bitmap bitmap;
	private TextView ivTitleName;
	private ImageView backImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_img_page);

		options = new DisplayImageOptions.Builder()
				.resetViewBeforeLoading(true).cacheOnDisc(true)
				.imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565).considerExifParams(true)
				.displayer(new FadeInBitmapDisplayer(0)).build();

		initView();
		initData();
	}

	private void initView() {
		// 显示返回按钮
		backImage = (ImageView) findViewById(R.id.image_back);
		backImage.setVisibility(View.VISIBLE);
		// 标题
		ivTitleName = (TextView) findViewById(R.id.ivTitleName);
		img = (GestureImageView) findViewById(R.id.img);
		loading_progress = (RelativeLayout) findViewById(R.id.loading_progress);
		loadingText = (TextView) findViewById(R.id.loadingText);
		bitmap = (Bitmap) getIntent().getParcelableExtra("bitmap");
	}

	private void initData() {
		ivTitleName.setVisibility(View.GONE);// 隐藏标题
		backImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		img.setImageBitmap(bitmap);
		loading_progress.setVisibility(View.GONE);
	}

	@Override
	protected void onResume() {
		super.onResume();
	};

	@Override
	protected void onPause() {
		super.onPause();
	};

}
