package com.mail163.email.service;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service implements OnCompletionListener,
		 OnPreparedListener {
	public static String resUrl = "";
	public static String musicName = "music";
	public static MediaPlayer player;

	public void onCreate() {
		super.onCreate();
		playMusic();
	}

	public void playMusic() {
		try {
			if (player == null) {
				if("".equals(resUrl))
					return;
				Uri uri = Uri.parse(resUrl);
				player = MediaPlayer.create(this, uri);

				player.setOnPreparedListener(this);
				player.setOnCompletionListener(this);
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.start();
			}
			if (!player.isPlaying()) {
				player.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pauseMusic() {
		if (player != null)
			if (player.isPlaying()) {
				player.pause();
			}
	}
	public void stopMusic() {
		if (player != null) {
			player.stop();
			try {
				// 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
				player.prepare();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				int op = bundle.getInt("op");
				switch (op) {
				case 0:
					stopMusic();
					break;
				case 1:
					playMusic();
					break;
				case 2:
					pauseMusic();
					break;
				}
			}
		}
	}
	public void onDestroy() {
		super.onDestroy();
		release();
	}

	public void onCompletion() {
		try {
			player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onCompletion(MediaPlayer player) {
		doCompletion();
	}

	public void onPrepared(MediaPlayer player) {
		Log.v("TAG", " onPrepared...");
	}

	public void doCompletion() {
		try {
			 player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void release() {
		try {
			if (player != null) {
				player.stop();
				player.release();
				player = null;
			}
		} catch (Exception ex) {
			player = null;
			ex.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}