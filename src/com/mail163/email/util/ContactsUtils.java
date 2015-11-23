package com.mail163.email.util;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.widget.Toast;

import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.mail.Address;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.EmailContact;
import com.mail163.email.provider.EmailContent.EmailContactColumns;

public class ContactsUtils {
	// 字母Z使用了两个标签，这里有２７个值
	// i, u, v都不做声母, 跟随前面的字母
	private static char[] chartable = { '啊', '芭', '擦', '搭', '蛾', '发', '噶', '哈',
			'哈', '击', '喀', '垃', '妈', '拿', '哦', '啪', '期', '然', '撒', '塌', '塌',
			'塌', '挖', '昔', '压', '匝', '座' };

	private static char[] alphatable = { 'A', 'B', 'C', 'D', 'E', 'F', 'G',
			'H', 'I',

			'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z' };

	private static int[] table = new int[27];

	// 初始化
	static {
		for (int i = 0; i < 27; ++i) {
			table[i] = gbValue(chartable[i]);
		}
	}

	public static char Char2Alpha(char ch) {

		if (ch >= 'a' && ch <= 'z')
			return (char) (ch - 'a' + 'A');
		if (ch >= 'A' && ch <= 'Z')
			return ch;

		int gb = gbValue(ch);
		if (gb < table[0]) {
			return '0';
		}
		int i;
		for (i = 0; i < 26; ++i) {
			if (match(i, gb))
				break;
		}

		if (i >= 26)
			return '0';
		else
			return alphatable[i];
	}

	// 取出汉字的编码
	private static int gbValue(char ch) {
		String str = new String();
		str += ch;
		try {
			byte[] bytes = str.getBytes("GBK");
			if (bytes.length < 2)
				return 0;
			return (bytes[0] << 8 & 0xff00) + (bytes[1] & 0xff);
		} catch (Exception e) {
			return 0;
		}

	}

	private static boolean match(int i, int gb) {
		if (gb < table[i])
			return false;

		int j = i + 1;

		// 字母Z使用了两个标签
		while (j < 26 && (table[j] == table[i]))
			++j;

		if (j == 26)
			return gb <= table[j];
		else
			return gb < table[j];

	}

	/**
	 * "test" <android@163.com>
	 * 
	 * @param toAdress
	 */
	public static void addEmailContacts(Context mContext, String toAdress) {
		int start;
		int end;
		String[] mAdressList = toAdress.split(",");
		for (int i = 0; i < mAdressList.length; i++) {
			Address from = Address.unpackFirst(mAdressList[i]);
			String address = from.getAddress();
			String personal = from.getPersonal();
			if (personal == null) {
				if (address.contains("<")) {
					start = address.indexOf("<") + 1;
					end = address.indexOf("@");
					personal = address.substring(start, end);
					address = address.substring(start);
				} else {
					start = address.indexOf("@");
					personal = address.substring(0, start);
				}
			}
			if (!isAddress(mContext, address)) {
//				addContactsTwo(mContext, personal, address);
				  addEmailContactcMessage(mContext, personal, address);
			}
		}
	}

	public static boolean isAddress(Context mContext, String address) {
		Cursor mCursor = null;
		try {
			Uri baseUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
			final ContentResolver resolver = mContext.getContentResolver();
			mCursor = resolver.query(baseUri, null, Email.DATA + " like '"
					+ address + "'", null, null);
			if (mCursor.getCount() > 0) {
				return true;
			} else {
				return false;
			}
		} finally {
			if (mCursor != null)
				mCursor.close();
		}
	}

	public static void addContacts(Context mContext) {
		final ContentResolver resolver = mContext.getContentResolver();
		final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(RawContacts.CONTENT_URI);
		ContentValues values = new ContentValues();
		builder.withValues(values);
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
		builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
		builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		builder.withValue(StructuredName.DISPLAY_NAME, "MP");
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
		builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
		builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
		builder.withValue(Phone.NUMBER, "110");
		builder.withValue(Data.IS_PRIMARY, 1);
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
		builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
		builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
		builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
		builder.withValue(Email.DATA, "mp870601@163.com");
		operationList.add(builder.build());

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}

	public static void addContactsTwo(Context mContext, String name,
			String email) {
		final ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		Uri rawContactUri = cr.insert(RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		// 加入姓名
		values.clear();
		values.put(Data.RAW_CONTACT_ID, rawContactId);
		values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		values.put(StructuredName.DISPLAY_NAME, name);
		cr.insert(Data.CONTENT_URI, values);

		// 加入号码
		// values.clear();
		// values.put(Data.RAW_CONTACT_ID, rawContactId);
		// values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
		// values.put(Phone.TYPE, Phone.TYPE_MOBILE); //手机号码
		// values.put(Phone.NUMBER, cellPhoneNumber);
		// cr.insert(Data.CONTENT_URI, values);

		// 加入邮件
		values.clear();
		values.put(Data.RAW_CONTACT_ID, rawContactId);
		values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
		values.put(Email.TYPE, Email.TYPE_MOBILE); // 手机号码
		values.put(Email.DATA, email);
		cr.insert(Data.CONTENT_URI, values);
	}

	/**
	 * 根据提供的ID删除数据库中相对应的项
	 * 
	 * @param id
	 * @param listId
	 */
	public static void deleteContactsById(Context mContext, int id) {
		long rawContactId = 0;
		Uri questUri = ContentUris.withAppendedId(Data.CONTENT_URI, id);

		Cursor c = mContext.getContentResolver().query(questUri,
				new String[] { Data.RAW_CONTACT_ID }, null, null, null);
		if (c != null) {
			try {
				while (c.moveToNext()) {
					rawContactId = c.getLong(0);
				}
			} finally {
				c.close();
			}
		}
		Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				rawContactId);

		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		uri = b.build();
		mContext.getContentResolver().delete(uri, null, null);
	}

	public static void updateContactsById(Context mContext, String name,
			String email, int id) {
		final ContentResolver cr = mContext.getContentResolver();
		long rawContactId = 0;
		Uri questUri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
		Cursor c = cr.query(questUri, new String[] { Data.RAW_CONTACT_ID },
				null, null, null);
		if (c != null) {
			try {
				while (c.moveToNext()) {
					rawContactId = c.getLong(0);
				}
			} finally {
				c.close();
			}
		}
		Logs.d(Logs.LOG_TAG, "rawContactId :" + rawContactId);
//		ContentValues values = new ContentValues();
//		// update Name
//		values.clear();
//		values.put(StructuredName.DISPLAY_NAME, name);
//		cr.update(Data.CONTENT_URI, values, Data.RAW_CONTACT_ID + " == "
//				+ rawContactId + " and " + Data.MIMETYPE + " == '"
//				+ StructuredName.CONTENT_ITEM_TYPE + "'", null);
//
//		// update Email
//		values.clear();
//		values.put(Email.DATA, email);
//		cr.update(Data.CONTENT_URI, values, Data.RAW_CONTACT_ID + " == "
//				+ rawContactId + " and " + Data.MIMETYPE + " == '"
//				+ Email.CONTENT_ITEM_TYPE + "'", null);

		final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		
		//update name
		ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
		builder.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
				+ "=?", new String[] {
				String.valueOf(rawContactId), StructuredName.CONTENT_ITEM_TYPE});
		builder.withValue(StructuredName.DISPLAY_NAME, name);
		operationList.add(builder.build());

		//update email
		builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
		builder.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
				+ "=?" , new String[] {
				String.valueOf(rawContactId), Email.CONTENT_ITEM_TYPE });
		builder.withValue(Email.DATA, email);
		operationList.add(builder.build());
		try {
			cr.applyBatch(ContactsContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}
	public static void deleteEmailContactsById(Context mContext, int id) {
        mContext.getContentResolver().delete(
                EmailContent.EmailContact.CONTENT_URI,
                EmailContent.EmailContact.ID + "="+id, null);
    }
    public static void deleteContactsOwnerById(Context mContext, int id) {
        final ContentResolver cr = mContext.getContentResolver();
        cr.delete(EmailContent.EmailContact.CONTENT_URI,  EmailContent.EmailContact.ID + "=" + id, null);
    }
    public static void updateEmailContactsById(Context mContext, String name,
            String email, int id) {
        if(!Utiles.isEmailAddress(email)){
            Toast.makeText(mContext, mContext.getString(R.string.contact_email_address_pandan), Toast.LENGTH_SHORT).show();
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(EmailContactColumns.USERNAME, name);
        values.put(EmailContactColumns.EMAIL, email);

        cr.update(EmailContent.EmailContact.CONTENT_URI, values,
                EmailContent.EmailContact.ID + "=" + id, null);
    }
    public static void addEmailContactcMessage(Context mContext,String userName,String email){

        int count = EmailContact.count(mContext,EmailContact.CONTENT_URI, EmailContact.EMAIL+" = '"+email+"'", null);
        
        Logs.d(Logs.LOG_TAG, "11111111addEmailContactcMessage  :"+count);
        
        if(count == 0){
            ContentValues values = new ContentValues();
            values.put(EmailContactColumns.USERNAME, userName);
            values.put(EmailContactColumns.EMAIL, email);
            mContext.getContentResolver().insert(EmailContent.EmailContact.CONTENT_URI, values);
        }
        
    }

}
