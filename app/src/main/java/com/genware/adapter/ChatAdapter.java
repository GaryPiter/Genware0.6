package com.genware.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.genware.activity.ImgPageActivity;
import com.genware.activity.R;
import com.genware.activity.TxtShowActivity;
import com.genware.db.ChatProvider;
import com.genware.db.ChatProvider.ChatConstants;
import com.genware.util.L;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.T;
import com.genware.util.TimeUtil;
import com.genware.util.TranslationUtil;
import com.genware.util.XMPPHelper;

public class ChatAdapter extends SimpleCursorAdapter {

	private static final int DELAY_NEWMSG = 2000;
	private Context mContext;
	private LayoutInflater mInflater;

	public ChatAdapter(Context context, Cursor cursor, String[] from) {
		// super(context, android.R.layout.simple_list_item_1, cursor, from,
		// to);
		super(context, 0, cursor, from, null);
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor cursor = this.getCursor();
		cursor.moveToPosition(position);

		long dateMilliseconds = cursor.getLong(cursor.getColumnIndex(ChatProvider.ChatConstants.DATE));

		int _id = cursor.getInt(cursor.getColumnIndex(ChatProvider.ChatConstants._ID));
		String date = TimeUtil.getChatTime(dateMilliseconds);
		String message = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		int come = cursor.getInt(cursor.getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
		boolean from_me = (come == ChatConstants.OUTGOING);
		String jid = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.JID));
		int delivery_status = cursor.getInt(cursor.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));
		ViewHolder viewHolder;
		if (convertView == null || convertView.getTag(R.drawable.ic_launcher + come) == null) {
			if (come == ChatConstants.OUTGOING) {
				convertView = mInflater.inflate(R.layout.chat_item_right, parent, false);
			} else {
				convertView = mInflater.inflate(R.layout.chat_item_left, null);
			}
			viewHolder = buildHolder(convertView);
			convertView.setTag(R.drawable.ic_launcher + come, viewHolder);
			convertView.setTag(R.string.app_name, R.drawable.ic_launcher + come);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher + come);
		}

		if (!from_me && delivery_status == ChatConstants.DS_NEW) {
			markAsReadDelayed(_id, DELAY_NEWMSG);
		}

		bindViewData(viewHolder, date, from_me, jid, message, delivery_status);
		return convertView;
	}

	private void markAsReadDelayed(final int id, int delay) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				markAsRead(id);
			}
		}, delay);
	}

	/**
	 * 标记为已读消息
	 * 
	 * @param id
	 */
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + ChatProvider.TABLE_NAME + "/" + id);
		L.d("markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		mContext.getContentResolver().update(rowuri, values, null, null);
	}

	private void bindViewData(ViewHolder holder, String date, boolean from_me, String from, String message,
			int delivery_status) {
		holder.avatar.setBackgroundResource(R.drawable.login_default_avatar);// 设置头像

		if (from_me// 判定是否显示用户头像
				&& !PreferenceUtils.getPrefBoolean(mContext, PreferenceConstants.SHOW_MY_HEAD, true)) {
			holder.avatar.setVisibility(View.GONE);
		}

		holder.time.setText(date);// 设置时间

		String decide = message.substring(0, 4);
		String msg = message.substring(4, message.length());
		if (decide.equals("iadd")) {// 如果聊天内容为空隐藏对话
			holder.content.setText("Hello!");
		} else if (decide.equals("yadd")) {
			holder.content.setText("你好!");
		}
		// 判定是否图片。
		if (decide.equals("imag")) {
			holder.content.setVisibility(View.GONE);
			holder.imageview2.setVisibility(View.VISIBLE);
			Bitmap bitmap = TranslationUtil.convertString2Icon(msg);
			holder.imageview2.setImageBitmap(bitmap);
			holder.imageview2.setOnClickListener(new onClick(bitmap));
			return;
		}

		// 判定是否文本。
		if (decide.equals("text")) {
			holder.content.setVisibility(View.VISIBLE);
			holder.imageview2.setVisibility(View.GONE);
			holder.content.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, msg, false));
		}

		// 判定是否txt文件。
		if (decide.equals("mtxt")) {
			holder.content.setVisibility(View.GONE);
			holder.imageview2.setVisibility(View.VISIBLE);
			holder.imageview2.setImageResource(R.drawable.doc);
			holder.imageview2.setOnClickListener(new onClick(msg));
		}

		// 判定是不能打开文件。
		if (decide.equals("othe")) {
			String userName = PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "");
			int i1 = msg.indexOf("/");
			System.out.println(i1);
			String fileName = msg.substring(0, i1);// 文件名
			String fileMsg = msg.substring(i1 + 1, msg.length());
			int i2 = fileMsg.indexOf("/");
			String formName = fileMsg.substring(0, i2);// 发送者
			String fileData = fileMsg.substring(i2 + 1, fileMsg.length());// 文件内容
			holder.content.setVisibility(View.VISIBLE);
			holder.imageview2.setVisibility(View.GONE);
			if (formName.equals(userName)) {// 如果是本人则提示发送成功
				holder.content.setText("文件" + fileName + "发送成功");
			} else {
				Log.i("接收者", formName);
				SaveToSD(fileName, fileData);
				holder.content.setText("文件" + fileName + "存储成功");
			}
			// holder.imageview2.setOnClickListener(new onClick(msg));
		}

	}

	/**
	 * @param 文件名
	 * @param 内容
	 */
	public void SaveToSD(String fileName, String fileData) {
		// 使用SD卡记得加相应的权限 <配置清单中的两句>

		// 获取SDcard的储存路径
		File pathSD = Environment.getExternalStorageDirectory();
		File file = new File(pathSD, fileName);
		Log.e("路径", file.toString());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(fileData.getBytes());
			fos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private ViewHolder buildHolder(View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.content = (TextView) convertView.findViewById(R.id.textView2);
		holder.time = (TextView) convertView.findViewById(R.id.datetime);
		holder.avatar = (ImageView) convertView.findViewById(R.id.icon);
		holder.imageview2 = (ImageView) convertView.findViewById(R.id.imageview2);
		return holder;
	}

	private static class ViewHolder {
		TextView content;
		TextView time;
		ImageView avatar;
		ImageView imageview2;

	}

	/**
	 * 对聊天界面力不同图片显示的内容进行监听
	 */
	class onClick implements OnClickListener {
		private Bitmap bitmap;
		private String txt;
		private int i;

		public onClick(Bitmap bitmap) {
			this.bitmap = bitmap;
			this.i = 0;
		}

		public onClick(String txt) {
			this.txt = txt;
			this.i = 1;
		}

		@Override
		public void onClick(View v) {
			if (i == 0) {
				Intent intentImg = new Intent(mContext, ImgPageActivity.class);
				intentImg.putExtra("bitmap", bitmap);
				mContext.startActivity(intentImg);
			} else if (i == 1) {
				Log.i("txt", txt);
				Intent intentImg = new Intent(mContext, TxtShowActivity.class);
				intentImg.putExtra("txt", txt);
				mContext.startActivity(intentImg);
			}
		}

	}

}
