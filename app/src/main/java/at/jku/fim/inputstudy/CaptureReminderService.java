package at.jku.fim.inputstudy;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import at.jku.fim.phonykeyboard.latin.R;

public class CaptureReminderService extends IntentService {
    public static final String BROADCAST_ACTION_CAPTURE_NOTIFICATION = "at.jku.fim.phonykeyboard.BIOMETRICS_CAPTURE_NOTIFICATION";
    //public static final int CAPTURE_REPEAT_MS = 6 * 60 * 60 * 1000;
    public static final int CAPTURE_REPEAT_MS = 20 * 1000;

    private static final String TAG = "CaptureReminderService";
    private static final int MIN_NOTIFICATION_HOUR = 7;
    private static final int WAIT_FOR_USER_ACTIVE_MS = 10 * 1000;
    private static final int USER_ACTIVE_EARLY_NOTIFICATION_MINUTES = 30;

    private static boolean isScreenOnRegistered;
    private static Calendar nextNotification;

    private final Handler userActiveHandler = new Handler();

    public CaptureReminderService() {
        super("CaptureReminderService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        switch (intent.getStringExtra("originalIntent")) {
            case Intent.ACTION_SCREEN_ON:
                KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (keyguard != null && !keyguard.inKeyguardRestrictedInputMode()) {
                    // Screen was turned on, but because no keyguard is active, we have to wait to see if the
                    // user really is working with the device
                    Runnable userActiveRunnable = new Runnable() {
                        @Override
                        public void run() {
                            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                            boolean isScreenOn;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                                isScreenOn = powerManager.isInteractive();
                            } else {
                                isScreenOn = powerManager.isScreenOn();
                            }
                            if (isScreenOn) {
                                intent.setAction(Intent.ACTION_USER_PRESENT);
                                onHandleIntent(intent);
                            }
                        }
                    };
                    userActiveHandler.postDelayed(userActiveRunnable, WAIT_FOR_USER_ACTIVE_MS);
                    Log.i(TAG, "No keyguard set, emulating ACTION_USER_PRESENT");
                }
                break;
            case Intent.ACTION_USER_PRESENT:
                Calendar notificationWindow = GregorianCalendar.getInstance();
                // Check whether the user discarded the last notification and re-show it
                if (nextNotification == null) {
                    SharedPreferences preferences = getSharedPreferences(StudyActivity.PREFERENCES_NAME, 0);
                    Calendar lastLogin = getLastLogin();
                    if (preferences.getInt("captureCount", 0) > 0 && lastLogin != null) {
                        lastLogin.add(Calendar.MILLISECOND, CAPTURE_REPEAT_MS);
                        if (lastLogin.before(notificationWindow)) {
                            nextNotification = GregorianCalendar.getInstance();
                            Log.i(TAG, "Last notification was ignored, nagging again");
                        }
                    }
                }

                notificationWindow.add(Calendar.MINUTE, USER_ACTIVE_EARLY_NOTIFICATION_MINUTES);
                if (nextNotification != null && nextNotification.before(notificationWindow)) {
                    notificationWindow.add(Calendar.MINUTE, -USER_ACTIVE_EARLY_NOTIFICATION_MINUTES);
                    Log.i(TAG, String.format("Early sending notification, scheduled in %d minutes", Math.round((notificationWindow.getTimeInMillis() - nextNotification.getTimeInMillis()) / 1000 / 60)));

                    cancelNotification(this);
                    Intent notificationIntent = new Intent(this, CaptureNotificationReceiver.class);
                    notificationIntent.setAction(BROADCAST_ACTION_CAPTURE_NOTIFICATION);
                    sendBroadcast(notificationIntent);
                }
                break;
            default:
                SharedPreferences preferences = getSharedPreferences(StudyActivity.PREFERENCES_NAME, 0);
                if (preferences.getInt("captureCount", 0) > 0) {
                    Calendar lastLogin = getLastLogin();
                    Calendar now = GregorianCalendar.getInstance();
                    if (lastLogin != null) {
                        if (now.getTimeInMillis() - lastLogin.getTimeInMillis() > CAPTURE_REPEAT_MS) {
                            scheduleNotification(this, now);
                        } else {
                            lastLogin.add(Calendar.MILLISECOND, CAPTURE_REPEAT_MS);
                            scheduleNotification(this, lastLogin);
                        }
                    }
                }
        }
    }

    private Calendar getLastLogin() {
        try {
            SharedPreferences preferences = getSharedPreferences(StudyActivity.PREFERENCES_NAME, 0);
            Calendar lastLogin = GregorianCalendar.getInstance();
            lastLogin.setTime(SimpleDateFormat.getDateTimeInstance().parse(preferences.getString("lastLogin", null)));
            return lastLogin;
        } catch (ParseException e) {
            Log.e(TAG, "Unexpected lastLogin date format", e);
            return null;
        }
    }

    public static void scheduleNotification(Context context, Calendar time) {
        if (time.get(Calendar.HOUR_OF_DAY) < MIN_NOTIFICATION_HOUR) {
            time.set(Calendar.HOUR_OF_DAY, MIN_NOTIFICATION_HOUR);
        }

        if (!isScreenOnRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            context.getApplicationContext().registerReceiver(new CaptureReminderReceiver(), filter);
            isScreenOnRegistered = true;
        }

        AlarmManager manager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, CaptureNotificationReceiver.class);
        intent.setAction(BROADCAST_ACTION_CAPTURE_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        manager.set(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
        nextNotification = time;
    }

    public static void cancelNotification(Context context) {
        if (nextNotification == null) {
            return;
        }

        AlarmManager manager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, CaptureNotificationReceiver.class);
        intent.setAction(BROADCAST_ACTION_CAPTURE_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        manager.cancel(pendingIntent);
        nextNotification = null;
    }

    public static class CaptureReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startServiceIntent = new Intent(context, CaptureReminderService.class);
            startServiceIntent.putExtra("originalIntent", intent.getAction());
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_USER_PRESENT:
                case Intent.ACTION_SCREEN_ON:
                    context.startService(startServiceIntent);
                    break;
            }
        }
    }

    public static class CaptureNotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            nextNotification = null;

            // Test if app is in foreground - in that case, no notification is necessary
            if (!BackgroundManager.getInstance().isInBackground()) {
                Log.i(TAG, "Suppressing notification, app already in foreground");
                return;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setAutoCancel(true)
                    .setLocalOnly(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getResources().getString(R.string.study_capture_notification_title))
                    .setContentText(context.getResources().getString(R.string.study_capture_notification_text));

            SharedPreferences preferences = context.getSharedPreferences(StudyActivity.PREFERENCES_NAME, 0);
            int captureCount = preferences.getInt("captureCount", 0);
            if (captureCount > 0 && captureCount < 100) {
                builder.setProgress(100, captureCount, false);
            } else {
                builder.setProgress(0, 0, false);
            }

            // Creates an explicit intent for an Activity in your app
            Intent activityIntent = new Intent(context, StudyActivity.class);
            activityIntent.setAction("capture");
            // Sets the Activity to start in a new, empty task
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Creates the PendingIntent
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Puts the PendingIntent into the notification builder
            builder.setContentIntent(notifyPendingIntent);

            NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            // id allows you to update the notification later on.
            notificationManager.notify(0, builder.build());
        }
    }
}
