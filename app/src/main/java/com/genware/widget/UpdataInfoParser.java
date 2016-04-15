package com.genware.widget;

import java.io.InputStream;
import java.net.URLDecoder;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;

public class UpdataInfoParser {

	/*
	 * 用pull解析器解析服务器返回的xml文件 (xml封装了版本号)
	 */
	public static UpdataInfo getUpdataInfo(InputStream is) throws Exception {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(is, "utf-8");// 设置解析的数据源
		int type = parser.getEventType();
		UpdataInfo info = new UpdataInfo();// 实体
		while (type != XmlPullParser.END_DOCUMENT) {
			switch (type) {
			case XmlPullParser.START_TAG:
				if ("verCode".equals(parser.getName())) {
					info.setVerCode(Integer.parseInt(parser.nextText())); // 获取版本号
				} else if ("verName".equals(parser.getName())) {
					info.setVerName(parser.nextText()); // 获取要升级的APK文件
				} else if ("isdo".equals(parser.getName())) {
					info.setIsdo(Integer.parseInt(parser.nextText()));
				}else if ("sourcePath".equals(parser.getName())) {
					info.setSourcePath(parser.nextText()); 
				}else if ("apkName".equals(parser.getName())) {
					info.setApkName(parser.nextText());
				}else if ("description".equals(parser.getName())) {
					info.setDescription(URLDecoder.decode(parser.nextText(), "UTF-8")); //获取该文件的信息  
				}
				break;
			}
			type = parser.next();
		}
		return info;
	}
}
