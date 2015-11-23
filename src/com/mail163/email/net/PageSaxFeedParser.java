package com.mail163.email.net;

import android.app.Application;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import com.mail163.email.bean.AppCategoryDto;
import com.mail163.email.bean.Page;

public class PageSaxFeedParser extends BaseFeedParsers {
	static final String page = "page";
	static final String resultcount = "resultcount";
	static final  String curpage = "curpage";
	static final  String totalpage = "totalpage";
	static final  String iconpath = "iconpath";
	static final  String ITEM = "item";
	static final  String id = "id";
	static final  String name = "name";
	static final  String path = "path";
	static final  String url = "url";
	static final  String skinpath = "skinpath";

	public PageSaxFeedParser(String feedUrl,Application mApplication) {
		super(feedUrl);
	}

	public Page<AppCategoryDto> parse() {
		final Page<AppCategoryDto> pageAppCategoryDto = new  Page<AppCategoryDto>();
		final AppCategoryDto appCategoryDto = new AppCategoryDto();
		RootElement root = new RootElement(page);

		Element curpage = root.getChild("curpage");
		curpage.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				pageAppCategoryDto.setCurpage(body);
			}
		});
		Element iconpath = root.getChild("iconpath");
		iconpath.setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				pageAppCategoryDto.setIconpath(body);
			}
		});
		Element item = root.getChild(ITEM);
		item.setEndElementListener(new EndElementListener(){
			public void end() {
				pageAppCategoryDto.addItem(appCategoryDto.copy());
			}
		});
		item.getChild(id).setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				appCategoryDto.setId(body);
			}
		});
		item.getChild(name).setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				appCategoryDto.setName(body);
			}
		});
		item.getChild(path).setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				appCategoryDto.setPath(body);
			}
		});
		item.getChild(url).setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				appCategoryDto.setUrl(body);
			}
		});
		item.getChild(skinpath).setEndTextElementListener(new EndTextElementListener(){
			public void end(String body) {
				appCategoryDto.setSkinpath(body);
			}
		});
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return pageAppCategoryDto;
	}
}
