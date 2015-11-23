package com.mail163.email.bean;

import android.graphics.Bitmap;

public class AppCategoryDto {
	public String id;
	public String name;
	public String path;
	public String url;
	public String skinpath;
	public Bitmap icon;
	public AppCategoryDto copy(){
		AppCategoryDto copy = new AppCategoryDto();
		copy.id = id;
		copy.name = name;
		copy.path = path;
		copy.url = url;
		copy.skinpath = skinpath;
		copy.icon = icon;
		return copy;
	}
	public String getSkinpath() {
		return skinpath;
	}
	public void setSkinpath(String skinpath) {
		this.skinpath = skinpath;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setIcon(Bitmap icon) {
		this.icon = icon;
	}
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getUrl() {
		return url;
	}
	public Bitmap getIcon() {
		return icon;
	}
}
