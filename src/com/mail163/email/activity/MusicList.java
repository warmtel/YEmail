package com.mail163.email.activity;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mail163.email.Email;
import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.service.MusicService;
import com.mail163.email.util.Utiles;

public class MusicList extends ListActivity implements OnClickListener {
	private static String tag = "MusicList";
	private MessageListAdapter mAdapter;
	private Button mConfirm;
	private Button mCancel;
	private QueryHandler mQueryHandler;
	private TextView mLeftTitle;
	public String skinName;
	private ListView mListView;
	public static void actionMusicList(Context context) {
		Intent i = new Intent(context, MusicList.class);
		context.startActivity(i);
	}

	private static class QueryHandler extends AsyncQueryHandler {
		protected final WeakReference<MusicList> mActivity;

		public QueryHandler(Context context) {
			super(context.getContentResolver());
			mActivity = new WeakReference<MusicList>((MusicList) context);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			final MusicList activity = mActivity.get();
			if (activity != null && !activity.isFinishing()) {
				activity.mAdapter.changeCursor(cursor);
			} else {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.music_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.list_title);

		setupTitle();
		setupButton();
		setupListView();
		setupBackground();
		
		mQueryHandler = new QueryHandler(this);
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		mAdapter.onContentChanged();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mAdapter.onContentChanged();
	}
    private void setupBackground(){
    	if(Global.backDrawable == null){
    		Global.backDrawable = Utiles.fetchBackDrawable(this);
			setTheme(R.style.XTheme);
		}
		if(null != Global.backDrawable){
			if(Global.skinName.contains(Global.whiteSkin)){
				setTheme(R.style.Default);
	        }else{
	        	setTheme(R.style.XTheme);
	        }
			mListView.setBackgroundDrawable(Global.backDrawable);
		}
    }
	private void setupTitle() {
		mLeftTitle = (TextView) findViewById(R.id.title_left_text);
		mLeftTitle.setText(getString(R.string.music_backgroud_lable));
	}

	private void setupButton() {
		mConfirm = (Button) findViewById(R.id.btn_play);
		mConfirm.setOnClickListener(this);
		mCancel = (Button) findViewById(R.id.btn_close);
		mCancel.setOnClickListener(this);
	}

	private void setupListView() {
		mListView = getListView();
		mAdapter = new MessageListAdapter(this);
		setListAdapter(mAdapter);
		mListView.setSaveEnabled(false);
		mListView.setCacheColorHint(getResources().getColor(R.color.background_cachhint));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

	}

	public void startQuery() {
		Uri queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		mQueryHandler.startQuery(40, null, queryUri, null,
				MediaStore.MediaColumns.MIME_TYPE + " =  'audio/mpeg'", null,
				null);
	}

	class MessageListAdapter extends CursorAdapter {
		public static final int COLUMN_ID = 0;
		public static final int COLUMN_DISPLAY_NAME = 1;
		public static final int COLUMN_SUBJECT = 2;

		private Drawable mSelectedIconOn;
		private Drawable mSelectedIconOff;
		private Context mContext;
		private LayoutInflater mInflater;
		private HashSet<Long> mChecked = new HashSet<Long>();

		public MessageListAdapter(Context context) {
			super(context, null, true);
			mContext = context;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			Resources resources = context.getResources();
			mSelectedIconOn = resources.getDrawable(R.drawable.btn_radio_on);
			mSelectedIconOff = resources.getDrawable(R.drawable.btn_radio_off);
		}

		@Override
		protected synchronized void onContentChanged() {
			startQuery();
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			MusicListItem itemView = (MusicListItem) view;
			itemView.bindViewInit(this, true);

			itemView.mMessageId = cursor.getLong(cursor
					.getColumnIndex(MediaStore.MediaColumns._ID));
			itemView.mUser = cursor.getString(cursor
					.getColumnIndex(MediaStore.MediaColumns.TITLE));
			itemView.mSize = cursor.getLong(cursor
					.getColumnIndex(MediaStore.MediaColumns.SIZE));
			itemView.mFileDir = cursor.getString(cursor
					.getColumnIndex(MediaStore.MediaColumns.DATA));
			TextView titleView = (TextView) view.findViewById(R.id.music_name);
			titleView.setText(itemView.mUser);

			TextView nameView = (TextView) view.findViewById(R.id.music_size);
			nameView.setText(Formatter.formatFileSize(MusicList.this,
					itemView.mSize));

			itemView.mSelected = mChecked.contains(itemView.mMessageId);
			ImageView selectedView = (ImageView) view
					.findViewById(R.id.music_selected);
			selectedView.setImageDrawable(itemView.mSelected ? mSelectedIconOn
					: mSelectedIconOff);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.music_list_item, parent, false);
		}

		public void updateSelected(MusicListItem itemView, boolean newSelected) {
			sendPlayService(0);
			ImageView selectedView = (ImageView) itemView
					.findViewById(R.id.music_selected);
			selectedView.setImageDrawable(newSelected ? mSelectedIconOn
					: mSelectedIconOff);
			long id = itemView.mMessageId;
			mChecked.clear();
			mChecked.add(id);
			mAdapter.notifyDataSetChanged();
			MusicService.resUrl = itemView.mFileDir;
			MusicService.musicName = itemView.mUser;

			saveMusicShared();
			sendPlayService(1);
		}
	}

	public void sendPlayService(int op) {
		Bundle bundle = new Bundle();
		bundle.putInt("op", op);
		Intent intent = new Intent(Global.ACTION_MUSIC_SERVICE_BROADCASTK);
		intent.putExtras(bundle);
		sendBroadcast(intent);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_play) {
			finish();
		} else if (v.getId() == R.id.btn_close) {
			sendPlayService(0);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			sendPlayService(0);
		}
		return super.onKeyDown(keyCode, event);
	}

	private void saveMusicShared() {
		Editor sharedata = getSharedPreferences(Email.STORESETMESSAGE, 0)
				.edit();
		sharedata.putString(Global.set_music_dir, MusicService.resUrl);
		sharedata.putString(Global.set_music_name, MusicService.musicName);
		sharedata.commit();
	}
}