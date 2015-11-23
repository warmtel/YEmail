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
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mail163.email.Email.Global;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.util.ContactsUtils;

public class ContactsEmail extends ListActivity implements
        View.OnClickListener, TextWatcher, TextView.OnEditorActionListener,
        OnFocusChangeListener, OnTouchListener {
    private static final String EXTRA_ADD_CONTACTS = "addContacts";
    private static final int DIALOG_TEXT_ENTRY = 1;
    private static final int DIALOG_TEXT_UPDATE = 2;
    private static final String order = "sort_key";
    private ListView mListView;
    private WindowManager windowManager;
    private TextView txtOverlay;
    private SearchEditText mSearchEditText;
    private Button mAllSelectButton;
    private Button mCancelSelectButton;
    private Button mConfirmButton;
    private MessageListAdapter mAdapter;
    private QueryHandler mQueryHandler;
    public static char[] mcharList;
    private Intent mIntent;
    private int contacsId;
    private String editUser = "";
    private String editEmail = "";

    public static void actionAddContacts(Activity activity, int requestCode) {
        Intent i = new Intent(activity, ContactsEmail.class);
        activity.startActivityForResult(i, requestCode);
    }

    private static class QueryHandler extends AsyncQueryHandler {
        protected final WeakReference<ContactsEmail> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ContactsEmail>(
                    (ContactsEmail) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            final ContactsEmail activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                mcharList = new char[cursor.getCount()];
                while (cursor.moveToNext()) {
                    int key = cursor.getPosition();
                    String userNameValue = cursor
                            .getString(cursor
                                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
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
        if (Global.skinName.contains(Global.whiteSkin)) {
            setTheme(R.style.Default);
        } else {
            setTheme(R.style.XTheme);
        }
        super.onCreate(savedInstanceState);
        mIntent = getIntent();
        setContentView(R.layout.contacts_search_content);

        setupOverlay();
        setupListView();
        setupSearchView();
        setupButtonView();
        mQueryHandler = new QueryHandler(this);

        mSearchEditText.requestFocus();
        mAdapter.onContentChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mAdapter.onContentChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // error：Activity has leaked window that was originally added.
        // 在window上添加View，当离开当前Activity时报错. 加入如下解决问题.
        // 在关闭Activity的时候，没有关闭AlertDialog，也会引起上述问题
        windowManager.removeView(txtOverlay);
        // if (mAdapter != null && mAdapter.getCursor() != null) {
        // mAdapter.getCursor().close();
        // }
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

        mAdapter = new MessageListAdapter(this);
        setListAdapter(mAdapter);

        mListView.setOnScrollListener(mAdapter);
        mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
        mListView.setSaveEnabled(false);
        // mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);
    }

    /**
     * Configures search UI.
     */
    private void setupSearchView() {
        mSearchEditText = (SearchEditText) findViewById(R.id.search_src_text);
        mSearchEditText.addTextChangedListener(this);
        mSearchEditText.setOnEditorActionListener(this);
    }

    private void setupButtonView() {
        mAllSelectButton = (Button) findViewById(R.id.btn_all_select);
        mCancelSelectButton = (Button) findViewById(R.id.btn_cancel_select);
        mConfirmButton = (Button) findViewById(R.id.btn_confirm);
        mAllSelectButton.setOnClickListener(this);
        mCancelSelectButton.setOnClickListener(this);
        mConfirmButton.setOnClickListener(this);
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
        Logs.d(Logs.LOG_TAG, "contacsId :" + contacsId);
        switch (item.getItemId()) {
        case R.id.delete:
            ContactsUtils.deleteContactsById(this, contacsId);
            break;
        case R.id.edit:
            Cursor mCursor = (Cursor) mAdapter.getItem((int) info.position);
            editUser = mCursor.getString(mCursor
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            editEmail = mCursor
                    .getString(mCursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
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
            return new AlertDialog.Builder(ContactsEmail.this)
                    .setTitle(R.string.contacts_add)
                    .setView(textEntryView)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    Logs.d(Logs.LOG_TAG, "USERNAME :"+mUserName
                                            .getText().toString());
                                    
                                    Logs.d(Logs.LOG_TAG, "email :"+mEmail
                                            .getText().toString());
                                    
                                    ContactsUtils.updateContactsById(
                                            ContactsEmail.this, mUserName
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
                                        ContactsUtils.deleteContactsById(
                                                ContactsEmail.this, contacsId);
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
        Uri queryUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        mQueryHandler.startQuery(42, null, queryUri, null, "", null, order);
    }

    public Cursor doFilter(String filter) {
        final ContentResolver resolver = getContentResolver();
        return resolver.query(getContactFilterUri(filter), null, "", null,
                order);
    }

    private Uri getContactFilterUri(String filter) {
        Uri baseUri;
        if (!TextUtils.isEmpty(filter)) {
            baseUri = Uri.withAppendedPath(
                    ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
                    Uri.encode(filter));
        } else {
            baseUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        }

        return baseUri;
    }

    private String getTextFilter() {
        if (mSearchEditText != null) {
            return mSearchEditText.getText().toString();
        }
        return null;
    }

    /**
     * Dismisses the search UI along with the keyboard if the filter text is
     * empty.
     */
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && TextUtils.isEmpty(getTextFilter())) {
            hideSoftKeyboard();
            onBackPressed();
            return true;
        }
        return false;
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                ((ListActivity) ContactsEmail.this).getCurrentFocus()
                        .getWindowToken(), 0);
    }

    class MessageListAdapter extends CursorAdapter implements OnScrollListener {
        private Context mContext;
        private LayoutInflater mInflater;
        private Drawable mSelectedIconOn;
        private Drawable mSelectedIconOff;
        private HashSet<String> mChecked = new HashSet<String>();
        private boolean visible;

        public MessageListAdapter(Context context) {
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

        @Override
        protected synchronized void onContentChanged() {
            CharSequence constraint = getTextFilter();
            if (!TextUtils.isEmpty(constraint)) {
                Filter filter = getFilter();
                filter.filter(constraint);
            } else {
                startQuery();
            }
        }

        public Set<String> getSelectedSet() {
            return mChecked;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactsEmailListItem itemView = (ContactsEmailListItem) view;
            itemView.bindViewInit(this, true);

            itemView.mMessageId = cursor.getLong(cursor
                    .getColumnIndex(ContactsContract.Contacts._ID));
            itemView.mUser = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            itemView.mEmail = cursor
                    .getString(cursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));

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
            return mInflater.inflate(R.layout.contacts_email_list_item, parent,
                    false);
        }

        public void updateSelected(ContactsEmailListItem itemView,
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

        /**
         * Run the query on a helper thread. Beware that this code does not run
         * on the main UI thread!
         */
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Cursor cursor = doFilter(constraint.toString());

            mcharList = new char[cursor.getCount()];
            while (cursor.moveToNext()) {
                int key = cursor.getPosition();
                String userNameValue = cursor
                        .getString(cursor
                                .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                char c = ContactsUtils.Char2Alpha(userNameValue.charAt(0));
                mcharList[key] = c;
            }
            return cursor;
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

    /**
     * Performs filtering of the list based on the search query entered in the
     * search text edit.
     */
    protected void onSearchTextChanged() {
        Filter filter = mAdapter.getFilter();
        filter.filter(getTextFilter());
    }

    public void selectMessageAll() {
        Set<String> mChecked = mAdapter.getSelectedSet();
        Cursor c = doFilter(getTextFilter());

        while (c.moveToNext()) {
            String email = c
                    .getString(c
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            String mUser = c.getString(c
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            mChecked.add(getMessage(mUser, email));
        }
        c.close();
        ContactsEmail.this.mAdapter.notifyDataSetChanged();
    }

    private void onDeselectAll() {
        mAdapter.getSelectedSet().clear();
        mListView.invalidateViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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

    @Override
    public void afterTextChanged(Editable s) {
        onSearchTextChanged();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            hideSoftKeyboard();
            if (TextUtils.isEmpty(getTextFilter())) {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == getListView() && hasFocus) {
            hideSoftKeyboard();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == getListView()) {
            hideSoftKeyboard();
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        hideSoftKeyboard();

        Cursor mCursor = (Cursor) l.getItemAtPosition(position);
        editUser = mCursor.getString(mCursor
                .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        editEmail = mCursor
                .getString(mCursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
        contacsId = (int) id;
        showDialog(DIALOG_TEXT_UPDATE);
    }

    /**
     * onListItemClick()方法与onItemClick()不能同时用
     */
    // @Override
    // public void onItemClick(AdapterView<?> parent, View view, int position,
    // long id) {
    // Logs.d(Logs.LOG_TAG, "position :" + position);
    //
    // }

}