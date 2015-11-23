package com.mail163.email.net;

import android.app.Application;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import com.mail163.email.SoftUpdateBean;

public class SoftUpdateSaxFeedParser extends BaseFeedParsers {
	static final String rootX = "yimail";
	static final String serverVersion = "serverVersion";
	static final String smsmail = "smsmail";
	static final  String sendmessage = "sendmessage";
	static final String apkSize = "apkSize";
	static final String addAccountCount = "addAccountCount";
	static final  String downloadUrl = "downloadUrl";
	static final  String updateContent = "updateContent";
	
	public SoftUpdateSaxFeedParser(String feedUrl,Application mApplication) {
		super(feedUrl);
	}

	public SoftUpdateBean parse() {
		final SoftUpdateBean softUpdata = new SoftUpdateBean();
		RootElement root = new RootElement(rootX);

		Element version = root.getChild(serverVersion);
		version.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setServerVersion(body);
			}
		});
		
		Element apkSizes = root.getChild(apkSize);
		apkSizes.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setApkSize(Long.parseLong(body));
			}
		});
		
		Element maddAccountCount = root.getChild(addAccountCount);
		maddAccountCount.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setAddAccountCount(Integer.parseInt(body));
			}
		});
		
		Element smsmails = root.getChild(smsmail);
		smsmails.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setSmsMail(body);
			}
		});
		Element mSendmessage = root.getChild(sendmessage);
		mSendmessage.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setSendmessage(body);
			}
		});
		Element url = root.getChild(downloadUrl);
		url.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setDownloadUrl(body);
			}
		});
		
		Element content = root.getChild(updateContent);
		content.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				softUpdata.setUpdateContent(body);
			}
		});
		
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {

			throw new RuntimeException(e);
		}
		return softUpdata;
	}
}
