package com.mail163.email.util;

import com.mail163.email.Logs;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

public class PhoneUtil {
	/**
	 * 获取SIM卡的IMSI码 SIM卡唯一标识：IMSI 国际移动用户识别码（IMSI：International Mobile Subscriber
	 * Identification Number）是区别移动用户的标志，
	 * 储存在SIM卡中，可用于区别移动用户的有效信息。IMSI由MCC、MNC、MSIN组成，其中MCC为移动国家号码，由3位数字组成，
	 * 唯一地识别移动客户所属的国家，我国为460；MNC为网络id，由2位数字组成，
	 * 用于识别移动客户所归属的移动网络，，中国移动TD系统使用00，中国联通GSM系统使用01
	 * ，中国移动GSM系统使用02，中国电信CDMA系统使用03， 一个典型的IMSI号码为460030912121001;
	 * MSIN为移动客户识别码，采用等长11位数字构成。
	 * 唯一地识别国内GSM移动通信网中移动客户。所以要区分是移动还是联通，只需取得SIM卡中的MNC字段即可
	 */
	public static String getImsi(final Context mActivity) {
		TelephonyManager telephonyManager = (TelephonyManager) mActivity
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getSubscriberId();

		if (imei == null) {
			return "12345678";
		}
		return imei;
	}

	/**
	 * 获取手机号码
	 */
	public static String getMdn(final Context mActivity) {
		TelephonyManager telephonyManager = (TelephonyManager) mActivity
				.getSystemService(Context.TELEPHONY_SERVICE);
		String mdn = telephonyManager.getLine1Number();
		return mdn;
	}

	/**
	 * 获取手机ESN CDMA手机机身号简称ESN-Electronic Serial Number的缩写�?
	 * 
	 * 它是�?��32bits长度的参数，是手机的惟一标识�? GSM手机是IMEI�?International Mobile Equipment
	 * Identity)�? 国际移动身份码�?
	 */
	public static String getEsn(final Context mActivity) {
		TelephonyManager tm = (TelephonyManager) mActivity
				.getSystemService(Context.TELEPHONY_SERVICE);
		String esn = tm.getDeviceId();

		return esn;
	}

	/**
	 * 获取手机MODLE,手机型号
	 */
	public static String getModel() {
		String model = Build.MODEL;
		return model;
	}
    public static String getMobileNumber(Context mContext){
    	  TelephonyManager phoneMgr=(TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
    	  if(phoneMgr.getLine1Number() == null){
    		  return "111";
    	  }
    	  return phoneMgr.getLine1Number();
    }
    public static String getSdkVersion() {
		String model = Build.VERSION.SDK;
		return model;
	}
	/**
	 * 获取手机类型
	 */
	public static int getPhoneType(final Context mActivity) {
		TelephonyManager telephonyManager = (TelephonyManager) mActivity
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getPhoneType();
	}

	/**
	 * 获取手机ROM
	 */
	public static String getRom() {
		String DISPLAY = Build.DISPLAY; // �?titanium-userdebug 2.1
		// TITA_K29_00.13.01I 173018 test-keys
		return DISPLAY;
	}

	/**
	 * android--获取手机的IMSI码,并判断是中国移动\中国联通\中国电信
	 * 
	 * @param mContext
	 */
	public static String IMSIType(Context mContext) {
		TelephonyManager telManager = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = telManager.getSubscriberId();
		if (imsi != null) {
			// 因为移动网络编号46000下的IMSI已经用完，所以虚拟了一个46002编号，134/159号段使用了此编号
			if (imsi.startsWith("46000") || imsi.startsWith("46002")) {
				return "中国移动";
			} else if (imsi.startsWith("46001")) {
				return "中国联通";
			} else if (imsi.startsWith("46003")) {
				return "中国电信";
			}else{
				return "wifi";
			}
		}else{
			return "wifi";
		}
	}
}
