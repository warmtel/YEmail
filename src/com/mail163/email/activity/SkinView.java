package com.mail163.email.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.net.HttpConectorThread;
import com.mail163.email.util.Utiles;

public class SkinView extends Activity implements OnClickListener {
	private static final String tag = "SkinView";
	private static final int DIALOG_progress = 1;
	private static final int DIALOG_DOWNLOAD = 2;
	private static final int MAX_PROGRESS = 100;
	private ProgressDialog mUpdateSoftDialog;
	private ProgressDialog mProgressDialog;
	private String fileUrl = "/sdcard/yimail/skin/s5.zip";
	private String httpUrl = "http://192.168.8.75:89/skin_new.zip";

	private int mProgress;
	private int mprogressIncrement;
	private HttpConectorThread httpConectorThread;
	private static String EXTRA_BUNDLE = "BUNDLE";
	private static String EXTRA_SKINURI = "SKINURI";
	private static String EXTRA_SKINNAME = "SKINNAME";
	private static String EXTRA_SKINPATH = "SKINPATH";
	private static String EXTRA_SKINID = "SKINID";
	private Button btnDown;

	private RelativeLayout skinViewLayout;

	public static void actionSkinView(Context context, String uri,
			String skinName,String skinPath,long skinId) {
		Intent mIntent = new Intent(context, SkinView.class);
		Bundle mBundle = new Bundle();
		mBundle.putString(EXTRA_SKINURI, uri);
		mBundle.putString(EXTRA_SKINNAME, skinName);
		mBundle.putString(EXTRA_SKINPATH, skinPath);
		mBundle.putLong(EXTRA_SKINID, skinId);
		mIntent.putExtra(EXTRA_BUNDLE, mBundle);

		context.startActivity(mIntent);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			doProgress();
		}
	};

	public void doProgress() {
		if(!httpConectorThread.isStop){
			return;
		}
		if (mProgress >= MAX_PROGRESS) {
			mProgressDialog.dismiss();
			createSkinManageAlertDialog();
		} else {
			if (httpConectorThread.endPos != 0) {
				mProgress = (int) (httpConectorThread.startPos * 100 / (httpConectorThread.endPos));
			}

			if (mProgress > mprogressIncrement) {
				mProgressDialog.incrementProgressBy(1);
			}
			mprogressIncrement = mProgress;
			if (mProgress > 95) {
				mProgressDialog.setProgress(100);
			}
			mHandler.sendEmptyMessageDelayed(0, 100);

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.skin_view);
		btnDown = (Button) findViewById(R.id.download);

		skinViewLayout = (RelativeLayout) findViewById(R.id.skin_view);
		
		btnDown.setOnClickListener(this);
		Intent mIntent = getIntent();
		if (mIntent == null) {
			Toast.makeText(this, getString(R.string.skin_download_error), Toast.LENGTH_SHORT).show();
			finish();
		}
		Bundle mBundle = mIntent.getBundleExtra(EXTRA_BUNDLE);
		if(mBundle == null){
			return;
		}

		String title = mBundle.getString(EXTRA_SKINNAME);

		String uri = mBundle.getString(EXTRA_SKINURI);

		httpUrl = mBundle.getString(EXTRA_SKINPATH);

		long skinId = mBundle.getLong(EXTRA_SKINID,1);
		fileUrl = "/sdcard/yimail/skin/skin"+skinId+".zip";
		
		this.setTitle(title);
		new LoadImageTask().execute(uri);
	}

	private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_progress);
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bitmap = Utiles.getBitmapFromUrl(SkinView.this, params[0]);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			dismissDialog(DIALOG_progress);
			if (bitmap != null) {
//				skinView.setImageBitmap(bitmap);
				skinViewLayout.setBackgroundDrawable(new BitmapDrawable(bitmap));
			}
		}

	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_progress:
			return createProgressDialog();

		case DIALOG_DOWNLOAD:
			return onCreateDownLoadDialog();
		
		}
		
		return super.onCreateDialog(id);
	}
	private void createSkinManageAlertDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.skin_manage_menu).setMessage(
				getString(R.string.skin_download_success)).setPositiveButton(
				R.string.confirm, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		alert.create().show();

	}
	private Dialog createProgressDialog() {
		mUpdateSoftDialog = new ProgressDialog(this);
		mUpdateSoftDialog.setTitle(R.string.skin_manage_menu);

		return mUpdateSoftDialog;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.download) {
			doDownLoad();
		}

	}

	public void doDownLoad() {
		showDialog(DIALOG_DOWNLOAD);
		mProgress = 0;
		mProgressDialog.setProgress(0);
		mHandler.sendEmptyMessage(0);

		httpConectorThread = new HttpConectorThread(httpUrl, fileUrl);
		Thread httpThread = new Thread(httpConectorThread);
		httpThread.start();
	}

	protected Dialog onCreateDownLoadDialog() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(getString(R.string.skin_downloading));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax(MAX_PROGRESS);

		mProgressDialog.setButton2(getText(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						httpConectorThread.isStop = false;
						Utiles.delFile(fileUrl);
					}
				});
		return mProgressDialog;

	}
}
