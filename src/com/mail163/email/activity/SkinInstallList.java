package com.mail163.email.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.SkinNewList.AccountInfoHolder;
import com.mail163.email.bean.AppCategoryDto;
import com.mail163.email.util.Utiles;
import com.mail163.email.util.ZipFileUtils;

public class SkinInstallList extends ListActivity {
	private static final int DIALOG_BACK = 1;
	private LayoutInflater mInflater;

	private ListView mSkinInatallList;
	private SkinListAdapter mSkinListAdapter;
	String skinPath;
	String targetDir;
	/**
	 * 存放皮肤路径的动态数据
	 */
	private ArrayList<String> skinInfoList = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInflater = getLayoutInflater();
	
		setTitle(getResources().getString(R.string.skin_manage_menu));

		skinPath = Environment.getExternalStorageDirectory() + "/yimail/skin/";
		targetDir = Environment.getDataDirectory() + "/data/"
				+ getPackageName() + "/skin/";
		

		mSkinInatallList = getListView();
		mSkinListAdapter = new SkinListAdapter();
		mSkinInatallList.setAdapter(mSkinListAdapter);

		mSkinInatallList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (arg2 == 0) {// 默认皮肤
					String dir = mSkins.get(arg2).dir;
					Utiles.delFolder(dir);

				} else {
					ZipFileUtils.Unzip(skinPath + mSkins.get(arg2).dir,
							targetDir);
				}
				showDialog(DIALOG_BACK);
			}
		});
		
		if (Global.backDrawable == null) {
			Global.backDrawable = Utiles.fetchBackDrawable(this);
			setTheme(R.style.XTheme);
		}
		if (null != Global.backDrawable) {
			if(Global.skinName.contains(Global.whiteSkin)){
				setTheme(R.style.Default);
	        }else{
	        	setTheme(R.style.XTheme);
	        }
			mSkinInatallList.setBackgroundDrawable(Global.backDrawable);
		}

	}

	@Override
	public void onResume() {
		super.onResume();
        String mr = targetDir+"&默认背景&黑色";
        if(!skinInfoList.contains(mr)){
        	skinInfoList.add(mr);
        }
		getFile(skinPath);
		getSkinInfoList(skinInfoList);
		mSkinListAdapter.notifyDataSetChanged();
	}
	@Override
	public void onPause() {
		super.onPause();
	}
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_BACK:
			return createBackDialog();
		}
		return super.onCreateDialog(id);
	}

	private Dialog createBackDialog() {
		return new AlertDialog.Builder(this).setMessage(
				R.string.skin_install_success).setTitle(R.string.app_name)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								
								AccountFolderList.actionShowAccounts(SkinInstallList.this,true);
							}
						}).create();
	}

	/**
	 * 获得皮肤的详细信息
	 * 
	 * @param skinInfoList
	 * @return
	 */
	private void getSkinInfoList(ArrayList<String> skinInfoList) {
		if (skinInfoList == null || skinInfoList.size() == 0) {
			return;
		}

		Pattern pattern = Pattern.compile("[&]");
		int size = skinInfoList.size();
		for (int i = 0; i < size; i++) {
       
			String[] skinInfoArr = pattern
					.split(skinInfoList.get(i).toString());
			mSkinListAdapter.addSkinList(skinInfoArr);
		}
	}

	/**
	 * 读取文件 扫描的路径
	 */
	private void getFile(String path) {
		if (null == path) {
			return;
		}
		File file = new File(path);
		File[] fileArr = file.listFiles();
		if(fileArr == null){
			return;
		}
		for (File temp : fileArr) {
			if (temp.isDirectory()) {
				getFile(temp.getPath());
			} else {
				skinInfoList.add(ZipFileUtils.getSkinInfo(temp
						.getAbsolutePath()));
			}
		}
	}

	private ArrayList<SkinInfoHolder> mSkins = new ArrayList<SkinInfoHolder>();

	class SkinListAdapter extends BaseAdapter {
		public void addSkinList(String[] mSkinlist) {		
				SkinInfoHolder holder = getSkin(mSkinlist[1]);
				if (holder == null) {
					holder = new SkinInfoHolder();
					mSkins.add(holder);
				}
				holder.dir = mSkinlist[0];
				holder.skinName = mSkinlist[1];
		}

		public SkinInfoHolder getSkin(String skinName) {
			SkinInfoHolder skinHolder = null;
			for (int i = 0, count = getCount(); i < count; i++) {
				SkinInfoHolder holder = (SkinInfoHolder) getItem(i);
				if (holder.skinName.equals(skinName)) {
					skinHolder = holder;
				}
			}
			return skinHolder;
		}
		
		public int getCount() {
			return mSkins.size();
		}

		public Object getItem(int position) {
			return mSkins.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			SkinInfoHolder skiner = (SkinInfoHolder) getItem(position);

			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.skin_manage_item, null);
			}
			SkinViewHolder holder = (SkinViewHolder) view.getTag();

			if (holder == null) {
				holder = new SkinViewHolder();
				holder.skinNametv = (TextView) view
						.findViewById(R.id.skin_name);
				holder.icon = (View) view.findViewById(R.id.skin);
				holder.messagetv = (TextView) view.findViewById(R.id.message);
				view.setTag(holder);
			}
			holder.skinNametv.setText(skiner.skinName);
			holder.messagetv.setText(skiner.message);
			return view;
		}

	}

	static class SkinInfoHolder {
		String dir;
		long mId;
		String skinName;
		String message;
	}

	static class SkinViewHolder {
		View icon;
		TextView skinNametv;
		TextView messagetv;
	}
}
