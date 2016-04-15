package com.genware.activity;

import org.jivesoftware.smackx.muc.MultiUserChat;

import com.genware.util.GroupChatUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GroupChatActivity extends Activity implements OnClickListener {

	private Context mContext;
	private Button creatRoomBtn;
	private EditText roomID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_chat);
		mContext = GroupChatActivity.this;
		initView();
	}

	private void initView() {
		roomID=(EditText)findViewById(R.id.et_room_id);
		creatRoomBtn = (Button) findViewById(R.id.creat_btn_group);
		creatRoomBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.creat_btn_group:
			// 获取房间名称
			String room=roomID.getText().toString();
			final MultiUserChat muc = GroupChatUtils.getInstance().createRoom(room);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (muc != null) {
						GroupChatUtils.getInstance().setMuc(muc);
						Toast.makeText(mContext, "创建成功", Toast.LENGTH_SHORT).show();
						Intent intent = new Intent(mContext, RoomChatActivity.class);
						startActivity(intent);
						finish();
					} else
						Toast.makeText(mContext, "创建失败", Toast.LENGTH_SHORT).show();
				}
			});
			break;
		default:
			break;
		}
	}

}
