package com.genware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.genware.app.XXApp;

import android.util.Log;

public class GroupChatUtils {

	private final String CONFERENCE = "@conference.";
	private static GroupChatUtils instance;
	private MultiUserChat muc;

	public MultiUserChat getMuc() {
		return muc;
	}

	public void setMuc(MultiUserChat muc) {
		this.muc = muc;
	}
	
	/**
	 * @return 单例模式
	 */
	public static GroupChatUtils getInstance() {
		if (null == instance)
			instance = new GroupChatUtils();
		return instance;
	}

	/**
	 * @param roomName  房间名称     创建房间 
	 */
	public MultiUserChat createRoom(String user) {
		// 当前用户的连接和房间的JID
		MultiUserChat multiUserChat = new MultiUserChat(XXApp.mConn, user + CONFERENCE + XXApp.mConn.getServiceName());
		try {
			multiUserChat.create(user);
			Form configForm = multiUserChat.getConfigurationForm();
			Form submitForm = configForm.createAnswerForm();
			// 向要提交的表单添加默认答复
			for (Iterator<FormField> fields = configForm.getFields(); fields.hasNext();) {
				FormField formField = fields.next();
				if (!formField.getType().equals(FormField.TYPE_HIDDEN) && formField.getVariable() != null) {
					// 设置默认值作为答复
					submitForm.setDefaultAnswer(formField.getVariable());
				}
			}
			// 用户的ID
			List<String> owners = new ArrayList<String>();
			owners.add(XXApp.mConn.getUser());
			// 房间的名称
			submitForm.setAnswer("muc#roomconfig_roomowners", owners);
			// 只有注册的昵称才能进入房间
			submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
			// 允许加入的成员数
			submitForm.setAnswer("muc#roomconfig_maxusers", Arrays.asList("30"));
			// 设置为永久房间
			submitForm.setAnswer("muc#roomconfig_persistentroom", true);
			// 房间仅对成员开放
			submitForm.setAnswer("muc#roomconfig_membersonly", false);
			// 允许占有者邀请其他人
			submitForm.setAnswer("muc#roomconfig_allowinvites", true);
			// 进入是否需要密码
			// submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",true);
			// 允许使用者修改昵称
			submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
			// 登录房间对话
			submitForm.setAnswer("muc#roomconfig_enablelogging", true);
			// 完成房间的配置
			multiUserChat.sendConfigurationForm(submitForm);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return multiUserChat;
	}
	
	/**
	 * 加入聊天室
	 * 
	 * @param user
	 * @param pwd
	 *            会议室密码
	 * @param roomName
	 * @return
	 */
	public MultiUserChat joinRoom(String user, String pwd, String roomName) {
		MultiUserChat muc = new MultiUserChat(XXApp.mConn,
				roomName.contains(CONFERENCE) ? roomName : roomName + CONFERENCE + XXApp.mConn.getServiceName());
		DiscussionHistory history = new DiscussionHistory();
		history.setMaxStanzas(100);
		history.setSince(new Date(2014, 07, 31));
		try {
			muc.join(user, pwd, history, SmackConfiguration.getPacketReplyTimeout());
			Message msg = muc.nextMessage();
			if (null != msg){
//				SLog.i(tag, msg.toXML());
			}
			Message msg2 = muc.nextMessage(100);
			if (null != msg2){
//				SLog.i(tag, msg2.toXML());
			}
		} catch (XMPPException e) {
//			SLog.e(tag, " 加入 聊天室 出错");
			e.printStackTrace();
			return null;
		}
		return muc;
	}
	
	/**
	 * 获取会议室成员名字
	 * 
	 * @param muc
	 * @return
	 */
	public List<String> getMUCMembers(MultiUserChat muc) {
		List<String> members = new ArrayList<String>();
		Iterator<String> it = muc.getOccupants();
		while (it.hasNext()) {
			String name = StringUtils.parseResource(it.next());
			Log.i("成员名字", name);
			members.add(name);
		}
		return members;
	}

}
