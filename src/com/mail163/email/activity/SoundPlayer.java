package com.mail163.email.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.mail163.email.R;

public class SoundPlayer extends Activity implements OnClickListener, OnCompletionListener {
	private SeekBar seekbar;
	private ImageView btnPlay, btnPause;
	private boolean flag = true;
	private Uri mUri;
	private int playPauseFlag = 0;
	private MediaPlayer mPlayer;

	public static void actionSoundPlayer(Context mContext, Uri contentUri) {
		Intent intent = new Intent(mContext, SoundPlayer.class);
		intent.setData(contentUri);
		mContext.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.media_play);
		setView();
		
        setTitle(getString(R.string.sound_play_attchment_title));
		flag = true;

		Intent i = getIntent();
		if (i != null) {
			mUri = i.getData();
			if (mUri != null) {
				startPlayback(this, mUri);
				btnPlay.setVisibility(ImageView.GONE);
				btnPause.setVisibility(ImageView.VISIBLE);
				playPauseFlag = 1;
				refreshUI();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void setView() {
		btnPlay = (ImageView) this.findViewById(R.id.play_play);
		btnPlay.setOnClickListener(this);
		btnPause = (ImageView) this.findViewById(R.id.play_pause);
		btnPause.setOnClickListener(this);

		seekbar = (SeekBar) this.findViewById(R.id.player_seekbar);
	}

	public void refreshUI() {
		new Thread() {
			public void run() {
				while (flag) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					updatePlayer(getTotalTime(),getCurrentTime());
				}
			}
		}.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		flag = false;
		stop();
	}

	public void updatePlayer(int tTime, int cTime) {
		seekbar.setMax(tTime);
		seekbar.setProgress(cTime);
	}

	private void onClickPlayPause() {
		if (playPauseFlag == 0) {
			btnPlay.setVisibility(ImageView.GONE);
			btnPause.setVisibility(ImageView.VISIBLE);
			playPauseFlag = 1;
		} else if (playPauseFlag == 1) {
			btnPlay.setVisibility(ImageView.VISIBLE);
			btnPause.setVisibility(ImageView.GONE);
			playPauseFlag = 0;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.play_play) {
			onClickPlayPause();
			startPlayback(this, mUri);
		}else if(v.getId() == R.id.play_pause){
			onClickPlayPause();
			stop();
		}
	}

	public void startPlayback(Context mContext, Uri uri) {
		stop();

		try {
			mPlayer = MediaPlayer.create(mContext, uri);
			mPlayer.setOnCompletionListener(this);
			mPlayer.start();
		} catch (IllegalArgumentException e) {
			mPlayer = null;
			return;
		}

	}

	public void stopPlayback() {
		if (mPlayer == null) // we were not in playback
			return;

		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
	}

	public void stop() {
		stopPlayback();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		stop();
		btnPlay.setVisibility(View.VISIBLE);
		btnPause.setVisibility(View.GONE);
		playPauseFlag = 0;
	}

	public int getCurrentTime() {
		int currentTime = 0;
		if (mPlayer != null) {
			try {
				currentTime = mPlayer.getCurrentPosition();
			} catch (Exception e) {
				e.printStackTrace();
				mPlayer = null;
			}
		}
		return currentTime;
	}


	public int getTotalTime() {
		int totalTime = 0;
		if (mPlayer != null) {
			try {
				totalTime = mPlayer.getDuration();
			} catch (Exception e) {
				e.printStackTrace();
				mPlayer = null;
			}
		}
		return totalTime;
	}
}
