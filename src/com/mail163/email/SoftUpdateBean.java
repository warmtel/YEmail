
package com.mail163.email;

public class SoftUpdateBean {
	public String serverVersion;//ServerVersion:1  //服务端最新版本号
	public long apkSize;  //更新包大小
	public int addAccountCount; //添加账号数
	public String smsMail; 
	public String downloadUrl = "";//DownloadUrl:"",  //APK包下载地址
	public String updateContent;  //更新情况
	public int getAddAccountCount() {
		return addAccountCount;
	}
	public void setAddAccountCount(int addAccountCount) {
		this.addAccountCount = addAccountCount;
	}
	public String getSmsMail() {
		return smsMail;
	}
	public String getSendmessage() {
		return sendmessage;
	}
	public void setSendmessage(String sendmessage) {
		this.sendmessage = sendmessage;
	}
	public String sendmessage;
	public void setSmsMail(String smsMail) {
		this.smsMail = smsMail;
	}
	public long getApkSize() {
		return apkSize;
	}
	public void setApkSize(long apkSize) {
		this.apkSize = apkSize;
	}
	public String getUpdateContent() {
		return updateContent;
	}
	public void setUpdateContent(String updateContent) {
		this.updateContent = updateContent;
	}
	public String getDownloadUrl() {
		return downloadUrl;
	}
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	public String getServerVersion() {
		return serverVersion;
	}
	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}
	

}
