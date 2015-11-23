package com.mail163.email.activity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import com.mail163.email.Email.Global;

public class Camera extends Activity {
	private static final int ATTACHMENT_CAMERA = 1;
	private File out;
	private Uri uri;
	private Intent rootIntent;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rootIntent =  getIntent();

		try {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, Activity.DEFAULT_KEYS_DIALER);
//			out = new File(Environment.getExternalStorageDirectory(),"camera.01.png");
//			uri = Uri.fromFile(out);
//			intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//			
//			startActivityForResult(Intent.createChooser(intent, "相机"),
//					ATTACHMENT_CAMERA );

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (null == data) {
			finish();
			return ;
		}
		Bundle extras = data.getExtras();
		if(extras == null){
			finish();
			return;
		}
		Bitmap bm = (Bitmap) extras.get("data");
		File file;
		// 是否存在SD卡
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			file = new File(Environment.getExternalStorageDirectory().getPath()+ Global.tempFile);
			
		} else {
			file = getFilesDir();
		}
		
		
		
		File myCaptureFile = null;
		try {
			myCaptureFile = File.createTempFile("camera", ".jpg", file);

			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(myCaptureFile));
			/* 采用压缩转档方法 */
			bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);

			/* 调用flush()方法，更新BufferStream */
			bos.flush();

			/* 结束OutputStream */
			bos.close();
		} catch (Exception e) { 
			e.printStackTrace();
		}
		if (myCaptureFile != null && myCaptureFile.exists()) {
			Uri uri = Uri.fromFile(myCaptureFile);
			rootIntent.setData(uri);
			setResult(RESULT_OK, rootIntent);
			finish();
		}
		
	}
//	protected void onActivityResult(int requestCode, int resultCode, Intent data){
//		try {
//			if (RESULT_OK == resultCode) {
//				switch (requestCode) {
//				case ATTACHMENT_CAMERA:
//					rootIntent.setData(uri);
//					setResult(RESULT_OK, rootIntent);
//					finish();
//					break;
//				}
//			}
//		} catch (Exception e) {
//			Log.d("TAG", "Exception");
//			finish();
//		}
//
//	}
}