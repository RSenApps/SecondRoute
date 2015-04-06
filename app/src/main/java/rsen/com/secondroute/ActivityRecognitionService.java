package rsen.com.secondroute;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * Handles Route Comparison and TRACKING
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ActivityRecognitionService extends IntentService{

    private static final String TAG ="ActivityRecognition";

    public ActivityRecognitionService() {
        super("ActivityRecognitionService");
    }

    /**
     * Google Play Services calls this once it has analysed the sensor data
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            MyLog.l("Driving confidence: " + result.getActivityConfidence(DetectedActivity.IN_VEHICLE), this);
            Intent i = new Intent(this, ContextService.class);
            if (result.getActivityConfidence(DetectedActivity.IN_VEHICLE) > 50)
            {
                i.putExtra("isdriving", true);
            }
            else {
                i.putExtra("isdriving", false);
            }
            startService(i);
        }
    }

    /**
     * When supplied with the integer representation of the activity returns the activity as friendly string
     * @return a friendly string of the
     */
    private static String getFriendlyName(int detected_activity_type){
        switch (detected_activity_type ) {
            case DetectedActivity.IN_VEHICLE:
                return "in vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on bike";
            case DetectedActivity.ON_FOOT:
                return "on foot";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.STILL:
                return "still";
            default:
                return "unknown";
        }
    }
}