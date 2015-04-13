package rsen.com.secondroute;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics.start(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.home_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SetAddressActivity.class);
                i.putExtra("home", true);
                startActivity(i);

            }
        });
        findViewById(R.id.work_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SetAddressActivity.class);
                i.putExtra("home", false);
                startActivity(i);
            }
        });
        findViewById(R.id.copylog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", MyLog.getLog(MainActivity.this));
                clipboard.setPrimaryClip(clip);
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "rsenapps+secondroute@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SecondRoute Log");
                emailIntent.putExtra(Intent.EXTRA_TEXT, MyLog.getLog(MainActivity.this));
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SeekBar minDifference = ((SeekBar) findViewById(R.id.minDifference));
        final TextView minDifferenceDisplay = (TextView) findViewById(R.id.minDifferenceDisplay);
        minDifference.setProgress(prefs.getInt("minDifference", 5) - 2);
        minDifferenceDisplay.setText("" + prefs.getInt("minDifference", 5));
        minDifference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 2; // min value is 2
                prefs.edit().putInt("minDifference", value).apply();
                minDifferenceDisplay.setText(value + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        final AudioManager audioManager =
                (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        SeekBar volume = ((SeekBar) findViewById(R.id.volume));
        volume.setProgress(prefs.getInt("volume", (int)((double)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/4*3)));
        volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("volume", seekBar.getProgress()).apply();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), AudioManager.FLAG_PLAY_SOUND);
            }
        });
        final CheckBox announceETACheck = (CheckBox) findViewById(R.id.announceCheck);
        announceETACheck.setChecked(prefs.getBoolean("announceETA", true));
        announceETACheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("announceETA", isChecked).apply();
            }
        });
        final CheckBox ignoreActivity = (CheckBox) findViewById(R.id.ignoreActivity);
        ignoreActivity.setChecked(prefs.getBoolean("ignoreActivity", false));
        ignoreActivity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("ignoreActivity", isChecked).apply();
            }
        });

    }

    private void setupMaps() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString("pathWork", "").equals(""))
        {
            findViewById(R.id.tapToSetupWork).setVisibility(View.VISIBLE);
            findViewById(R.id.mapCardWorkContainer).setVisibility(View.GONE);
            findViewById(R.id.tapToSetupWork).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (prefs.getFloat("homelat", 0) != 0 && prefs.getFloat("homelng", 0) != 0 && prefs.getFloat("worklat", 0) != 0 && prefs.getFloat("worklng", 0) != 0) {
                        Intent i = new Intent(MainActivity.this, ChoosePreferredRouteActivity.class);
                        i.putExtra("home", false);
                        startActivity(i);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please first enter home and work locations", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else {
            findViewById(R.id.tapToSetupWork).setVisibility(View.GONE);
            findViewById(R.id.mapCardWorkContainer).setVisibility(View.VISIBLE);
            final MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapCardWork);

            final GoogleMap map = mapFragment.getMap();
            map.clear();
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    Intent i = new Intent(MainActivity.this, ChoosePreferredRouteActivity.class);
                    i.putExtra("home", false);
                    startActivity(i);
                }
            });
            map.setMyLocationEnabled(false);
            map.getUiSettings().setAllGesturesEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(false);
            map.setOnMapLoadedCallback  (new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    float coor1 = prefs.getFloat("Workswlat", 0);
                    float coor2 = prefs.getFloat("Workswlng", 0);
                    float coor3 = prefs.getFloat("Worknelat", 0);
                    float coor4 = prefs.getFloat("Worknelng", 0);

                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(new LatLng(coor1, coor2), new LatLng(coor3, coor4)), 50));
                }
            });
            PolylineOptions rectOptions = new PolylineOptions();
            rectOptions.color(getResources().getColor(android.R.color.holo_red_dark));
            List<String> coordinates = Arrays.asList(prefs.getString("pathWork", "").split(";"));
            for (int i = 0; i < coordinates.size(); i+=2)
            {
                rectOptions.add(new LatLng(Double.parseDouble(coordinates.get(i)), Double.parseDouble(coordinates.get(i+1))));
            }
            rectOptions.width(20);
            map.addPolyline(rectOptions);
        }
        if (prefs.getString("pathHome", "").equals(""))
        {
            findViewById(R.id.tapToSetupHome).setVisibility(View.VISIBLE);
            findViewById(R.id.mapCardHomeContainer).setVisibility(View.GONE);
            findViewById(R.id.tapToSetupHome).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (prefs.getFloat("homelat", 0) != 0 && prefs.getFloat("homelng", 0) != 0 && prefs.getFloat("worklat", 0) != 0 && prefs.getFloat("worklng", 0) != 0) {
                        Intent i = new Intent(MainActivity.this, ChoosePreferredRouteActivity.class);
                        i.putExtra("home", true);
                        startActivity(i);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please first enter home and work locations", Toast.LENGTH_SHORT).show();
                    }

                }
        });
        }
        else {
            findViewById(R.id.tapToSetupHome).setVisibility(View.GONE);
            findViewById(R.id.mapCardHomeContainer).setVisibility(View.VISIBLE);
            final MapFragment mapFragmentHome = (MapFragment) getFragmentManager().findFragmentById(R.id.mapCardHome);
            final GoogleMap map = mapFragmentHome.getMap();
            map.clear();
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    Intent i = new Intent(MainActivity.this, ChoosePreferredRouteActivity.class);
                    i.putExtra("home", true);
                    startActivity(i);
                }
            });
            map.setMyLocationEnabled(false);
            map.getUiSettings().setAllGesturesEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(false);

            map.setOnMapLoadedCallback  (new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    float coor1 = prefs.getFloat("Homeswlat", 0);
                    float coor2 = prefs.getFloat("Homeswlng", 0);
                    float coor3 = prefs.getFloat("Homenelat", 0);
                    float coor4 = prefs.getFloat("Homenelng", 0);

                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(new LatLng(coor1, coor2), new LatLng(coor3, coor4)), 50));
                }
            });
            PolylineOptions rectOptions = new PolylineOptions();
            rectOptions.color(getResources().getColor(android.R.color.holo_red_dark));
            List<String> coordinates = Arrays.asList(prefs.getString("pathHome", "").split(";"));
            for (int i = 0; i < coordinates.size(); i+=2)
            {
                rectOptions.add(new LatLng(Double.parseDouble(coordinates.get(i)), Double.parseDouble(coordinates.get(i+1))));
            }
            rectOptions.width(20);
            map.addPolyline(rectOptions);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ((TextView)findViewById(R.id.home_address)).setText(prefs.getString("home_address", "Tap to Set").split(",")[0]);
        ((TextView)findViewById(R.id.work_address)).setText(prefs.getString("work_address", "Tap to Set").split(",")[0]);
        ((TextView)findViewById(R.id.log)).setText(MyLog.getLog(this));
        final Switch enableSwitch = (Switch) findViewById(R.id.enableSwitch);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(enableSwitch.isEnabled()) {
                    prefs.edit().putBoolean("enabled", isChecked).commit();
                    refreshEnabledState();
                    startService(new Intent(MainActivity.this, AddGeofencesService.class));
                    if (!isChecked) {
                        stopService(new Intent(MainActivity.this, ContextService.class));
                    }
                }
            }
        });
        refreshEnabledState();
        setupMaps();
        //((TextView)findViewById(R.id.preferred_route)).setText("Preferred Route to Work: " + prefs.getString("preferredRouteWork", "unset"));
        //((TextView)findViewById(R.id.preferred_route_home)).setText("Preferred Route to Home: " + prefs.getString("preferredRouteHome", "unset"));

/*
       final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                ((TextView)findViewById(R.id.home_address)).setText((String) message.obj);

                return true;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> directionsList = BingMapsAPI.getListOfPossibleRoutes(prefs.getFloat("homelat", 0), prefs.getFloat("homelng", 0), prefs.getFloat("worklat", 0), prefs.getFloat("worklng", 0));
                Message message = handler.obtainMessage();
                message.obj = directionsList.toString();
                handler.sendMessage(message);

            }
        }).start();
        */



    }
    private void refreshEnabledState()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Switch enableSwitch = (Switch) findViewById(R.id.enableSwitch);
        enableSwitch.setEnabled(false);
        if (prefs.getString("preferredRouteHome", "").equals("") || prefs.getString("preferredRouteWork", "").equals("")
                || prefs.getString("pathHome", "").equals("") || prefs.getString("pathWork", "").equals(""))
        {
            //not configured
            ((CardView) findViewById(R.id.configuredCard)).setCardBackgroundColor(getResources().getColor(R.color.not_configured));
            ((TextView) findViewById(R.id.configuredTitle)).setText("Not Configured");
            ((TextView) findViewById(R.id.configuredDetail)).setText("Finish setting up SecondRoute for it to run automatically");
            enableSwitch.setEnabled(false);
            enableSwitch.setChecked(false);
        }
        else if (prefs.getBoolean("enabled", true)) {
            //configured
            ((CardView) findViewById(R.id.configuredCard)).setCardBackgroundColor(getResources().getColor(R.color.configured));
            ((TextView) findViewById(R.id.configuredTitle)).setText("Ready");

            ((TextView) findViewById(R.id.configuredDetail)).setText("SecondRoute will start automatically when you leave your home/work");
            enableSwitch.setChecked(true);
            enableSwitch.setEnabled(true);
        }
        else {
            ((CardView) findViewById(R.id.configuredCard)).setCardBackgroundColor(getResources().getColor(R.color.not_configured));
            ((TextView) findViewById(R.id.configuredTitle)).setText("Disabled");
            ((TextView) findViewById(R.id.configuredDetail)).setText("SecondRoute will not monitor traffic until reenabled");
            enableSwitch.setChecked(false);
            enableSwitch.setEnabled(true);
        }
    }
}
