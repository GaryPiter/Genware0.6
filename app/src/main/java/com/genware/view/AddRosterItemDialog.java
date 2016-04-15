//package com.genware.view;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import com.genware.activity.MainActivity;
//import com.genware.exception.XXAdressMalformedException;
//import com.genware.service.XXService;
//import com.genware.util.PreferenceConstants;
//import com.genware.util.PreferenceUtils;
//import com.genware.util.T;
//import com.genware.util.XMPPHelper;
//import com.genware.xx.R;
//import com.genware.xx.SendFileActivity;
//
//public class AddRosterItemDialog extends AlertDialog implements
//		DialogInterface.OnClickListener, TextWatcher {
//
//	private MainActivity mMainActivity;
//	private XXService mXxService;
//
//	private Button okButton;
//	private EditText userInputField;
//	private EditText aliasInputField;
//	private GroupNameView mGroupNameView;
//
//	public AddRosterItemDialog(MainActivity mainActivity, XXService service) {
//		super(mainActivity);
//		mMainActivity = mainActivity;
//		mXxService = service;
//
//		setTitle(R.string.addFriend_Title);// 弹出对话框标题
//		setIcon(R.drawable.ic_launcher);// 自定义图标
//
//		LayoutInflater inflater = (LayoutInflater) mainActivity
//				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View group = inflater
//				.inflate(R.layout.addrosteritemdialog, null, false);
//		setView(group);
//
//		userInputField = (EditText) group
//				.findViewById(R.id.AddContact_EditTextField);
//		aliasInputField = (EditText) group
//				.findViewById(R.id.AddContactAlias_EditTextField);
//
//		mGroupNameView = (GroupNameView) group
//				.findViewById(R.id.AddRosterItem_GroupName);
//		mGroupNameView.setGroupList(mMainActivity.getRosterGroups());
//
//		setButton(BUTTON_POSITIVE, mainActivity.getString(android.R.string.ok),
//				this);
//		setButton(BUTTON_NEGATIVE,
//				mainActivity.getString(android.R.string.cancel),
//				(DialogInterface.OnClickListener) null);
//
//	}
//
//	public AddRosterItemDialog(MainActivity mainActivity, XXService service,
//			String jid) {
//		this(mainActivity, service);
//		userInputField.setText(jid);
//	}
//
//	public void onCreate(Bundle icicle) {
//		super.onCreate(icicle);
//
//		okButton = getButton(BUTTON_POSITIVE);
//		// afterTextChanged(userInputField.getText());
//
//		// userInputField.addTextChangedListener(this);
//	}
//
//	@SuppressLint("NewApi")
//	public void onClick(DialogInterface dialog, int which) {
//		String username = PreferenceUtils.getPrefString(mMainActivity,
//				PreferenceConstants.ACCOUNT, "");
//		if (userInputField.getText().toString().isEmpty()) {//如果添加为空提示
//			T.show(mMainActivity, "请输入账号", Toast.LENGTH_SHORT);
//		}else if(userInputField.getText().toString().equals(username)){
//			T.show(mMainActivity, "不能搜索自己", Toast.LENGTH_SHORT);
//		}else{
//			mXxService.sendMessage(userInputField.getText()// 添加好友先发送认证
//					.toString() + "@" + PreferenceConstants.IP, "iadd" + username);
//		}
//		// mXxService.addRosterItem(userInputField.getText()
//		// .toString()+PreferenceConstants.IP,
//		// aliasInputField.getText().toString(),
//		// mGroupNameView.getGroupName());
//	}
//
//	public void afterTextChanged(Editable s) {
//		try {
//			XMPPHelper.verifyJabberID(s);
//			okButton.setEnabled(true);
//			userInputField.setTextColor(XMPPHelper
//					.getEditTextColor(mMainActivity));
//		} catch (XXAdressMalformedException e) {
//			okButton.setEnabled(false);
//			userInputField.setTextColor(Color.RED);
//		}
//	}
//
//	public void beforeTextChanged(CharSequence s, int start, int count,
//			int after) {
//
//	}
//
//	public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//	}
//
//}
