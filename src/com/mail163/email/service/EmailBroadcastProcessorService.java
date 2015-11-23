package com.mail163.email.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Config;
import android.util.Log;

import com.mail163.email.Email;
import com.mail163.email.Preferences;

/**
 * The service that really handles broadcast intents on a worker thread.
 *
 * We make it a service, because:
 * <ul>
 *   <li>So that it's less likely for the process to get killed.
 *   <li>Even if it does, the Intent that have started it will be re-delivered by the system,
 *   and we can start the process again.  (Using {@link #setIntentRedelivery}).
 * </ul>
 */
public class EmailBroadcastProcessorService extends IntentService {
    public EmailBroadcastProcessorService() {
        // Class name will be the thread name.
        super(EmailBroadcastProcessorService.class.getName());

        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    /**
     * Entry point for {@link EmailBroadcastReceiver}.
     */
    public static void processBroadcastIntent(Context context, Intent broadcastIntent) {
        Intent i = new Intent(context, EmailBroadcastProcessorService.class);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // This method is called on a worker thread.

        final Intent original = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        final String action = original.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            onBootCompleted();

        // TODO: Do a better job when we get ACTION_DEVICE_STORAGE_LOW.
        //       The code below came from very old code....
        } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
            // Stop IMAP/POP3 poll.
            MailService.actionCancel(this);
        } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
            enableComponentsIfNecessary();
        }
    }

    private void enableComponentsIfNecessary() {
        if (Email.setServicesEnabled(this)) {
            // At least one account exists.
            // TODO probably we should check if it's a POP/IMAP account.
            MailService.actionReschedule(this);
        }
    }

    /**
     * Handles {@link Intent#ACTION_BOOT_COMPLETED}.  Called on a worker thread.
     */
    private void onBootCompleted() {
        if (Config.LOGD) {
            Log.d(Email.LOG_TAG, "BOOT_COMPLETED");
        }
        performOneTimeInitialization();

        enableComponentsIfNecessary();

    }

    private void performOneTimeInitialization() {
        final Preferences pref = Preferences.getPreferences(this);
        int progress = pref.getOneTimeInitializationProgress();
        final int initialProgress = progress;

        if (progress < 1) {
            Log.i(Email.LOG_TAG, "Onetime initialization: 1");
            progress = 1;
            
        }

        // Add your initialization steps here.
        // Use "progress" to skip the initializations that's already done before.
        // Using this preference also makes it safe when a user skips an upgrade.  (i.e. upgrading
        // version N to version N+2)

        if (progress != initialProgress) {
            pref.setOneTimeInitializationProgress(progress);
            Log.i(Email.LOG_TAG, "Onetime initialization: completed.");
        }
    }

    private void setComponentEnabled(Class<?> clazz, boolean enabled) {
        final ComponentName c = new ComponentName(this, clazz.getName());
        getPackageManager().setComponentEnabledSetting(c,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
