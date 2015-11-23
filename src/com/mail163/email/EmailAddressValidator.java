
package com.mail163.email;

import com.mail163.email.mail.Address;

import android.widget.AutoCompleteTextView.Validator;

public class EmailAddressValidator implements Validator {
    public CharSequence fixText(CharSequence invalidText) {
        return "";
    }

    public boolean isValid(CharSequence text) {
        return Address.parse(text.toString()).length > 0;
    }
}
