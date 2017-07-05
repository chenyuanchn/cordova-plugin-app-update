package com.cordova.appUpdate;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.cordova.CordovaInterface;

import __ANDROID_PACKAGE__.R;

public class UpdateManager {
	/*
	 * The version of the remote file formats <?xml version='1.0'
	 * encoding='utf-8'?> <update> <version>1</version> <name>name</name>
	 * <title>新版本</title> <description>检测到最新版本，请及时更新！</description>
	 * <url>http://xxx.xxx.xxx.xxx/xx/xx/xx.apk</url> </update>
	 */
	private String updateXmlUrl;

	private String package_name;
	private Resources resources;
	/* 下载中 */
	private static final int DOWNLOAD = 1;
	/* 下载结束 */
	private static final int DOWNLOAD_FINISH = 2;
	/* 提示 */
	private static final int TS = 3;
	/* 提示2 */
	private static final int TS2 = 4;
	/* 保存解析的XML信息 */
	HashMap<String, String> mHashMap;
	/* 下载保存路径 */
	private String mSavePath;
	/* 记录进度条数量 */
	// private int progress;
	/* 是否取消更新 */
	// private boolean cancelUpdate = false;

	private Context mContext;
	private CordovaInterface cordova;
	/* 更新进度条 */
	// private ProgressBar mProgress;
	// private Dialog mDownloadDialog;

	private NotificationManager notificationManager = null;
	private Notification notification = null;
	private RemoteViews contentView = null;
	private Intent updateIntent = null;
	private PendingIntent pendingIntent = null;

	private boolean runFlag = false;
	private boolean isCheckFlag = true;

	/** 通知栏按钮广播 */
	private ButtonBroadcastReceiver bReceiver;

	private HttpURLConnection conn = null;
	private FileOutputStream fos = null;
	private InputStream is = null;

	private IntentFilter intentFilter = null;
	protected static final String LOG_TAG = "UpdateApp";

	private Handler mHandler;

	public UpdateManager(CordovaInterface cordova, String updateUrl) {
		this.cordova = cordova;
		this.mContext = cordova.getActivity();
		this.updateXmlUrl = updateUrl;
		package_name = mContext.getPackageName();
		resources = mContext.getResources();
		initButtonReceiver();
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case TS:
					Toast toast = Toast.makeText(mContext, "已经是最新版本",
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					break;
				case TS2:
					Toast toast2 = Toast.makeText(mContext, "超时，请检查网络连接!",
							Toast.LENGTH_LONG);
					toast2.setGravity(Gravity.CENTER, 0, 0);
					toast2.show();
					break;
				// 正在下载
				case DOWNLOAD:
					// 设置进度条位置
					// contentView.setTextViewText(R.id.notificationPercent,
					// progress + "%");
					// contentView.setProgressBar(R.id.notificationProgress,
					// 100,
					// progress, false);
					// notificationManager.notify(0, notification);

					break;
				case DOWNLOAD_FINISH:
					// 安装文件
					// 下载完成，点击安装

					File apkfile = new File(mSavePath, mHashMap.get("name"));
					if (!apkfile.exists()) {
						return;
					}
					// 通过Intent安装APK文件
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setDataAndType(
							Uri.parse("file://" + apkfile.toString()),
							"application/vnd.android.package-archive");

					pendingIntent = PendingIntent.getActivity(mContext, 0,
							intent, 0);

					Notification.Builder builder1 = new Notification.Builder(mContext);
					builder1.setSmallIcon(R.mipmap.icon); //设置图标
//					builder1.setTicker("显示第二个通知"); 
					builder1.setContentTitle(mHashMap.get("name")); //设置标题
					builder1.setContentText("下载成功，点击安装"); //消息内容
//					builder1.setWhen(System.currentTimeMillis()); //发送时间
					builder1.setDefaults(Notification.DEFAULT_ALL); //设置默认的提示音，振动方式，灯光
					builder1.setAutoCancel(true);//打开程序后图标消失
//					Intent intent =new Intent (MainActivity.this,Center.class);
//					PendingIntent pendingIntent =PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
					builder1.setContentIntent(pendingIntent);
					Notification notification1 = builder1.build();
					notification1.flags |= Notification.FLAG_AUTO_CANCEL; // 点击清除按钮或点击通知后会自动消失
					notificationManager.notify(0, notification1); // 通过通知管理器发送通知
					
//					notification.setLatestEventInfo(mContext,
//							mHashMap.get("name"), "下载成功，点击安装", pendingIntent);
//					notification.flags |= Notification.FLAG_AUTO_CANCEL; // 点击清除按钮或点击通知后会自动消失
//					notificationManager.notify(0, notification);

					// notificationManager.cancel(0);
					// contentView.setTextViewText(R.id.notificationPercent,
					// "100%");
					// contentView.setProgressBar(R.id.notificationProgress,
					// 100,
					// progress, false);
					// contentView.setTextViewText(R.id.notificationTitle,
					// "下载完成");
					// notificationManager.notify(0, notification);
					// installApk();
					break;
				default:
					break;
				}
			}
		};
	}

	/** 带按钮的通知栏点击广播接收 */
	public void initButtonReceiver() {
		bReceiver = new ButtonBroadcastReceiver();
		intentFilter = new IntentFilter();
		intentFilter.addAction("com.notifications.intent.action.ButtonClick");
		mContext.registerReceiver(bReceiver, intentFilter);
	}

	/**
	 * 广播监听按钮点击事件
	 */
	public class ButtonBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals("com.notifications.intent.action.ButtonClick")) {
				// 通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
				int buttonId = intent.getIntExtra("ButtonId", 0);
				switch (buttonId) {
				case 1:
					runFlag = true;
					notificationManager.cancel(0);
					break;
				default:
					break;
				}
			}
		}
	}

	/**
	 * 检测软件更新
	 */
	public void checkUpdate() {
		Toast toast = Toast.makeText(mContext, "正在获取最新版本信息", Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();

		Runnable runnable = new Runnable() {
			public void run() {
				int serverInt = getServerVerInfo();
				Log.e(LOG_TAG, " &&&&&&&&&&&&&&&serverInt ：" + serverInt);		
				if (serverInt > 0) {
					int currentVerCode = getCurrentVerCode();
					if (serverInt > currentVerCode) {
						showNoticeDialog();
						isCheckFlag = false;
					} else {
						mHandler.sendEmptyMessage(TS);
						isCheckFlag = false;
						// Toast.makeText(mContext, "已经是最新版本",
						// Toast.LENGTH_LONG).show();
					}
				} else {
					mHandler.sendEmptyMessage(TS2);
					isCheckFlag = false;
					// Toast.makeText(mContext, "超时，请检查网络连接",
					// Toast.LENGTH_LONG).show();
				}
			}
		};
		this.cordova.getThreadPool().execute(runnable);
	}

	// 获取当前软件版本
	private int getCurrentVerCode() {
		int versionCode = 0;
		try {
			// 获取软件版本号，对应AndroidManifest.xml下android:versionCode
			Log.e(LOG_TAG, " &&&&&&&&&&&&&&&package_name ：" + package_name);
			versionCode = mContext.getPackageManager().getPackageInfo(
					package_name, 0).versionCode;
			Log.e(LOG_TAG, " &&&&&&&&&&&&&&&versionCode ：" + versionCode);		
		} catch (NameNotFoundException e) {
			Log.e(LOG_TAG, "获取应用当前版本代码versionCode异常：" + e.toString());
		}
		return versionCode;
	}

	// 获取服务器当前软件版本
	private int getServerVerInfo() {
		is = null;
		is = returnFileIS(updateXmlUrl);
		int versionCodeRemote = 0;
		// 解析XML文件。 由于XML文件比较小，因此使用DOM方式进行解析
		ParseXmlService service = new ParseXmlService();
		try {
			mHashMap = service.parseXml(is);
		} catch (Exception e) {
			Log.e(LOG_TAG, "获取服务器当前版本异常：" + e.toString());
		}
		if (null != mHashMap && null != mHashMap.get("version")) {
			versionCodeRemote = Integer.valueOf(mHashMap.get("version"));
			Log.e(LOG_TAG, " &&&&&&&&&&&&&&&versionCodeRemote ：" + versionCodeRemote);		
		}
		return versionCodeRemote;
	}

	/**
	 * 通过url返回文件
	 *
	 * @param path
	 * @return
	 */
	private InputStream returnFileIS(String path) {

		URL url = null;
		is = null;
		try {
			url = new URL(path);
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "通过url返回文件异常：" + e.toString());
		}
		try {
			conn = null;
			conn = (HttpURLConnection) url.openConnection();// 利用HttpURLConnection对象,我们可以从网络中获取网页数据.
			conn.setDoInput(true);
			conn.setConnectTimeout(5000);// 设置连接主机超时（单位：毫秒）
			conn.setReadTimeout(5000);// 设置从主机读取数据超时（单位：毫秒）
			conn.connect();
			is = conn.getInputStream(); // 得到网络返回的输入流

		} catch (IOException e) {
			Log.e(LOG_TAG, "通过url返回文件 IO异常：" + e.toString());
		} catch (NullPointerException e) {
			Log.e(LOG_TAG, "通过url返回文件 NullPointer异常：" + e.toString());
		}
		return is;
	}

	/**
	 * 显示软件更新对话框
	 */
	private void showNoticeDialog() {
		Runnable runnable = new Runnable() {
			public void run() {
				String titleTemp = "软件更新";
				String descriptionTemp = "检测到最新版本，请及时更新！";
				if (null != mHashMap && null != mHashMap.get("title")) {
					titleTemp = mHashMap.get("title");
				}
				if (null != mHashMap && null != mHashMap.get("description")) {
					descriptionTemp = mHashMap.get("description");
				}

				// 构造对话框
				AlertDialog.Builder builder = new Builder(mContext);
				builder.setTitle(titleTemp);
				// builder.setMessage(getString("soft_update_info")/*R.string.soft_update_info*/);
				builder.setMessage(descriptionTemp);// 从xml取出描述
				// 稍后更新
				builder.setPositiveButton("以后再说", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				// 更新
				builder.setNegativeButton("马上更新", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						ConnectivityManager connManager = (ConnectivityManager) mContext
								.getSystemService(mContext.CONNECTIVITY_SERVICE);
						NetworkInfo mWifi = connManager
								.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
						if (!mWifi.isConnected()) {
							showWifiDialog();
						} else {
							// 显示下载对话框
							showDownloadDialog();
						}
					}
				});
				Dialog noticeDialog = builder.create();
				noticeDialog.show();
			}
		};
		this.cordova.getActivity().runOnUiThread(runnable);
	}

	/**
	 * 显示软件更新对话框
	 */
	private void showWifiDialog() {
		Runnable runnable = new Runnable() {
			public void run() {
				// 构造对话框
				AlertDialog.Builder builder = new Builder(mContext);
				builder.setTitle("未连接wifi");
				builder.setMessage("请连接wifi进行更新下载");
				// 稍后更新
				builder.setPositiveButton("以后再说", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				// 更新
				builder.setNegativeButton("土豪不在乎", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// 显示下载对话框
						showDownloadDialog();
					}
				});
				Dialog noticeDialog = builder.create();
				noticeDialog.show();
			}
		};
		this.cordova.getActivity().runOnUiThread(runnable);
	}

	/**
	 * 显示软件下载对话框
	 */
	private void showDownloadDialog() {

		/*
		 * // 构造软件下载对话框 AlertDialog.Builder builder = new Builder(mContext);
		 * builder.setTitle("更新"); // 给下载对话框增加进度条 final LayoutInflater inflater
		 * = LayoutInflater.from(mContext); View v =
		 * inflater.inflate(getLayout("appupdate_progress"
		 * )R.layout.appupdate_progress, null);
		 * 
		 * ProgressBar mProgress = (ProgressBar)
		 * v.findViewById(getId("update_progress")R.id.update_progress);
		 * builder.setView(v); // 取消更新 builder.setNegativeButton("取消", new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(DialogInterface dialog, int which) {
		 * dialog.dismiss(); // 设置取消状态 // cancelUpdate = true; } }); AlertDialog
		 * mDownloadDialog = builder.create(); mDownloadDialog.show();
		 */

		notificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification();

		// 设置通知栏显示内容
		notification.icon = R.mipmap.icon;// 这个图标必须要设置，不然下面那个RemoteViews不起作用.
		// 这个参数是通知提示闪出来的值.
		notification.tickerText = "开始下载";

		contentView = new RemoteViews(mContext.getPackageName(),
				R.layout.notification_item);
		contentView.setTextViewText(R.id.notificationTitle,
				mHashMap.get("name"));
		contentView.setTextViewText(R.id.notificationPercent, "0%");
		contentView.setProgressBar(R.id.notificationProgress, 100, 0, false);

		// 点击的事件处理
		Intent buttonIntent = new Intent(
				"com.notifications.intent.action.ButtonClick");
		buttonIntent.putExtra("ButtonId", 1);
		// 这里加了广播，所及INTENT的必须用getBroadcast方法
		PendingIntent intent_click = PendingIntent.getBroadcast(mContext, 1,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		contentView.setOnClickPendingIntent(R.id.notificationDelete,
				intent_click);

		notification.contentView = contentView;

		updateIntent = new Intent(mContext, cordova.getClass());
		updateIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pendingIntent = PendingIntent.getActivity(mContext, 0, updateIntent, 0);

		notification.contentIntent = pendingIntent;

		notificationManager.notify(0, notification);
		// notificationManager.notify(notification_id, notification);

		// 下载文件
		downloadApk();
	}

	/**
	 * 下载apk文件
	 */
	private void downloadApk() {
		// 启动新线程下载软件
//		new Thread(new DownloadApkThread()).start();
		
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					// 获得存储卡的路径
					String sdpath = getDiskCacheDir(mContext) + "/";
					mSavePath = sdpath + "download";

					URL url = new URL(mHashMap.get("url"));
					// 创建连接
					conn = null;
					conn = (HttpURLConnection) url.openConnection();
					conn.connect();
					// 获取文件大小
					int length = conn.getContentLength();
					// 创建输入流
					is = null;
					is = conn.getInputStream();

					File file = new File(mSavePath);
					// 判断文件目录是否存在
					if (!file.exists()) {
						file.mkdir();
					}

					File apkFile = new File(mSavePath, mHashMap.get("name"));
					fos = null;
					fos = new FileOutputStream(apkFile);
					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					// do {
					int readsize = 0;
					int downloadCount = 0;// 已经下载好的大小
					int updateCount = 0;// 已经上传的文件大小
					int down_step = 1;// 提示step
					while (((readsize = is.read(buf)) != -1) && !runFlag) {
						// 写入文件
						fos.write(buf, 0, readsize);
						downloadCount += readsize;

						/**
						 * 每次增长2%
						 */
						if (updateCount == 0
								|| (downloadCount * 100 / length - down_step) >= updateCount) {
							updateCount += down_step;
							// 改变通知栏
							// notification.setLatestEventInfo(this, "正在下载...",
							// updateCount
							// + "%" + "", pendingIntent);
							contentView.setTextViewText(R.id.notificationPercent,
									updateCount + "%");
							contentView.setProgressBar(R.id.notificationProgress,
									100, updateCount, false);
							// show_view
							notificationManager.notify(0, notification);

						}

						// // 计算进度条位置
						// progress = (int) (((float) count / length) * 100);
						// // 更新进度
						// mHandler.sendEmptyMessage(DOWNLOAD);
						// if (readsize <= 0) {
						// // 下载完成
						//
						// mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
						// break;
						// }

					}
					if (conn != null) {
						conn.disconnect();
					}

					fos.close();
					is.close();
					// }
					if (!runFlag) {
						mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
					}
				} catch (MalformedURLException e) {
					Log.e(LOG_TAG, "下载文件线程中 MalformedURL异常：" + e.toString());
				} catch (IOException e) {
					Log.e(LOG_TAG, "下载文件线程中 IO异常：" + e.toString());
				}
				// 取消下载对话框显示
				// mDownloadDialog.dismiss();
			}
		};
		this.cordova.getThreadPool().execute(runnable);
	}

	/**
	 * 下载文件线程
	 */
//	private class DownloadApkThread implements Runnable {
//
//		@Override
//		public void run() {
//			try {
//				// 获得存储卡的路径
//				String sdpath = getDiskCacheDir(mContext) + "/";
//				mSavePath = sdpath + "download";
//
//				URL url = new URL(mHashMap.get("url"));
//				// 创建连接
//				conn = null;
//				conn = (HttpURLConnection) url.openConnection();
//				conn.connect();
//				// 获取文件大小
//				int length = conn.getContentLength();
//				// 创建输入流
//				is = null;
//				is = conn.getInputStream();
//
//				File file = new File(mSavePath);
//				// 判断文件目录是否存在
//				if (!file.exists()) {
//					file.mkdir();
//				}
//
//				File apkFile = new File(mSavePath, mHashMap.get("name"));
//				fos = null;
//				fos = new FileOutputStream(apkFile);
//				int count = 0;
//				// 缓存
//				byte buf[] = new byte[1024];
//				// 写入到文件中
//				// do {
//				int readsize = 0;
//				int downloadCount = 0;// 已经下载好的大小
//				int updateCount = 0;// 已经上传的文件大小
//				int down_step = 2;// 提示step
//				while (((readsize = is.read(buf)) != -1) && !runFlag) {
//					// 写入文件
//					fos.write(buf, 0, readsize);
//					downloadCount += readsize;
//
//					/**
//					 * 每次增长2%
//					 */
//					if (updateCount == 0
//							|| (downloadCount * 100 / length - down_step) >= updateCount) {
//						updateCount += down_step;
//						// 改变通知栏
//						// notification.setLatestEventInfo(this, "正在下载...",
//						// updateCount
//						// + "%" + "", pendingIntent);
//						contentView.setTextViewText(R.id.notificationPercent,
//								updateCount + "%");
//						contentView.setProgressBar(R.id.notificationProgress,
//								100, updateCount, false);
//						// show_view
//						notificationManager.notify(0, notification);
//
//					}
//
//					// // 计算进度条位置
//					// progress = (int) (((float) count / length) * 100);
//					// // 更新进度
//					// mHandler.sendEmptyMessage(DOWNLOAD);
//					// if (readsize <= 0) {
//					// // 下载完成
//					//
//					// mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
//					// break;
//					// }
//
//				}
//				if (conn != null) {
//					conn.disconnect();
//				}
//
//				fos.close();
//				is.close();
//				// }
//				if (!runFlag) {
//					mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
//				}
//			} catch (MalformedURLException e) {
//				Log.e(LOG_TAG, "下载文件线程中 MalformedURL异常：" + e.toString());
//			} catch (IOException e) {
//				Log.e(LOG_TAG, "下载文件线程中 IO异常：" + e.toString());
//			}
//			// 取消下载对话框显示
//			// mDownloadDialog.dismiss();
//		}
//	}

	private int getId(String name) {
		return resources.getIdentifier(name, "id", package_name);
	}

	private int getLayout(String name) {
		return resources.getIdentifier(name, "layout", package_name);
	}

	/**
	 * 安装APK文件
	 */
	private void installApk() {
		File apkfile = new File(mSavePath, mHashMap.get("name"));
		if (!apkfile.exists()) {
			return;
		}
		// 通过Intent安装APK文件
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
				"application/vnd.android.package-archive");
		mContext.startActivity(i);
	}

	/*
	 * 当SD卡存在或者SD卡不可被移除的时候， 就调用getExternalCacheDir()方法来获取缓存路径，
	 * 否则就调用getCacheDir()方法来获取缓存路径。 前者获取到的就是 /sdcard/Android/data/<application
	 * package>/cache 这个路径，而后者获取到的是 /data/data/<application package>/cache 这个路径。
	 */
	private String getDiskCacheDir(Context context) {
		String cachePath = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return cachePath;
	}

	public boolean isCheckFlag() {
		return isCheckFlag;
	}

	public void setCheckFlag(boolean isCheckFlag) {
		this.isCheckFlag = isCheckFlag;
	}
}