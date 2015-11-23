package com.mail163.email.activity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mail163.email.Logs;
import com.mail163.email.R;

public class AttachmentBrowser extends ListActivity implements OnClickListener {

	private static final String ACTION_ADD_ATTACHMENT = "com.android.email.intent.action.ADD_ATTACHMENT";
	private static final String ACTION_SAVE_ATTACHMENT = "com.android.email.intent.action.SAVE_ATTACHMENT";
	private static final String EXTRA_ADD_ATTACHMENT = "addAttachment";
	private static final String EXTRA_SAVE_ATTACHMENT = "saveAttachment";
	private static final String EXTRA_SAVE_ATTACHMENT_NAME = "attachmentName";
	
	private static final String STATE_CURRENT_FILE_PATH = "com.mail189.email.activity.AttachmentBrowser.state";

	private static final int DIALOG_REPLACE_ATTACHMENT = 1;

	// 用于显示在List上的文件名
	private ArrayList<File> fileListItem = null;

	// 分别记录根目录、最近的上一级目录和当前目录项
	private File rootFolder;
	private File lastFolder;
	private String currentPath;

	// 本视图中用到的控件对象
	private Button rootFolderBtn;
	private Button parentFolderBtn;
	private TextView currentPathTxt;
	private Button selectFolderBtn;

	private RelativeLayout mListFooterLayout;

	private Intent mIntent;
	private FileListAdapter adapter;
	private String saveAttachmentName;

	private SystemFileFilter mSystemFileFilter;

	public static void actionAddAttachment(Activity activity,
			String requestData, int requestCode) {
		Intent i = new Intent(activity, AttachmentBrowser.class);
		i.setAction(ACTION_ADD_ATTACHMENT);
		i.putExtra(EXTRA_ADD_ATTACHMENT, requestData);
		activity.startActivityForResult(i, requestCode);
	}

	public static void actionSaveAttachment(Activity activity,
			String attachmentName, String requestData, int requestCode) {
		Intent i = new Intent(activity, AttachmentBrowser.class);
		i.setAction(ACTION_SAVE_ATTACHMENT);
		i.putExtra(EXTRA_SAVE_ATTACHMENT_NAME, attachmentName);
		i.putExtra(EXTRA_SAVE_ATTACHMENT, requestData);
		activity.startActivityForResult(i, requestCode);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		currentPath = state.getString(STATE_CURRENT_FILE_PATH);
		showFolderItem(new File(currentPath));
		super.onRestoreInstanceState(state);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(STATE_CURRENT_FILE_PATH, currentPath);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_browser);

		rootFolderBtn = (Button) findViewById(R.id.root_folder_btn);
		rootFolderBtn.setOnClickListener(this);
		parentFolderBtn = (Button) findViewById(R.id.parent_folder_btn);
		parentFolderBtn.setOnClickListener(this);
		selectFolderBtn = (Button) findViewById(R.id.select_folder_btn);
		selectFolderBtn.setOnClickListener(this);
		mListFooterLayout = (RelativeLayout) findViewById(R.id.listFooter);

		this.getListView().setScrollbarFadingEnabled(true);

		mIntent = getIntent();
		
		if (mIntent.getBooleanExtra("add", false)) {
			mListFooterLayout.setVisibility(View.GONE);
			setTitle(R.string.ab_title_add_attachment);
		} else {
			setTitle(R.string.ab_title_save_attachment);
			saveAttachmentName = mIntent
					.getStringExtra(EXTRA_SAVE_ATTACHMENT_NAME);
		}

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			rootFolder = Environment.getExternalStorageDirectory();
		} else {
			rootFolder = Environment.getRootDirectory();
		}
//		rootFolder = Environment.getDataDirectory();
		
		lastFolder = rootFolder;
		mSystemFileFilter = new SystemFileFilter();
		currentPath = lastFolder.getAbsolutePath();
		currentPathTxt = (TextView) findViewById(R.id.current_path_txt);
		currentPathTxt.setText(currentPath);

		showFolderItem(lastFolder);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = fileListItem.get(position);
		if (file.isDirectory()) {
			showFolderItem(file);
		} else {
			if (mIntent.getBooleanExtra("add", false)) {
				Uri uri = Uri.fromFile(file);
				mIntent.setData(uri);
				setResult(RESULT_OK, mIntent);
				finish();
			} else {
				Toast.makeText(this, R.string.ab_toast_save_attachment_remind,
						Toast.LENGTH_SHORT).show();
				return;
			}
		}
	}

	private void showFolderItem(File folder) {

		// 保证返回到SD卡根目录时，不能再向上返回
		if (rootFolder.equals(folder)) {
			lastFolder = folder;
		} else {
			lastFolder = folder.getParentFile();
		}

		// 得到当前路径返回的文件和文件夹列表
		File[] files = folder.listFiles(mSystemFileFilter);

		// 用于显示在列表上的文件名
		fileListItem = new ArrayList<File>();

		for (File file : files) {
			fileListItem.add(file);
		}

		currentPath = folder.getAbsolutePath();
		currentPathTxt.setText(currentPath);
		if (rootFolder.getAbsolutePath().equals(currentPath)) {
			parentFolderBtn.setEnabled(false);
		} else {
			parentFolderBtn.setEnabled(true);
		}

		// 设置列表监听器
		adapter = new FileListAdapter(this);
		setListAdapter(adapter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.root_folder_btn:
			if (rootFolder.getAbsolutePath().equals(currentPath)) {
				Toast.makeText(this, R.string.ab_toast_already_in_home_folder,
						Toast.LENGTH_SHORT).show();
			} else {
				showFolderItem(rootFolder);
			}
			break;
		case R.id.parent_folder_btn:
			showFolderItem(lastFolder);
			break;
		case R.id.select_folder_btn:
			File file = new File(currentPath.toString(), saveAttachmentName);
			if (file.exists()) {
				showDialog(DIALOG_REPLACE_ATTACHMENT);
			} else {
				mIntent.putExtra(EXTRA_SAVE_ATTACHMENT, currentPath.toString());
				setResult(RESULT_OK, mIntent);
				finish();
			}
			break;
		default:
			break;
		}

	}

	/**
	 * 自定义Adapter继承自BaseAdapter，实现特定的界面
	 */
	private class FileListAdapter extends BaseAdapter {

		// 将要显示的邮件以EmailListItem类的形式封装到ArrayList中
		private ArrayList<File> mFileList = fileListItem;

		private Context mContext;

		public FileListAdapter(Context context) {
			this.mContext = context;
		}

		@Override
		public int getCount() {
			return mFileList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			// 优化ListVie方法
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.file_list_item, null);
				FileListItemViewCache itemViewCache = new FileListItemViewCache();
				itemViewCache.iconImgView = (ImageView) convertView
						.findViewById(R.id.file_icon);
				itemViewCache.fileNameTxtview = (TextView) convertView
						.findViewById(R.id.file_name);
				convertView.setTag(itemViewCache);
			}
			FileListItemViewCache itemViewCache = (FileListItemViewCache) convertView
					.getTag();

			// 得到相应的文件对象
			File file = (File) getItem(position);

			// 根据文件和文件夹类型不同载入不同图标
			if (file.isDirectory()) {
				itemViewCache.iconImgView.setImageResource(R.drawable.folder);
			} else {
				itemViewCache.iconImgView.setImageResource(R.drawable.file);
			}
			// 设置要显示的文件名
			itemViewCache.fileNameTxtview.setText(file.getName());
			return convertView;
		}

	}

	/**
	 * 用于优化ListView的视图类
	 */
	static class FileListItemViewCache {
		public ImageView iconImgView;
		public TextView fileNameTxtview;
	}

	public class SystemFileFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			if (pathname.isDirectory()) {
				String folderName = pathname.getName().toLowerCase();
				if (folderName.startsWith(".")) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_REPLACE_ATTACHMENT:
			return new AlertDialog.Builder(AttachmentBrowser.this).setTitle(
					R.string.ab_dialog_title).setMessage(
					getString(R.string.ab_dialog_msg_replay_file_info,
							saveAttachmentName)).setPositiveButton(
					R.string.confirm, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							mIntent.putExtra(EXTRA_SAVE_ATTACHMENT, currentPath
									.toString());
							setResult(RESULT_OK, mIntent);
							finish();
						}
					}).setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					}).create();
		default:
			return null;
		}
	}

}
