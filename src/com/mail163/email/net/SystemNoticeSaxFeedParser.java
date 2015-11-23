package com.mail163.email.net;

import android.app.Application;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import com.mail163.email.NoticeBean;

public class SystemNoticeSaxFeedParser extends BaseFeedParsers {
	static final String rootX = "notice";
	static final String control = "control";
	static final String version = "version";
	static final  String email = "email";
	static final  String message = "message";
	
	
	public SystemNoticeSaxFeedParser(String feedUrl,Application mApplication) {
		super(feedUrl);
	}

	public NoticeBean parse() {
		final NoticeBean mNoticeBean = new NoticeBean();
		RootElement root = new RootElement(rootX);

		Element controlopen = root.getChild(control);
		controlopen.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				mNoticeBean.setControl(body);
			}
		});
		Element versions = root.getChild(version);
		versions.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				mNoticeBean.setVersions(Long.parseLong(body));
			}
		});
		
		Element mail = root.getChild(email);
		mail.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				mNoticeBean.setEmail(body);
			}
		});
		
		Element content = root.getChild(message);
		content.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				mNoticeBean.setMessage(body);
			}
		});
		
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {

			throw new RuntimeException(e);
		}
		return mNoticeBean;
	}
}
