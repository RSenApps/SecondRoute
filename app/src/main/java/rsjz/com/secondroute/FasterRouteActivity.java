package rsjz.com.secondroute;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Locale;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;


public class FasterRouteActivity extends Activity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {
    GoogleSpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    String instruction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faster_route);
        instruction = getIntent().getStringExtra("instruction");
        int differenceInTime = getIntent().getIntExtra("differenceInTime", 0);
        instruction += " to save " + differenceInTime + " minutes.";
        ((TextView) findViewById(R.id.instruction)).setText(instruction);
        findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigate();
            }
        });
        findViewById(R.id.no).getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
        findViewById(R.id.yes).getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
        findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        displayAndroidWearNotification();
        tts = new TextToSpeech(this, this);



    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter("speech-return"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.cancel(001);
        stopService(new Intent(this, SpeechService.class));
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if (intent.getBooleanExtra("yes", true))
            {
                navigate();
            }
            else {
                finish();
            }
        }
    };
    private void displayAndroidWearNotification() {
        int notificationId = 001;
        // Build intent for notification content
        Intent i = getNavigationIntent();
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(this, 0, i, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Faster Route Available")
                        .setContentText(instruction)
                        .setContentIntent(viewPendingIntent);

        // Get an instance of the NotificationManager service
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
                notificationManager.notify(notificationId, notificationBuilder.build());
    }

    public void navigate()
    {
        startActivity(getNavigationIntent());
        finish();
    }
    private Intent getNavigationIntent()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float lat = prefs.getFloat("homelat", 0);
        float lng = prefs.getFloat("homelng", 0);
        if (!ContextService.isHeadingHome) //travelling to work
        {
            lat = prefs.getFloat("worklat", 0);
            lng = prefs.getFloat("worklng", 0);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lng));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }



    @Override
    public void onInit(int status) {

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
        startService(new Intent(this, SpeechService.class));
    }
    private void speakOut() {

        HashMap<String, String> params = new HashMap<String, String>();

        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"stringId");
        tts.speak(instruction + " Would you like to start navigation?", TextToSpeech.QUEUE_FLUSH, params);
    }
}
