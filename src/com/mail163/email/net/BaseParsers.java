package com.mail163.email.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.Application;

import com.mail163.email.Logs;

public abstract class BaseParsers {
	private String httpUrl;
	private Application mApplication;

	protected BaseParsers(String httpUrl, Application mApplication) {
		try {
			this.mApplication = mApplication;
			this.httpUrl = httpUrl;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected InputStream getInputStream() {
		AndroidHttpClient client = null;
		HttpResponse response;
		HttpGet request = new HttpGet(httpUrl);
		if (request == null) {
			return null;
		}
		try {
	
			client = AndroidHttpClient.newInstance(mApplication,
					"Android client");
			response = client.execute(request);
			if (response == null) {
				return null;
			}
			int statusCode = response.getStatusLine().getStatusCode();
			Logs.v(Logs.LOG_TAG, "statusCode :" + statusCode);
			if (statusCode == 200) {
				InputStream is = response.getEntity().getContent();
				return is;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	protected String getResponseXmlString() {
		AndroidHttpClient client = null;
		HttpResponse response;
		HttpGet request = new HttpGet(httpUrl);
		if (request == null) {
			return null;
		}
		try {
			client = AndroidHttpClient.newInstance(mApplication,
					"Android client");
			response = client.execute(request);
			if (response == null) {
				return null;
			}
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				InputStream is = response.getEntity().getContent();
				if (client != null) {
					client.close();
					client = null;
				}
				return convertStreamToString(is);
			}
		} catch (IOException e) {
			if (client != null) {
				client.close();
				client = null;
			}
			throw new RuntimeException(e);
		}
		return null;
	}

	protected String convertStreamToString(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "UTF-8"), 8 * 1024);

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			sb.delete(0, sb.length());
			Logs.e(Logs.LOG_TAG, e.getMessage());
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Logs.e(Logs.LOG_TAG, e.getMessage());
			}
		}

		return sb.toString();
	}

}