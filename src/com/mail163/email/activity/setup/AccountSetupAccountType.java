
package com.mail163.email.activity.setup;

import com.mail163.email.R;
import com.mail163.email.VendorPolicyLoader;
import com.mail163.email.mail.Store;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Account;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
public class AccountSetupAccountType extends Activity implements OnClickListener {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";
    private static final String EXTRA_EAS_FLOW = "easFlow";
    private static final String EXTRA_ALLOW_AUTODISCOVER = "allowAutoDiscover";

    private Account mAccount;
    private boolean mMakeDefault;
    private boolean mAllowAutoDiscover;
    private TextView mLeftTitle;
    public static void actionSelectAccountType(Activity fromActivity, Account account,
            boolean makeDefault, boolean easFlowMode, boolean allowAutoDiscover) {
        Intent i = new Intent(fromActivity, AccountSetupAccountType.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        i.putExtra(EXTRA_EAS_FLOW, easFlowMode);
        i.putExtra(EXTRA_ALLOW_AUTODISCOVER, allowAutoDiscover);
        fromActivity.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mAccount = (Account) intent.getParcelableExtra(EXTRA_ACCOUNT);
        mMakeDefault = intent.getBooleanExtra(EXTRA_MAKE_DEFAULT, false);
        mAllowAutoDiscover = intent.getBooleanExtra(EXTRA_ALLOW_AUTODISCOVER, true);

        // Otherwise proceed into this screen
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.account_setup_account_type);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.list_title);
        
        ((Button)findViewById(R.id.pop)).setOnClickListener(this);
        ((Button)findViewById(R.id.imap)).setOnClickListener(this);
        
        setupTitle();
//        final Button exchangeButton = ((Button)findViewById(R.id.exchange));
//        exchangeButton.setOnClickListener(this);

//        if (isExchangeAvailable()) {
//            exchangeButton.setVisibility(View.VISIBLE);
//            if (VendorPolicyLoader.getInstance(this).useAlternateExchangeStrings()) {
//                exchangeButton.setText(
//                        R.string.account_setup_account_type_exchange_action_alternate);
//            }
//        }
        // TODO: Dynamic creation of buttons, instead of just hiding things we don't need
    }
    private void setupTitle() {
		mLeftTitle = (TextView) findViewById(R.id.title_left_text);
		mLeftTitle.setText(getString(R.string.account_setup_basics_title));
	}
    private void onPop() {
        try {
            URI uri = new URI(mAccount.getStoreUri(this));
            uri = new URI("pop3", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(this, uri.toString());
        } catch (URISyntaxException use) {
            /*
             * This should not happen.
             */
            throw new Error(use);
        }
        AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
        finish();
    }

    /**
     * The user has selected an IMAP account type.  Try to put together a URI using the entered
     * email address.  Also set the mail delete policy here, because there is no UI (for IMAP).
     */
    private void onImap() {
        try {
            URI uri = new URI(mAccount.getStoreUri(this));
            uri = new URI("imap", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(this, uri.toString());
        } catch (URISyntaxException use) {
            /*
             * This should not happen.
             */
            throw new Error(use);
        }
        // Delete policy must be set explicitly, because IMAP does not provide a UI selection
        // for it. This logic needs to be followed in the auto setup flow as well.
        mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
        AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
        finish();
    }

    /**
     * Determine if we can show the "exchange" option
     *
     * TODO: This should be dynamic and data-driven for all account types, not just hardcoded
     * like this.
     */
    private boolean isExchangeAvailable() {
        //EXCHANGE-REMOVE-SECTION-START
        try {
            URI uri = new URI(mAccount.getStoreUri(this));
            uri = new URI("eas", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            Store.StoreInfo storeInfo = Store.StoreInfo.getStoreInfo(uri.toString(), this);
            return (storeInfo != null && checkAccountInstanceLimit(storeInfo));
        } catch (URISyntaxException e) {
        }
        //EXCHANGE-REMOVE-SECTION-END
        return false;
    }

    /**
     * If the optional store specifies a limit on the number of accounts, make sure that we
     * don't violate that limit.
     * @return true if OK to create another account, false if not OK (limit reached)
     */
    /* package */ boolean checkAccountInstanceLimit(Store.StoreInfo storeInfo) {
        // return immediately if account defines no limit
        if (storeInfo.mAccountInstanceLimit < 0) {
            return true;
        }

        // count existing accounts
        int currentAccountsCount = 0;
        Cursor c = null;
        try {
            c = this.getContentResolver().query(
                    Account.CONTENT_URI,
                    Account.CONTENT_PROJECTION,
                    null, null, null);
            while (c.moveToNext()) {
                Account account = EmailContent.getContent(c, Account.class);
                String storeUri = account.getStoreUri(this);
                if (storeUri != null && storeUri.startsWith(storeInfo.mScheme)) {
                    currentAccountsCount++;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // return true if we can accept another account
        return (currentAccountsCount < storeInfo.mAccountInstanceLimit);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pop:
                onPop();
                break;
            case R.id.imap:
                onImap();
                break;
//            case R.id.exchange:
//            	 onImap();
//                break;
        }
    }
}
