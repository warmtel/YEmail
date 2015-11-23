package com.mail163.email.activity;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

import com.mail163.email.R;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.util.ContactsUtils;
import com.mail163.email.util.Utiles;

public class ContactsEmailOwner extends ListActivity implements
        View.OnClickListener {
    private static final String EXTRA_ADD_CONTACTS = "addContacts";
    private static final String order = "email";
    private static final int DIALOG_TEXT_ENTRY = 1;
    private static final int DIALOG_TEXT_ADDCONTACT = 2;
    private static final int DIALOG_TEXT_UPDATE = 3;
    private ListView mListView;
    private WindowManager windowManager;
    private TextView txtOverlay;
    private Button mAllSelectButton;
    private Button mCancelSelectButton;
    private Button mConfirmButton;
    private Button mAddContactButton;
    private MessageEmailListAdapter mAdapter;
    private QueryHandler mQueryHandler;
    public static char[] mcharList;
    private Intent mIntent;
    private int contacsId;
    private String editUser = "";
    private String editEmail = "";

    public static void actionAddEmailContacts(Activity activity, int requestCode) {
        Intent i = new Intent(activity, ContactsEmailOwner.class);
        activity.startActivityForResult(i, requestCode);
    }

    private static class QueryHandler extends AsyncQueryHandler {
        protected final WeakReference<ContactsEmailOwner> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ContactsEmailOwner>(
                    (ContactsEmailOwner) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final ContactsEmailOwner activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                mcharList = new char[cursor.getCount()];
                while (cursor.moveToNext()) {
                    int key = cursor.getPosition();
                    String userNameValue = cursor
                            .getString(cursor
                                    .getColumnIndex(EmailContent.EmailContact.USERNAME));
                    char c = ContactsUtils.Char2Alpha(userNameValue.charAt(0));
                    mcharList[key] = c;
                }
                activity.mAdapter.changeCursor(cursor);
            } else {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntent = getIntent();
        setContentView(R.layout.contacts_emailowner_content);

        setupOverlay();
        setupListView();
        setupButtonView();
        mQueryHandler = new QueryHandler(this);
        startQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // error：Activity has leaked window that was originally added.
        // 在window上添加View，当离开当前Activity时报错. 加入如下解决问题.
        // 在关闭Activity的时候，没有关闭AlertDialog，也会引起上述问题
        windowManager.removeView(txtOverlay);
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
    }

    private void setupOverlay() {
        txtOverlay = (TextView) LayoutInflater.from(this).inflate(
                R.layout.popup_char_hint, null);
        txtOverlay.setVisibility(View.INVISIBLE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(txtOverlay, lp);
    }

    private void setupListView() {
        mListView = getListView();

        mAdapter = new MessageEmailListAdapter(this);
        setListAdapter(mAdapter);

        mListView.setOnScrollListener(mAdapter);
        mListView.setSaveEnabled(false);

        registerForContextMenu(mListView);
    }

    private void setupButtonView() {
        mAllSelectButton = (Button) findViewById(R.id.btn_all_select);
        mCancelSelectButton = (Button) findViewById(R.id.btn_cancel_select);
        mConfirmButton = (Button) findViewById(R.id.btn_confirm);
        mAddContactButton =  (Button) findViewById(R.id.btn_newcontact);
        mAllSelectButton.setOnClickListener(this);
        mCancelSelectButton.setOnClickListener(this);
        mConfirmButton.setOnClickListener(this);
        mAddContactButton.setOnClickListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        menu.setHeaderTitle(getString(R.string.contacts_people));
        getMenuInflater().inflate(R.menu.contacts_list_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        contacsId = (int) info.id;
        switch (item.getItemId()) {
        case R.id.delete:
            ContactsUtils.deleteEmailContactsById(this, contacsId);
            break;
        case R.id.edit:
            Cursor mCursor = (Cursor) mAdapter.getItem((int) info.position);
            editUser = mCursor.getString(mCursor
                    .getColumnIndex(EmailContent.EmailContact.USERNAME));
            editEmail = mCursor
                    .getString(mCursor
                            .getColumnIndex(EmailContent.EmailContact.EMAIL));
            showDialog(DIALOG_TEXT_ENTRY);
            break;
        }
        return super.onContextItemSelected(item);
    }
    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case (DIALOG_TEXT_ENTRY):
            removeDialog(id);
            break;
        case DIALOG_TEXT_ADDCONTACT:
            removeDialog(id);
            break;
        }
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_TEXT_ENTRY:
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(
                    R.layout.alert_dialog_text_entry, null);
            LinearLayout mLinearLayout = (LinearLayout) textEntryView;
            final TextView mUserName = (TextView) mLinearLayout.getChildAt(1);
            final TextView mEmail = (TextView) mLinearLayout.getChildAt(3);

            mUserName.setText(editUser);
            mEmail.setText(editEmail); 
            
            return new AlertDialog.Builder(ContactsEmailOwner.this)
                    .setTitle(R.string.contacts_add)
                    .setView(textEntryView)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    ContactsUtils.updateEmailContactsById(
                                            ContactsEmailOwner.this, mUserName
                                                    .getText().toString(),
                                            mEmail.getText().toString(),
                                            contacsId);
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    dialog.cancel();
                                }
                            }).create();
        case DIALOG_TEXT_ADDCONTACT:
            LayoutInflater factorys = LayoutInflater.from(this);
            final View textEntrysView = factorys.inflate(
                    R.layout.alert_dialog_text_entry, null);
            LinearLayout mLinearsLayout = (LinearLayout) textEntrysView;
            final TextView mUserNames = (TextView) mLinearsLayout.getChildAt(1);
            final TextView mEmails = (TextView) mLinearsLayout.getChildAt(3);

            return new AlertDialog.Builder(ContactsEmailOwner.this)
                    .setTitle(R.string.contacts_add)
                    .setView(textEntrysView)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    String name = mUserNames.getText().toString();
                                    String email = mEmails.getText().toString();
                                    if(name == null || name.equals("") || email == null || email.equals("")){
                                        Toast.makeText(ContactsEmailOwner.this, getString(R.string.newcontact_toasts), Toast.LENGTH_SHORT).show();
                                    }else  if(!Utiles.isEmailAddress(email)){
                                        Toast.makeText(ContactsEmailOwner.this, getString(R.string.contact_email_address_pandan), Toast.LENGTH_SHORT).show();
                                    }else{
                                        ContactsUtils.addEmailContactcMessage(
                                                ContactsEmailOwner.this, name,email
                                                );
                                        startQuery();
                                    }
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    dialog.cancel();
                                }
                            }).create();
        case DIALOG_TEXT_UPDATE:
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.contacts_add))
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setItems(R.array.contacts_email_update,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (which == 0) {
                                        showDialog(DIALOG_TEXT_ENTRY);
                                    } else {
                                        ContactsUtils.deleteContactsOwnerById(
                                                ContactsEmailOwner.this, contacsId);
                                    }

                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(getString(R.string.cancel), null).show();
            break;
        }
        return null;
    }
    public void startQuery() {
        Uri queryUri = EmailContent.EmailContact.CONTENT_URI;
        mQueryHandler.startQuery(45, null, queryUri, null, "", null, order);
    }

    class MessageEmailListAdapter extends CursorAdapter implements
            OnScrollListener {
        private Context mContext;
        private LayoutInflater mInflater;
        private Drawable mSelectedIconOn;
        private Drawable mSelectedIconOff;
        private HashSet<String> mChecked = new HashSet<String>();
        private boolean visible;

        public MessageEmailListAdapter(Context context) {
            super(context, null, true);
            mContext = context;
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            Resources resources = context.getResources();
            mSelectedIconOn = resources
                    .getDrawable(R.drawable.btn_check_buttonless_dark_on);
            mSelectedIconOff = resources
                    .getDrawable(R.drawable.btn_check_buttonless_dark_off);

        }


        public Set<String> getSelectedSet() {
            return mChecked;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactsEmailListOwnerItem itemView = (ContactsEmailListOwnerItem) view;
            itemView.bindViewInit(this, true);

            itemView.mMessageId = cursor.getLong(cursor
                    .getColumnIndex(EmailContent.EmailContact.ID));
            itemView.mUser = cursor.getString(cursor
                    .getColumnIndex(EmailContent.EmailContact.USERNAME));
            itemView.mEmail = cursor.getString(cursor
                    .getColumnIndex(EmailContent.EmailContact.EMAIL));

            String message = getMessage(itemView.mUser, itemView.mEmail);
            itemView.mSelected = mChecked.contains(message);

            TextView titleView = (TextView) view.findViewById(R.id.title);
            titleView.setText(itemView.mUser);

            TextView nameView = (TextView) view.findViewById(R.id.display_name);
            nameView.setText(itemView.mEmail);

            ImageView selectedView = (ImageView) view
                    .findViewById(R.id.selected);
            selectedView.setImageDrawable(itemView.mSelected ? mSelectedIconOn
                    : mSelectedIconOff);

            TextView firstCharHintTextView = (TextView) view
                    .findViewById(R.id.text_first_char_hint);

            char previewChar;
            if (cursor.getPosition() - 1 == -1) {
                previewChar = ' ';
            } else {
                previewChar = mcharList[cursor.getPosition() - 1];
            }
            char currentChar = mcharList[cursor.getPosition()];

            if (currentChar != previewChar) {
                firstCharHintTextView.setVisibility(View.VISIBLE);
                firstCharHintTextView.setText(String.valueOf(currentChar));
            } else {
                firstCharHintTextView.setVisibility(View.GONE);
            }

        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.contacts_emailowner_list_item,
                    parent, false);
        }

        public void updateSelected(ContactsEmailListOwnerItem itemView,
                boolean newSelected) {
            ImageView selectedView = (ImageView) itemView
                    .findViewById(R.id.selected);
            selectedView.setImageDrawable(newSelected ? mSelectedIconOn
                    : mSelectedIconOff);

            // Set checkbox state in list, and show/hide panel if necessary
            String message = getMessage(itemView.mUser, itemView.mEmail);
            if (newSelected) {
                mChecked.add(message);
            } else {
                mChecked.remove(message);
            }

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (visible) {
                txtOverlay.setText(String.valueOf(mcharList[firstVisibleItem
                        + (visibleItemCount >> 1)]));
                txtOverlay.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            visible = true;
            if (scrollState == ListView.OnScrollListener.SCROLL_STATE_IDLE) {
                txtOverlay.setVisibility(View.INVISIBLE);
                visible = false;
            }
        }

    }

    public String getMessage(String mUserName, String mEmail) {
        StringBuffer mMessage = new StringBuffer();
        mMessage.append("\"");
        mMessage.append(mUserName);
        mMessage.append("\"");
        mMessage.append("<");
        mMessage.append(mEmail);
        mMessage.append(">");
        return mMessage.toString();
    }

    public Cursor doFilter() {
        final ContentResolver resolver = getContentResolver();
        return resolver.query(EmailContent.EmailContact.CONTENT_URI, null, "",
                null, order);
    }

    public void selectMessageAll() {
        Set<String> mChecked = mAdapter.getSelectedSet();
        Cursor c = doFilter();

        while (c.moveToNext()) {
            String email = c
                    .getString(c
                            .getColumnIndex(EmailContent.EmailContact.EMAIL));
            String mUser = c.getString(c
                    .getColumnIndex(EmailContent.EmailContact.USERNAME));

            mChecked.add(getMessage(mUser, email));
        }
        c.close();
        ContactsEmailOwner.this.mAdapter.notifyDataSetChanged();
    }

    private void onDeselectAll() {
        mAdapter.getSelectedSet().clear();
        mListView.invalidateViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_newcontact:

            showDialog(DIALOG_TEXT_ADDCONTACT);
            
            break;
        case R.id.btn_all_select:
            selectMessageAll();
            break;
        case R.id.btn_cancel_select:
            onDeselectAll();
            break;
        case R.id.btn_confirm:
            StringBuffer mSelected = new StringBuffer();
            String[] mCheckList = mAdapter.getSelectedSet().toArray(
                    new String[0]);
            int length = mCheckList.length;
            for (int i = 0; i < length; i++) {
                mSelected.append(mCheckList[i]);
                mSelected.append(",");
            }
            ContactManageTab mTabMainActivity = (ContactManageTab) getParent();
            mIntent.putExtra(EXTRA_ADD_CONTACTS, mSelected.toString());
            mTabMainActivity.setResult(RESULT_OK, mIntent);
            finish();
            break;
        default:
            break;
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor mCursor = (Cursor) l.getItemAtPosition(position);
        editUser = mCursor.getString(mCursor
                .getColumnIndex(EmailContent.EmailContact.USERNAME));
        editEmail = mCursor
                .getString(mCursor
                        .getColumnIndex(EmailContent.EmailContact.EMAIL));
        contacsId = (int) id;
        showDialog(DIALOG_TEXT_UPDATE);
    }



}