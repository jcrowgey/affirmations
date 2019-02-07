package info.jcrwgy.affirmations;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(mailTo = "jcrowgey@gmail.com", mode = ReportingInteractionMode.SILENT)

public class Affirmations extends Application {
    public static SharedPreferences settings;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = getSharedPreferences(getPackageName() + "_preferences", 
            MODE_PRIVATE);
   }
}
