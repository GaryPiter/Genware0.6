package com.genware.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.genware.activity.FragmentCallBack;
import com.genware.activity.R;
import com.genware.db.ChatProvider;
import com.genware.db.ChatProvider.ChatConstants;
import com.genware.service.XXService;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.SharedTools;
import com.genware.util.T;
import com.genware.util.TimeUtil;
import com.genware.util.TranslationUtil;
import com.genware.util.XMPPHelper;
import com.genware.view.CustomDialog;

public class RecentChatAdapter extends SimpleCursorAdapter {
	private static final String SELECT = ChatConstants.DATE + " in (select max(" + ChatConstants.DATE + ") from "
			+ ChatProvider.TABLE_NAME + " group by " + ChatConstants.JID + " having count(*)>0)";// 查询合并重复jid字段的所有聊天对象
	private static final String[] FROM = new String[] { ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
			ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段
	private static final String SORT_ORDER = ChatConstants.DATE + " DESC";
	private ContentResolver mContentResolver;
	private LayoutInflater mLayoutInflater;
	private Activity mContext;
	private FragmentCallBack mFragmentCallBack;
	private XXService xxService;

	public RecentChatAdapter(Activity context) {
		super(context, 0, null, FROM, null);
		mContext = context;
		mFragmentCallBack = (FragmentCallBack) mContext;
		xxService = mFragmentCallBack.getService();
		mContentResolver = context.getContentResolver();
		mLayoutInflater = LayoutInflater.from(context);
	}

	public void requery() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, FROM, SELECT, null, SORT_ORDER);
		Cursor oldCursor = getCursor();
		changeCursor(cursor);
		mContext.stopManagingCursor(oldCursor);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor cursor = this.getCursor();
		cursor.moveToPosition(position);
		long dateMilliseconds = cursor.getLong(cursor.getColumnIndex(ChatProvider.ChatConstants.DATE));
		String date = TimeUtil.getChatTime(dateMilliseconds);
		String message = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		String jid = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.JID));

		String selection = ChatConstants.JID + " = '" + jid + "' AND " + ChatConstants.DIRECTION + " = "
				+ ChatConstants.INCOMING + " AND " + ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;// 新消息数量字段
		Cursor msgcursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				new String[] { "count(" + ChatConstants.PACKET_ID + ")", ChatConstants.DATE, ChatConstants.MESSAGE },
				selection, null, SORT_ORDER);
		msgcursor.moveToFirst();
		int count = msgcursor.getInt(0);
		ViewHolder viewHolder;
		if (convertView == null || convertView.getTag(R.drawable.ic_launcher + (int) dateMilliseconds) == null) {
			convertView = mLayoutInflater.inflate(R.layout.recent_listview_item, parent, false);
			viewHolder = buildHolder(convertView, jid);
			convertView.setTag(R.drawable.ic_launcher + (int) dateMilliseconds, viewHolder);
			convertView.setTag(R.string.app_name, R.drawable.ic_launcher + (int) dateMilliseconds);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher + (int) dateMilliseconds);
		}
		viewHolder.jidView.setText(XMPPHelper.splitJidAndServer(jid));
		String decide = message.substring(0, 4);
		String msg = message.substring(4, message.length());
		// 判定是否图片。
		if (decide.equals("imag")) {
			viewHolder.msgView.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, "[图片]", true));
		}

		// 判定是否不可打开文件。
		if (decide.equals("othe")) {
			viewHolder.msgView.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, "[文件]", true));
		}
		// 判定是否发送文本文件。
		if (decide.equals("mtxt")) {
			viewHolder.msgView.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, "[txt]", true));
		}

		// 判定是文本。
		if (decide.equals("text")) {
			viewHolder.msgView.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, msg, true));
		}

		// 判定是否添加好友。
		if (decide.equals("iadd")) {
			viewHolder.icon.setImageResource(R.drawable.iadd);// 将头像换为添加好友图片
			String username = PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "");
			viewHolder.msgView.setText(XMPPHelper.convertNormalStringToSpannableString(mContext, "你好！", true));
			if (!username.equals(msg)) {
				viewHolder.jidView.setText(msg + "请求添加你好友！");
				SharedTools.putStringValue(mContext, "user" + position, msg);
				viewHolder.front.setOnClickListener(new onClickDialog(position));
			} else {
				viewHolder.jidView.setText("你请求添加" + XMPPHelper.splitJidAndServer(jid) + "好友！");
				SharedTools.putStringValue(mContext, "user" + position, XMPPHelper.splitJidAndServer(jid));
				viewHolder.front.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						T.show(mContext, "请等待对方验证", Toast.LENGTH_SHORT);
					}
				});
			}
		}

		// 判定是否添加好友。
		if (decide.equals("yadd")) {
			viewHolder.icon.setImageResource(R.drawable.iadd);// 将头像换为添加好友图片
			String username = PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "");
			viewHolder.msgView.setVisibility(View.GONE);
			if (!username.equals(msg)) {
				viewHolder.jidView.setText(msg + "和你已成为好友！");
				SharedTools.putStringValue(mContext, "yuser" + position, msg);
				xxService.addRosterItem(msg + "@" + PreferenceConstants.IP, null, "我的好友");
				// mXxService.addRosterItem(userInputField.getText()
				// .toString()+PreferenceConstants.IP,
				// aliasInputField.getText().toString(),
				// mGroupNameView.getGroupName());
			} else {
				viewHolder.jidView.setText("你和" + XMPPHelper.splitJidAndServer(jid) + "已成为好友！");
				SharedTools.putStringValue(mContext, "yuser" + position, XMPPHelper.splitJidAndServer(jid));
				xxService.addRosterItem(XMPPHelper.splitJidAndServer(jid) + "@" + PreferenceConstants.IP, null, "我的好友");
			}

			viewHolder.front.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					T.show(mContext, "你们已是好友了，快去聊天吧！", 1);
				}
			});

		}

		viewHolder.dataView.setText(date);

		if (msgcursor.getInt(0) > 0) {
			Log.i("msgcursor.getInt(0)", "msgcursor.getInt(0)");
			viewHolder.msgView.setText(msgcursor.getString(msgcursor.getColumnIndex(ChatConstants.MESSAGE)));
			viewHolder.dataView
					.setText(TimeUtil.getChatTime(msgcursor.getLong(msgcursor.getColumnIndex(ChatConstants.DATE))));
			viewHolder.unReadView.setText(msgcursor.getString(0));
		}
		viewHolder.unReadView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
		viewHolder.unReadView.bringToFront();
		msgcursor.close();

		return convertView;
	}

	public class onClickDialog implements OnClickListener {
		private int position;

		public onClickDialog(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			SharedTools.putIntValue(mContext, "position", position);
			showAddDialog();
		}

	}

	/**
	 * 添加好友确认对话框
	 */
	protected void showAddDialog() {
		// 先new出一个监听器，设置好监听
		DialogInterface.OnClickListener dialogOnclicListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case Dialog.BUTTON_POSITIVE:// 取消
//					Toast.makeText(mContext, "取消" + which, Toast.LENGTH_SHORT).show();
					break;
				case Dialog.BUTTON_NEGATIVE:// 确定
					int position = SharedTools.getIntValue(mContext, "position", 0);
//					Toast.makeText(mContext, "确定" + which, Toast.LENGTH_SHORT).show();
					String user = SharedTools.getStringValue(mContext, "user" + position, "没有");
					// xxService.addRosterItem(user, user, "我的好友");
					xxService.sendMessage(user + "@" + PreferenceConstants.IP, "yadd" + user);// 确认添加返回让对方更新列表
					break;
				// case Dialog.BUTTON_NEUTRAL:
				// Toast.makeText(mContext, "忽略" + which,
				// Toast.LENGTH_SHORT).show();
				// break;
				}
			}
		};
		// dialog参数设置
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext); // 先得到构造器
		builder.setTitle("提示"); // 设置标题
		builder.setMessage("添加好友?"); // 设置内容
		builder.setIcon(R.drawable.ic_launcher);// 设置图标，图片id即可
		builder.setPositiveButton("取消", dialogOnclicListener);
		builder.setNegativeButton("确认", dialogOnclicListener);
		// builder.setNeutralButton("忽略", dialogOnclicListener); //3个的话中间的按钮
		builder.create().show();
	}

	private ViewHolder buildHolder(View convertView, final String jid) {
		ViewHolder holder = new ViewHolder();
		holder.front = (RelativeLayout) convertView.findViewById(R.id.front1);
		holder.icon = (ImageView) convertView.findViewById(R.id.icon);
		holder.jidView = (TextView) convertView.findViewById(R.id.recent_list_item_name);
		holder.dataView = (TextView) convertView.findViewById(R.id.recent_list_item_time);
		holder.msgView = (TextView) convertView.findViewById(R.id.recent_list_item_msg);
		holder.unReadView = (TextView) convertView.findViewById(R.id.unreadmsg);
		holder.deleteBtn = (Button) convertView.findViewById(R.id.recent_del_btn);
		holder.deleteBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				removeChatHistoryDialog(jid, XMPPHelper.splitJidAndServer(jid));
			}
		});
		return holder;
	}

	private static class ViewHolder {
		TextView jidView;
		TextView dataView;
		TextView msgView;
		TextView unReadView;
		ImageView icon;
		Button deleteBtn;
		RelativeLayout front;
	}

	void removeChatHistory(final String JID) {
		mContentResolver.delete(ChatProvider.CONTENT_URI, ChatProvider.ChatConstants.JID + " = ?",
				new String[] { JID });
	}

	void removeChatHistoryDialog(final String JID, final String userName) {
		new CustomDialog.Builder(mContext).setTitle(R.string.deleteChatHistory_title)
				.setMessage(mContext.getString(R.string.deleteChatHistory_text, userName, JID))
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						removeChatHistory(JID);
					}
				}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				}).create().show();
	}
}
