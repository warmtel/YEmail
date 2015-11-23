package com.mail163.email.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.mail163.email.Email;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.bean.AppCategoryDto;
import com.mail163.email.bean.Page;
import com.mail163.email.net.PageSaxFeedParser;

public class SkinNewList extends ListActivity implements OnItemClickListener {
	private LayoutInflater mInflater;
	private SkinDownListAdapter mAdapter;
	private ProgressBar progress;
	private TextView loadView;
	private ListView mListView;
	private View mListFooterView;
	
	private MainListHandler mHandler = new MainListHandler();
	class MainListHandler extends Handler {
		private static final int MSG_PROGRESS = 2;
		private static final int MSG_DATA_CHANGED = 3;
		private static final int MSG_ADD_ICON = 4;
		private static final int MSG_NETERROR = 5;

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_PROGRESS:
				if (msg.arg1 == 1) {
					progress.setVisibility(View.VISIBLE);
					loadView.setVisibility(View.VISIBLE);
				} else {
					progress.setVisibility(View.INVISIBLE);
					loadView.setVisibility(View.INVISIBLE);
				}
				break;
			case MSG_DATA_CHANGED:
				mAdapter.notifyDataSetChanged();
				break;
			case MSG_ADD_ICON:
				HashMap<String, Bitmap> map = (HashMap<String, Bitmap>) ((Object[]) msg.obj)[0];
				mAdapter.AddIconItem(map);
				mAdapter.notifyDataSetChanged();
				break;
			case MSG_NETERROR:
				Toast.makeText(SkinNewList.this, getString(R.string.skin_download_error), Toast.LENGTH_SHORT).show();
				break;
			default:
				super.handleMessage(msg);
			}
		}

		public void progress(boolean progress) {
			android.os.Message msg = new android.os.Message();
			msg.what = MSG_PROGRESS;
			msg.arg1 = progress ? 1 : 0;
			sendMessage(msg);
		}

		public void dataChanged() {
			sendEmptyMessage(MSG_DATA_CHANGED);
		}
		
		public void addIconItems(HashMap<String, Bitmap> map) {
			android.os.Message msg = new android.os.Message();
			msg.what = MSG_ADD_ICON;
			msg.obj = new Object[] { map };
			sendMessage(msg);
		}
		public void doNetError() {
			sendEmptyMessage(MSG_NETERROR);
		}
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInflater = getLayoutInflater();
		mListView = getListView();
		mListView.setBackgroundColor(getResources().getColor(
				R.color.text_white));
		mListView.setCacheColorHint(getResources().getColor(
				R.color.background_cachhint));

		mListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
		mListView.setOnItemClickListener(this);
		mListView.setLongClickable(true);
		
		mListFooterView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
		     .inflate(R.layout.progressbar, mListView, false);
		mListView.addFooterView(mListFooterView, null, false);
		
		progress = (ProgressBar)mListFooterView.findViewById(R.id.progress);
		loadView = (TextView)mListFooterView.findViewById(R.id.loading_text);
		
		mAdapter = new SkinDownListAdapter();
		setListAdapter(mAdapter);
		
		new SkinDownTask().execute("");
	}
	@Override
	public void onResume() {
		super.onResume();
	}
	private class SkinDownTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			String feedUrl = Email.skinDownUri;
			try {
				mAdapter.listSceneSalesStarted();
				
				PageSaxFeedParser pageSaxFeedParser = new PageSaxFeedParser(feedUrl,SkinNewList.this.getApplication());
				Page<AppCategoryDto> salespage = pageSaxFeedParser.parse();
				ArrayList<AppCategoryDto> salesList = (ArrayList<AppCategoryDto>) salespage.getResultlist();
				mAdapter.listSceneSales(salesList);
				
				String iconPath = salespage.getIconpath();
				HashMap<String, Bitmap> map = com.mail163.email.util.FileUtils.DecompressesToBitmap(
						iconPath, SkinNewList.this);
				if(map.size()>0){
					mAdapter.listAddIconItem(map);
				}
				mAdapter.listSceneSalesFinish();

			} catch (Exception e) {
				e.printStackTrace();
				mAdapter.listSceneSalesFinish();
				if(mAdapter.mAccounts.size() <= 0){
				    mHandler.doNetError();
				}
			}
			return 1;
		}
	}

	class SkinDownListAdapter extends BaseAdapter {
		public ArrayList<AccountInfoHolder> mAccounts = new ArrayList<AccountInfoHolder>();
		private HashMap<String, Bitmap> mIconMap1 = new HashMap<String, Bitmap>();

		public void listSceneSalesStarted() {
			mHandler.progress(true);
		}

		public void listSceneSales(ArrayList<AppCategoryDto> saleslist) {
			for (AppCategoryDto sales : saleslist) {
				AccountInfoHolder holder = getAccount(sales.getName());
				if (holder == null) {
					holder = new AccountInfoHolder();
					mAccounts.add(holder);
				}
				holder.accountName = "下载次数:4586次";
				holder.niceName =  sales.getName();
				holder.path = sales.getPath();
				holder.url = sales.getUrl();
				holder.skinPath = sales.getSkinpath();
				holder.mId = Long.parseLong(sales.getId());

				mHandler.dataChanged();
			}
		}

		public void listSceneSalesFinish() {
			mHandler.progress(false);
		}

		public void listAddIconItem(HashMap<String, Bitmap> map) {
			mHandler.addIconItems(map);
		}

		public AccountInfoHolder getAccount(String account) {
			AccountInfoHolder accountHolder = null;
			for (int i = 0, count = getCount(); i < count; i++) {
				AccountInfoHolder holder = (AccountInfoHolder) getItem(i);
				if (holder.niceName.equals(account)) {
					accountHolder = holder;
				}
			}
			return accountHolder;
		}

		public int getCount() {
			return mAccounts.size();
		}

		public Object getItem(int position) {
			return mAccounts.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public void AddIconItem(HashMap<String, Bitmap> map) {
			mIconMap1.putAll(map);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			AccountInfoHolder accounder = (AccountInfoHolder) getItem(position);

			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.skin_down_list_item, null);
			}
			AccountViewHolder holder = (AccountViewHolder) view.getTag();

			if (holder == null) {
				holder = new AccountViewHolder();
				holder.skinName = (TextView) view.findViewById(R.id.skin_name);
				holder.icon = (View) view.findViewById(R.id.chip);
				holder.skinMessage = (TextView) view.findViewById(R.id.skin_message);

				if (position % 2 == 1) {
					view.setBackgroundColor(Color.rgb(12 * 16 + 11,
							14 * 16 + 3, 15 * 16 + 15));
				} else {
					view.setBackgroundColor(Color.rgb(14 * 16 + 10,
							15 * 16 + 4, 15 * 16 + 14));
				}
				view.setTag(holder);
			}

			holder.skinName.setText(accounder.niceName);
			holder.skinMessage.setText(accounder.accountName);

			Bitmap tempIcon = mIconMap1.get(accounder.path);

			if (tempIcon == null) {
			} else {
				Drawable drawable = new BitmapDrawable(tempIcon);
				holder.icon.setBackgroundDrawable(drawable);
			}
			return view;
		}
	}

	static class AccountInfoHolder {
		long mId;
		String accountName;
		String niceName;
		String path;
		String url;
		String skinPath;
	}

	static class AccountViewHolder {
		View icon;
		TextView skinName;
		TextView skinMessage;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		 AccountInfoHolder mAccount = mAdapter.mAccounts.get(position);
        
         SkinView.actionSkinView(this,mAccount.url,mAccount.niceName,mAccount.skinPath,mAccount.mId);
	}
}
