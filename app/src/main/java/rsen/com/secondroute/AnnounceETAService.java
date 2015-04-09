package rsen.com.secondroute;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * Handles Route Comparison and TRACKING
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AnnounceETAService extends Service implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener
{
    Handler mHandler;
    private TextToSpeech tts;
    private int eta;
    private String place;
    @Override
    public void onCreate() {
        Crashlytics.start(this);
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        //perform all code here
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float lat = prefs.getFloat("homelat", 0);
        float lng = prefs.getFloat("homelng", 0);
        place = "home";
        if (!ContextService.isHeadingHome) //travelling to work
        {
            place = "work";
            lat = prefs.getFloat("worklat", 0);
            lng = prefs.getFloat("worklng", 0);
        }
        Log.d("Announce", "Announce service started");
        new LongOperation().execute(intent.getFloatExtra("lat", 0), intent.getFloatExtra("lng", 0), lat, lng);

        return super.onStartCommand(intent, flags, startId);
    }
    private class LongOperation extends AsyncTask<Float, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Float... params) {
            ArrayList<Route> pr = BingMapsAPI.getListOfPossibleRoutes(params[0], params[1], params[2], params[3]);
            if (pr != null) {
                eta = pr.get(0).durationMinutes;
                Log.d("Announce", "Duration set");

                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
            {
                tts = new TextToSpeech(AnnounceETAService.this, AnnounceETAService.this);
            }
            else {
                stopSelf();
            }
        }

    }



    @Override
    public void onInit(int status) {
        Log.d("Announce", "Status:" + status);
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                tts.setOnUtteranceCompletedListener(this);
                speakOut();
            }

        } else {
        }

    }

    public void onUtteranceCompleted(String utteranceId) {
        stopSelf();
    }

    private void speakOut() {

        HashMap<String, String> params = new HashMap<String, String>();
        Log.d("Announce", "Speaking");

        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"stringId");
        tts.speak("Estimated time to " + place + " is " + eta + " minutes", TextToSpeech.QUEUE_FLUSH, params);
    }
}
