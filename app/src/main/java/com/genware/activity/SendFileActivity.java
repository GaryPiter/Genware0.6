package com.genware.activity;

import java.io.File;
import java.util.ArrayList;

import com.genware.activity.R;
import com.genware.activity.R.id;
import com.genware.activity.R.layout;
import com.genware.activity.R.string;
import com.genware.adapter.FileAdapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class SendFileActivity extends ListActivity implements
		android.view.View.OnClickListener {
	private static final String ROOT_PATH = "/sdcard/";
	// 存储文件名称
	private ArrayList<String> names = null;
	// 存储文件路径
	private ArrayList<String> paths = null;
	private View view;
	private EditText editText;
	private ImageView image_back;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_file);
		initView();
		// 显示文件列表
		showFileDir(ROOT_PATH);
	}

	private void initView() {
		image_back = (ImageView) findViewById(R.id.image_back);
		image_back.setVisibility(View.VISIBLE);
		image_back.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_back:
			finish();
			break;
		default:
			break;
		}
	}

	@SuppressLint("NewApi")
	private void showFileDir(String path) {
		names = new ArrayList<String>();
		paths = new ArrayList<String>();
		File file = new File(path);
		File[] files = file.listFiles();
		// 如果当前目录不是根目录
		if (!ROOT_PATH.equals(path)) {
			if (file.getParent().equals("/")) {// 判定点击返回时是否返回到根木录。如果是则不添加返回按钮
				System.out.println("已经是根目录了!");
			} else {
				names.add("@1");
				paths.add(ROOT_PATH);
				System.out.println("ROOT_PATH------" + ROOT_PATH);
				names.add("@2");
				paths.add(file.getParent());
				System.out.println("file.getParent()------" + file.getParent());
			}
		}
		// 添加所有文件
		for (File f : files) {
			names.add(f.getName());
			paths.add(f.getPath());
		}
		this.setListAdapter(new FileAdapter(this, names, paths));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String path = paths.get(position);
		File file = new File(path);
		// 文件存在并可读
		if (file.exists() && file.canRead()) {
			if (file.isDirectory()) {
				// 显示子目录及文件
				showFileDir(path);
			} else {
				// 处理文件
				fileHandle(file);
			}
		}
		// 没有权限
		else {
			Resources res = getResources();
			new AlertDialog.Builder(this).setTitle("Message")
					.setMessage(res.getString(R.string.no_permission))
					.setPositiveButton("OK", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		}
		super.onListItemClick(l, v, position, id);
	}

	// 对文件进行增删改
	private void fileHandle(final File file) {
		OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 发送文件
				if (which == 0) {
					openFile(file);
				}
				// 修改文件名
				if (which == 1) {
					LayoutInflater factory = LayoutInflater
							.from(SendFileActivity.this);
					view = factory.inflate(R.layout.rename_dialog, null);
					editText = (EditText) view.findViewById(R.id.file_rename);
					editText.setText(file.getName());
					OnClickListener listener2 = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							String modifyName = editText.getText().toString();
							final String fpath = file.getParentFile().getPath();
							final File newFile = new File(fpath + "/"
									+ modifyName);
							if (newFile.exists()) {
								// 排除没有修改情况
								if (!modifyName.equals(file.getName())) {
									new AlertDialog.Builder(
											SendFileActivity.this)
											.setTitle("注意!")
											.setMessage("文件名已存在，是否覆盖？")
											.setPositiveButton(
													"确定",
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															if (file.renameTo(newFile)) {
																showFileDir(fpath);
																displayToast("重命名成功！");
															} else {
																displayToast("重命名失败！");
															}
														}
													})
											.setNegativeButton(
													"取消",
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
														}
													}).show();
								}
							} else {
								if (file.renameTo(newFile)) {
									showFileDir(fpath);
									displayToast("重命名成功！");
								} else {
									displayToast("重命名失败！");
								}
							}
						}
					};
//					AlertDialog renameDialog = new AlertDialog.Builder(
//							SendFileActivity.this).create();
//					renameDialog.setView(view);
//					renameDialog.setButton("确定", listener2);
//					renameDialog.setButton2("取消",
//							new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialog,
//										int which) {
//
//								}
//							});
//					renameDialog.show();
				}
				// 删除文件
				else {
					new AlertDialog.Builder(SendFileActivity.this)
							.setTitle("注意!")
							.setMessage("确定要删除此文件吗？")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (file.delete()) {
												// 更新文件列表
												showFileDir(file.getParent());
												displayToast("删除成功！");
											} else {
												displayToast("删除失败！");
											}
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				}
			}
		};
		// 选择文件时，弹出增删该操作选项对话框
		String[] menu = { "发送文件", "重命名", "删除文件" };
		new AlertDialog.Builder(SendFileActivity.this).setTitle("请选择要进行的操作!")
				.setItems(menu, listener)
				.setPositiveButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	// 发送文件
	private void openFile(File file) {
		// 数据是使用Intent返回
		Intent intent = new Intent();
		String fileName = file.getName();
		final String fpath = file.getParentFile().getPath();
		final File filePath = new File(fpath + "/" + fileName);

		// 文件扩展名
		String end = fileName.substring(fileName.lastIndexOf(".") + 1,
				fileName.length()).toLowerCase();
		// if (end.equals("m4a") || end.equals("mp3") || end.equals("wav")) {
		// } else if (end.equals("mp4") || end.equals("3gp")) {
		// } else
		if (end.equals("jpg") || end.equals("png") || end.equals("jpeg")// 是图片
				|| end.equals("bmp") || end.equals("gif")) {
			// 把返回数据存入Intent
			intent.putExtra("sdResult", "image");
		} else if (end.equals("txt")) {// 是文本
			// 把返回数据存入Intent
			intent.putExtra("sdResult", "txt");
		} else if (end.equals("m4a") || end.equals("mp3") || end.equals("wav")) {// 是音乐
			// 把返回数据存入Intent
			intent.putExtra("sdResult", "music");
		} else {// 如果无法直接打开，提示用邮箱发送
				// 把返回数据存入Intent
			intent.putExtra("sdResult", "other");
			intent.putExtra("fileName", fileName);

		}
		intent.putExtra("sdFile", filePath.toString());
		// 设置返回数据
		SendFileActivity.this.setResult(RESULT_OK, intent);
		// 关闭Activity
		SendFileActivity.this.finish();

	}

	private void displayToast(String message) {
		Toast.makeText(SendFileActivity.this, message, Toast.LENGTH_SHORT)
				.show();
	}

}