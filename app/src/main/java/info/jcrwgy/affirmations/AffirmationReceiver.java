package info.jcrwgy.affirmations;
import android.content.Context;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Random;

public class AffirmationReceiver extends BroadcastReceiver {
    private static final String TAG = "AffirmationReceiver";

    @Override
    public void onReceive(Context context, Intent intent){
        Log.d(TAG, "received intent");
        String mAff = (String)intent.getCharSequenceExtra(
            Intent.EXTRA_PROCESS_TEXT);
        Log.d(TAG, "maff over here: "+mAff);
        createAffirmationNotification(context, mAff);

    }

    public void createAffirmationNotification(Context context, String mAff){
        Log.d(TAG, "createAffirmationNotification is executing");
        Log.d(TAG, "selected affirmation: "+mAff);
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle("Your affirmation:")
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(mAff)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(mAff));


        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, AffirmationsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(AffirmationsActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
            0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);


        NotificationManager mNotificationManager =
            (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());

        // loop again
        // see if the service is up
        if(AffirmationsActivity.serviceRunningCheck(context)){
            context.sendBroadcast(new Intent("AFF_SENT"));
        } else {
            Intent intent = new Intent(context, AffirmationService.class);
            context.startService(intent);
        }
    }
}
