package ch.inf.usi.mindbricks.util;

import android.Manifest;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import ch.inf.usi.mindbricks.R;

public class NotificationHelper {

    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final String CHANNEL_NAME = "Pomodoro Timer";
    private static final String CHANNEL_DESC = "Notification for study and break sessions";

    private final Context context;

    public NotificationHelper(Application application) {
        this.context = application.getApplicationContext();
        createNotificationChannel();
    }

    // help source: https://stackoverflow.com/questions/45015803/android-o-notification-channels-and-notificationcompat?
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
