
package com.mail163.email;

public class NoticeBean {
	public String control;//通知开关
	public long versions; //控制对特定versions用户发送通知
	public String email = "";  //email地址用于控制对特定用户发送通知，中间逗号分隔
	public String message;  //通知消息
	
	public long getVersions() {
		return versions;
	}
	public void setVersions(long versions) {
		this.versions = versions;
	}
	public String getControl() {
		return control;
	}
	public void setControl(String control) {
		this.control = control;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	
	

}
