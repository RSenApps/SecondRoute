package rsjz.com.secondroute;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        findViewById(R.id.test_exit_geofence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, ContextService.class);
                startService(i);
            }
        });
        findViewById(R.id.test_enter_geofence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, ContextService.class);
                stopService(i);
            }
        });
        findViewById(R.id.test_difference_traffic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, FasterRouteActivity.class);
                i.putExtra("instruction", "Turn right onto the ramp of I-5");
                i.putExtra("differenceInTime", 12);
                startActivity(i);
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
        ((TextView)findViewById(R.id.home_address)).setText(prefs.getString("home_address", "unset").split(",")[0]);
        ((TextView)findViewById(R.id.work_address)).setText(prefs.getString("work_address", "unset").split(",")[0]);
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
}
