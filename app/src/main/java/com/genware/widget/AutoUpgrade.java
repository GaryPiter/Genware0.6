package com.genware.widget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import com.genware.activity.LoginActivity;
import com.genware.activity.MainActivity;
import com.genware.activity.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class AutoUpgrade {
	
	private static final String TAG = "AutoUpgrade";
	private static final String HOST = "www.iestool.com";
	private static final String HTTP = "http://";
	private static final String URL_SPLITTER = "/";
	private static final String URL_API_HOST = HTTP + HOST + URL_SPLITTER;
	//检查版本地址
	private static final String SERVER_UPDATE_VERSION_URL = URL_API_HOST+ "update/apk/IM/upgrade.xml";

	// 调用更新的Activity
	public Activity activity = null;
	// 当前版本号
	public int versionCode = 0;
	// 当前版本名称
	public String versionName = "";
	//服务器解析出来的版本信息
	private UpdataInfo info;
	
	private static final int UPDATA_CLIENT = 0x10;
	private static final int GET_UNDATAINFO_ERROR = 0x11;
	private static final int DOWN_ERROR = 0x12;
	
	
	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATA_CLIENT:
				//对话框通知用户升级程序 
				showUpdataDialog();
				break;
			case GET_UNDATAINFO_ERROR:
				//服务器超时 
				Toast.makeText(activity, activity.getResources().getString(R.string.str_get_updatainfo_error), 0).show();
				getMain();
				break;	
			case DOWN_ERROR:
				//下载apk失败
				Toast.makeText(activity, activity.getResources().getString(R.string.str_download_error), 0).show();
				LoginMain();
				break;	
			}
		}
	};
	
	
	/**
	 * 构造方法，获得当前版本信息
	 * 
	 * @param activity
	 */
	public AutoUpgrade(Activity activity) {
		this.activity = activity;
		// 获得当前版本信息
		try {
			// 获取应用包信息【getPackageName()是你当前类的包名，0代表是获取版本信息 】
			PackageInfo pinfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
			this.versionCode = pinfo.versionCode;
			this.versionName = pinfo.versionName;
			 Log.d(TAG, versionCode+"  "+versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 检测更新
	 * 从服务器获取xml解析并进行比对版本号 
	 */
	public void check() {

		// 检测网络
		if (isNetworkAvailable(this.activity) == false) {
			Log.d(TAG, "###网络未连接！");
			return;
		}
			Log.d(TAG, "网络已连接！");
		// 如果网络可用，检测新版本
		new Thread(new Runnable() {
			public void run() {
				try {
					//包装成url的对象 
					URL url = new URL(SERVER_UPDATE_VERSION_URL);
					HttpURLConnection conn =  (HttpURLConnection) url.openConnection(); 
					conn.setConnectTimeout(5000);
					InputStream is =conn.getInputStream(); 
					info =  UpdataInfoParser.getUpdataInfo(is);
					if(info.getVerCode() > versionCode){
						Log.d(TAG,"有新版本 ,提示用户升级 ");
						Message msg = new Message();
						msg.what = UPDATA_CLIENT;
						handler.sendMessage(msg);
					}else{
						Log.d(TAG,"已经是最新版本，无需升级");
						getMain();
					}
				} catch (Exception e) {
					// 待处理 
					Message msg = new Message();
					msg.what = GET_UNDATAINFO_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				} 
			}
		}).start();
			
	}
	
	/*
	 * 
	 * 弹出对话框通知用户更新程序 
	 * 
	 * 弹出对话框的步骤：
	 * 	1.创建alertDialog的builder.  
	 *	2.要给builder设置属性, 对话框的内容,样式,按钮
	 *	3.通过builder 创建一个对话框
	 *	4.对话框show()出来  
	 */
	protected void showUpdataDialog() {
		AlertDialog.Builder builer = new Builder(activity) ; 
		builer.setTitle(activity.getResources().getString(R.string.str_upgrade));
		builer.setMessage(MessageFormat.format(activity.getResources().getString(R.string.str_upgrade_message),
				info.getVerName(),info.getDescription()));
		//当点确定按钮时从服务器上下载 新的apk 然后安装 
		builer.setPositiveButton(R.string.ok, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				downLoadApk();
			}   
		});
		//当点取消按钮时进行登录
		builer.setNegativeButton(R.string.cancel, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				//进行主程序
				getMain();
			}   
		});
		AlertDialog dialog = builer.create();
		dialog.show();
	}
	
	/*
	 * 从服务器中下载APK
	 */
	protected void downLoadApk() {
		final ProgressDialog pd;	//进度条对话框
		
		pd = new  ProgressDialog(activity);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage(activity.getResources().getString(R.string.str_downloading));
		pd.show();
		new Thread(){
			@Override
			public void run() {
				try {
					File file = getFileFromServer(URL_API_HOST + info.getSourcePath() + info.getApkName(), pd);
					sleep(1000);
					installApk(activity,file);
					pd.dismiss(); //结束掉进度条对话框
				} catch (Exception e) {
					Message msg = new Message();
					msg.what = DOWN_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				}
			}}.start();
	}
	
	public static File getFileFromServer(String path, ProgressDialog pd) throws Exception{
		//如果相等的话表示当前的sdcard挂载在手机上并且是可用的
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			URL url = new URL(path);
			HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			//获取到文件的大小 
			pd.setMax(conn.getContentLength());
			InputStream is = conn.getInputStream();
			File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
			FileOutputStream fos = new FileOutputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			byte[] buffer = new byte[1024];
			int len ;
			int total=0;
			while((len =bis.read(buffer))!=-1){
				fos.write(buffer, 0, len);
				total+= len;
				//获取当前下载量
				pd.setProgress(total);
			}
			fos.close();
			bis.close();
			is.close();
			return file;
		}
		else{
			return null;
		}
	}
	
	/**
	 * 检测是否有可用网络
	 * 
	 * @param context
	 * @return 网络连接状态
	 */
	private boolean isNetworkAvailable(Context context) {
		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			// 获取网络信息
			NetworkInfo info = cm.getActiveNetworkInfo();
			// 返回检测的网络状态
			return (info != null && info.isConnected());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
     * 安装apk
     * @param url
     */
    private void installApk(Context context , File f){
        if (!f.exists()) {
            return;
        }   
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
	
    /* 
     * 进入程序的登录面 
     */  
    private void LoginMain(){  
        Intent intent = new Intent(activity,LoginActivity.class);  
        activity.startActivity(intent);  
        //结束掉当前的activity    
        activity.finish();  
    }  
    
    /* 
     * 进入程序的主界面 
     */  
    private void getMain(){  
        Intent intent = new Intent(activity,MainActivity.class);  
        activity.startActivity(intent);  
        //结束掉当前的activity    
        activity.finish();  
    }  

}
