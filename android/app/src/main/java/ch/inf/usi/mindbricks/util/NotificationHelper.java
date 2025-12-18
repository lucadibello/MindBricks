package ch.inf.usi.mindbricks.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import ch.inf.usi.mindbricks.R;

/**
 * Helper class for managing notifications.
 *
 * @author Luca Beltrami
 */
public class NotificationHelper {

    /**
     * Channel ID for notifications.
     */
    private static final String CHANNEL_ID = "pomodoro_channel";

    /**
     * Notification channel name.
     */
    private static final String CHANNEL_NAME = "Pomodoro Timer";

    /**
     * Description for the notification channel.
     */
    private static final String CHANNEL_DESC = "Notification for study and break sessions";

    private final Context context;

    /**
     * Constructor for NotificationHelper.
     *
     * @param ctx the application context
     */
    public NotificationHelper(Context ctx) {
        this.context = ctx;
        createNotificationChannel();
    }

    /**
     * Creates a notification channel for displaying notifications.
     * <p>
     * Help source: <a href="https://stackoverflow.com/questions/45015803/android-o-notification-channels-and-notificationcompat">stackoverflow.com</a>
     */
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESC);
        channel.setVibrationPattern(new long[]{0, 500, 250, 500});
        channel.enableVibration(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Shows a notification with the provided title, message, and notification ID.
     * If the app does not have the POST_NOTIFICATIONS permission, this method will not show the notification.
     *
     * @param title          the title of the notification
     * @param message        the message content of the notification
     * @param notificationId the unique ID for the notification
     */
    public void showNotification(String title, String message, int notificationId) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_coin_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }
}
