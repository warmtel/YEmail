package com.mail163.email.bean;

import java.util.ArrayList;
import java.util.List;

import android.widget.TextView;


public class Page<T>{
	private String resultcount;
	private List<T> resultlist = new ArrayList<T>();
	private String curpage;
	private String totalpage;
	private String iconpath;
	  /**
	 * @return the iconpath
	 */
	public String getIconpath() {
		return iconpath;
	}

	/**
	 * @param iconpath the iconpath to set
	 */
	public void setIconpath(String iconpath) {
		this.iconpath = iconpath;
	}

	/**
     * 是否有下一页
     */
    public boolean hasNextPage() {
    	 int intCurpage = Integer.parseInt(this.curpage);
    	 int intTotalpage = Integer.parseInt(totalpage);
         return intCurpage==intTotalpage?true:false;
    }
    
    public void addItem(T t){
    	resultlist.add(t);
    }
	/**
	 * @return the resultcount
	 */
	public String getResultcount() {
		return resultcount;
	}
	/**
	 * @param resultcount the resultcount to set
	 */
	public void setResultcount(String resultcount) {
		this.resultcount = resultcount;
	}
	/**
	 * @return the resultlist
	 */
	public List<T> getResultlist() {
		return resultlist;
	}
	/**
	 * @param resultlist the resultlist to set
	 */
	public void setResultlist(List<T> resultlist) {
		this.resultlist = resultlist;
	}
	/**
	 * @return the curpage
	 */
	public String getCurpage() {
		return curpage;
	}
	/**
	 * @param curpage the curpage to set
	 */
	public void setCurpage(String curpage) {
		this.curpage = curpage;
	}
	/**
	 * @return the totalpage
	 */
	public String getTotalpage() {
		return totalpage;
	}
	/**
	 * @param totalpage the totalpage to set
	 */
	public void setTotalpage(String totalpage) {
		this.totalpage = totalpage;
	}
}
