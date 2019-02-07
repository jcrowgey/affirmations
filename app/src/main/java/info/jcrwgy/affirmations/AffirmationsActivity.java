package info.jcrwgy.affirmations;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import java.util.Random;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class AffirmationsActivity extends Activity
{
    private static final String TAG = "AffirmationsActivity";
    private static String nextAffirmationStr = "";
    private NextAffirmationUpdateReceiver nextAffirmationUpdateReceiver;

    public static boolean serviceRunningCheck(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
            Integer.MAX_VALUE)) {
            if (AffirmationService.class.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }

    /*
    @Override
    protected void onStart() {
        super.onStart();
        instance = this;
    } */

    @Override
    public void onResume(){
        super.onResume();
        if(nextAffirmationUpdateReceiver == null) {
            nextAffirmationUpdateReceiver = new NextAffirmationUpdateReceiver();
        }
        IntentFilter intentFilter = new IntentFilter("UPDATE_NEXT_AFF");
        registerReceiver(nextAffirmationUpdateReceiver, intentFilter);

        // poll for an update now, in case something happened while we were asleep
        sendBroadcast(new Intent("AFF_ACT_RESUME"));

        // set service toggle correctly
        setServiceToggle();

    }

    @Override
    public void onPause(){
        super.onPause();
        if(nextAffirmationUpdateReceiver != null) unregisterReceiver(
            nextAffirmationUpdateReceiver);
    }



    public void setServiceToggle(){
        Switch onoffs = (Switch) findViewById(R.id.onoffswitch);
        onoffs.setChecked(serviceRunningCheck(getApplicationContext()));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Restore preferences
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        Switch onoffs = (Switch) findViewById(R.id.onoffswitch);

        int p = Affirmations.settings.getInt("delay",60);
        // MAX_TIME = scaleToMs(p)*2; // scale represent the mean, *2 is for a MAX
        // Log.d(TAG, "MAX_TIME set to "+Integer.toString(MAX_TIME));
        seekBar.setProgress(p);

        // Switch handling
        // set it correctly, then assign the change listener
        setServiceToggle();

        onoffs.setOnCheckedChangeListener( new 
            CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, 
                    boolean isChecked) {
                    if (isChecked) {
                        SharedPreferences.Editor editor = Affirmations.settings.edit();
                        editor.putBoolean("onoff",true);
                        editor.commit();
                        
                        // kick off affirmations service (unless it's already started) 
                        startAffirmationService();

                        // purposeful crash
                        // throw new RuntimeException("This is a crash");
                        
                    } else {
                        Log.d(TAG,"Turning it off");
                        SharedPreferences.Editor editor = Affirmations.settings.edit();
                        editor.putBoolean("onoff",false);
                        editor.commit();
                        // end loop
                        // manager.cancel(pintent);
                        endAffirmationService();
                        TextView tv = (TextView) findViewById(R.id.scheduled);
                        tv.setText("");
                    }
                }
            });

        // Frequency handling
        final TextView sbValue = (TextView) findViewById(R.id.seekbarvalue);
        sbValue.setText(formatFrequency(scaleToMs(seekBar.getProgress())));

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
               // Do something here, 
               //if you want to do anything at the start of
               // touching the seekbar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Display the value in textview
                int ms = scaleToMs(progress); 
                sbValue.setText(formatFrequency(ms));
                // MAX_TIME = ms*2;
                SharedPreferences.Editor editor = Affirmations.settings.edit();
                editor.putInt("delay",progress);
                editor.commit();
           }
       });

       TextView tv = (TextView) findViewById(R.id.scheduled);
       tv.setText(nextAffirmationStr);
    }


    public static int scaleToMs(int seekVal){
        // 0--59 :  1 -- 60 minutes
        // 60--103: 15 minute intervals from 1 hr to 12 hrs ( 11*4 )
        // 104 -- 127:  1/2 hour intervals from 12 24 hrs ( 24 )
        // 128 -- 132: 2,3,4,5 days 
        if (seekVal < 60) {
            return (int) TimeUnit.MILLISECONDS.convert((long) seekVal, TimeUnit.MINUTES) + 1;
        } else if (seekVal < 103) {
            long minutes =  60 + ( (seekVal - 59) * 15 );
            return (int) TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
        } else if (seekVal < 128) {
            long minutes = (12 * 60) + ( (seekVal - 102) * 30 );
            return (int) TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
        } else {
            return (int) TimeUnit.MILLISECONDS.convert((long) (seekVal-127), TimeUnit.DAYS);
        }
    }


    public String formatFrequency(int ms) {
        int secs = (int) TimeUnit.SECONDS.convert(ms, TimeUnit.MILLISECONDS);
        if (secs < 2) return "1 second";
        else {
            int mins = (int) TimeUnit.MINUTES.convert(ms, TimeUnit.MILLISECONDS);
            if (mins < 1) {
                return Integer.toString(secs) + " seconds";
            } else {
                int hours = (int) TimeUnit.HOURS.convert(ms, TimeUnit.MILLISECONDS);
                if (hours < 1) {
                    return Integer.toString(mins) + " minutes";
                } else {
                    int days = (int) TimeUnit.DAYS.convert(ms, TimeUnit.MILLISECONDS);
                    if (days < 1) {
                        return Integer.toString(hours) + " hours";
                    } else {
                        return Integer.toString(days) + " days";
                    }
                }
            }
        }
    }

    public void startAffirmationService(){
        // this is called when the user presses the "on" button
        // start service 
        // then try to bind to it to discover the next scheduled 
        // notification

        Intent intent = new Intent(this, AffirmationService.class);
        startService(intent);
    }

    public void endAffirmationService(){
        // tell the bound service to cancel any pending intents and shut itself down
        Intent intent = new Intent(this, AffirmationService.class);
        stopService(intent);
    }


    private class NextAffirmationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
           if (intent.getAction().equals("UPDATE_NEXT_AFF")){
               TextView tv = (TextView) findViewById(R.id.scheduled);
               tv.setText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT));
           }
        }
    }
}
