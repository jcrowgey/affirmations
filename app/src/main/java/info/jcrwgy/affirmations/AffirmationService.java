package info.jcrwgy.affirmations;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Random;

public class AffirmationService extends Service {
    public static final String TAG  = "AffirmationSchedulerService";
    private static String nextAffirmationStr = "";
    private static PendingIntent pintent;
    private static AlarmManager manager;

    private AffirmationSentReceiver affSentReceiver;


    public class AffirmationServiceBinder extends Binder {
        AffirmationService getService() {
            return AffirmationService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service created");
        if(affSentReceiver == null){
            affSentReceiver = new AffirmationSentReceiver();
        }
        IntentFilter intentFilter = new IntentFilter("AFF_SENT");
        intentFilter.addAction("AFF_ACT_RESUME");
        registerReceiver(affSentReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        // we started up so we go ahead and kick off the loop

        beginAffirmationDelay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(affSentReceiver != null) unregisterReceiver(affSentReceiver);
        manager.cancel(pintent);
        Log.d(TAG, "service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new AffirmationServiceBinder();

    public void beginAffirmationDelay(){
        int p = Affirmations.settings.getInt("delay",60);
        int max = AffirmationsActivity.scaleToMs(p)*2; // *2 for a max
        String mAff = selectRandomAffirmation();
        Log.d(TAG, "random affirmation selected: "+mAff);
        Random r = new Random();
        int timer = r.nextInt(max);
        Log.d(TAG, "Delay started, next affirmation in " + 
                    Integer.toString((int)Math.floor(timer/1000)) + 
                    "seconds; "+Integer.toString(timer)+"ms." );

        pintent = PendingIntent.getBroadcast( this, 0, new 
            Intent("android.intent.action.PROCESS_TEXT")
                .putExtra(Intent.EXTRA_PROCESS_TEXT, mAff)
                .setClass(this ,AffirmationReceiver.class)
            , PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager)( this.getSystemService(
            Context.ALARM_SERVICE ));

        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
            SystemClock.elapsedRealtime() + timer, pintent);

        long when = System.currentTimeMillis()+timer;
        nextAffirmationStr = "Next affirmation scheduled at: "+
            DateUtils.formatDateTime(this, when, 0x00000011);

        sendAffStr();
    }

    private void sendAffStr(){
        sendBroadcast(new Intent("UPDATE_NEXT_AFF")
            .putExtra(Intent.EXTRA_PROCESS_TEXT, nextAffirmationStr));
    }

    public String selectRandomAffirmation() {
        String[] mAffirmations = getResources().getStringArray(
            R.array.affirmations_array);

        Random r = new Random();
        return mAffirmations[r.nextInt(mAffirmations.length)];
    }

    private class AffirmationSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
           if (intent.getAction().equals("AFF_SENT")){
               beginAffirmationDelay();
           } else if(intent.getAction().equals("AFF_ACT_RESUME")){
               sendAffStr();
           }
        }
    }
}
