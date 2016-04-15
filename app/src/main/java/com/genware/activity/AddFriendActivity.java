package com.genware.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.genware.adapter.AddFriendAdapter;
import com.genware.app.Session;
import com.genware.app.XXApp;
import com.genware.service.XXService;
import com.genware.util.EncodingHandler;
import com.genware.util.PreferenceConstants;
import com.genware.util.PreferenceUtils;
import com.genware.util.T;

/**
 * 
 * @author 搜索好友并添加
 */
@SuppressLint("NewApi")
public class AddFriendActivity extends Activity implements OnClickListener {

	private XXService mXxService;
	private ImageView go_back;
	private EditText search_key;
	private Button btn_search;
	private ListView search_list;
	private String search_content;
	private List<Session> listUser;
	private Button ll_richscan_btn;// 扫一扫加好友
	private ImageView ll_richscan_img;// 二维码图片
	private AddFriendAdapter addFriendAdapter;
	private String userName;
	private String sendInviteUser = "";// 被邀请人

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				addFriendAdapter = new AddFriendAdapter(AddFriendActivity.this, listUser);
				search_list.setAdapter(addFriendAdapter);
				break;
			case -1:
				T.show(AddFriendActivity.this, "未查询到信息", Toast.LENGTH_SHORT);
				break;
			case 2:
				T.show(AddFriendActivity.this, "邀请已发送", Toast.LENGTH_SHORT);
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addrosteritemdialog);
		userName = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
		mXxService = MainActivity.mXxService;
		initView();
		initData();
	}

	private void initData() {
		search_list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
				final String you = listUser.get(position).getFrom();
				System.out.println("//---" + you);
				List<String> user = getChildrenRosters();
				// 判断已经是好友
				for (int i = 0; i < user.size(); i++) {
					System.out.println("//..." + user.get(i));
					if (user.get(i).equals(you)) {
						T.show(AddFriendActivity.this, "你们已经是好友了", Toast.LENGTH_SHORT);
						return;
					}
				}
				// 此处是判断是否重复邀请了某人，当然这个判断很简单，也是不科学的，至于怎么判断此人是否被你邀请过，大家自己可以想办法
				// 还有一点是，已经成为好友了，再去点邀请，这个也是需要判断的
				if (sendInviteUser.equals(you)) {
					T.show(AddFriendActivity.this, "你已经邀请过" + you + "了", Toast.LENGTH_SHORT);
					return;
				}
				showAddDialog(you);
			}
		});
		Log.i("userName2", userName);
		ll_richscan_img.setImageBitmap(createRichScan(userName));
		go_back.setOnClickListener(this);
		btn_search.setOnClickListener(this);
		ll_richscan_btn.setOnClickListener(this);
	}

	/**
	 * 初始化控件
	 */
	private void initView() {
		go_back = (ImageView) findViewById(R.id.img_back);// 返回
		search_key = (EditText) findViewById(R.id.search_key);
		btn_search = (Button) findViewById(R.id.btn_search);

		ll_richscan_btn = (Button) findViewById(R.id.ll_richscan_btn);// 扫一扫加好友
		ll_richscan_img = (ImageView) findViewById(R.id.ll_richscan_img);// 二维码
		// listview
		search_list = (ListView) findViewById(R.id.search_list);
		search_list.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.ll_richscan_btn:// 扫一扫加好友功能
			//打开扫描界面扫描条形码或二维码
			Intent openCameraIntent = new Intent(AddFriendActivity.this,CaptureActivity.class);
			startActivityForResult(openCameraIntent, 0);
			break;
		case R.id.img_back:// 返回
			this.finish();
			break;
		case R.id.btn_search:
			search_content = search_key.getText().toString();
			if (TextUtils.isEmpty(search_content)) {
				return;
			}
			searchUser();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//处理扫描结果（在界面上显示）
		if (resultCode == RESULT_OK) {
			Bundle bundle = data.getExtras();
			String scanResult = bundle.getString("result");
			Log.i("scanResult", scanResult+"---1");
			// 添加好友先发送认证
			mXxService.sendMessage(scanResult + "@" + PreferenceConstants.IP, "iadd" + userName);
			mHandler.sendEmptyMessage(2);
		}
	}

	/**
	 * @param 显示添加好友的窗口
	 */
	private void showAddDialog(final String you) {
		Builder dialog = new AlertDialog.Builder(AddFriendActivity.this);
		dialog.setItems(new String[] { "添加为好友" }, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int view) {
				// 添加好友先发送认证
				mXxService.sendMessage(you + "@" + PreferenceConstants.IP, "iadd" + userName);
				mHandler.sendEmptyMessage(2);
				finish();
				// 返回到
			}
		});
		dialog.create().show();
	}

	/**
	 * 搜索联系人
	 */
	private void searchUser() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				listUser = searchUsers(XXApp.mConn, search_content);
				if (listUser.size() > 0) {
					int k = -1;
					for (int i = 0; i < listUser.size(); i++) {
						if (listUser.get(i).getFrom().equals(userName)) {
							k = i;
						}
					}
					if (k > 0) {
						listUser.remove(k);
					}
					mHandler.sendEmptyMessage(1);
				} else {
					mHandler.sendEmptyMessage(-1);
				}
			}
		}).start();
	}

	/**
	 * 根据用户名生成对应的二维码
	 * 
	 * @param 用户名
	 * @return 生成图片
	 */
	public Bitmap createRichScan(String username) {
		final Resources r = getResources();
		Bitmap logoBmp = BitmapFactory.decodeResource(r, R.drawable.ic_launcher);// 二维码中间的log
		// 根据字符串生成二维码图片并显示在界面上，第二个参数为图片的大小（600*600）
		Bitmap qrCodeBitmap;
		try {
			qrCodeBitmap = EncodingHandler.createQRCode(username, 600);

			// ------------------添加logo部分------------------//

			// 二维码和logo合并
			Bitmap bitmap = Bitmap.createBitmap(qrCodeBitmap.getWidth(), qrCodeBitmap.getHeight(),
					qrCodeBitmap.getConfig());
			Canvas canvas = new Canvas(bitmap);
			// 二维码
			canvas.drawBitmap(qrCodeBitmap, 0, 0, null);
			// logo绘制在二维码中央
			// canvas.drawBitmap(logoBmp,
			// qrCodeBitmap.getWidth() / 2 - logoBmp.getWidth() / 2,
			// qrCodeBitmap.getHeight() / 2 - logoBmp.getHeight() / 2,
			// null);
			// ------------------添加logo部分------------------//
			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return logoBmp;

	}

	/**
	 * 查询用户
	 * 
	 * @param userName
	 * @return
	 * @throws XMPPException
	 */
	public static List<Session> searchUsers(XMPPConnection mXMPPConnection, String userName) {
		List<Session> listUser = new ArrayList<Session>();
		try {
			UserSearchManager search = new UserSearchManager(mXMPPConnection);
			// 此处一定要加上 search.
			Form searchForm = search.getSearchForm("search." + mXMPPConnection.getServiceName());
			Form answerForm = searchForm.createAnswerForm();
			answerForm.setAnswer("Username", true);
			answerForm.setAnswer("search", userName);
			ReportedData data = search.getSearchResults(answerForm, "search." + mXMPPConnection.getServiceName());
			Iterator<Row> it = data.getRows();
			Row row = null;
			while (it.hasNext()) {
				row = it.next();
				Session session = new Session();
				String name = row.getValues("Username").next().toString();
				session.setFrom(name);
				listUser.add(session);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return listUser;
	}

	/**
	 * @param groupname
	 * @return 查询所有的用户
	 */
	private Roster roster;
	private List<String> getChildrenRosters() {
		List<String> userJid = new ArrayList<String>();

		roster = XXApp.mConn.getRoster();
		// 获取好友列表
		Collection<RosterEntry> list = roster.getEntries();
		for (RosterEntry person : list) {
			String user = person.getUser();
			String[] nick = user.split("@");
			userJid.add(nick[0]);
		}
		return userJid;
	}

}
