package rsjz.com.secondroute;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;


public class ChoosePreferredRouteActivity extends Activity {
    ArrayList<Route> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_preferred_route);
        final boolean home = getIntent().getBooleanExtra("home", true);
        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                View[] cards = new View[] {findViewById(R.id.card1), findViewById(R.id.card2), findViewById(R.id.card3)};
                int[] mapIds = new int[] {R.id.mapCard1, R.id.mapCard2, R.id.mapCard3};
                int index = 0;
                for (final Route route: routes)
                {
                    View v = cards[index];
                    v.setVisibility(View.VISIBLE);
                    MapFragment mapFragment = ((MapFragment) getFragmentManager().findFragmentById(mapIds[index]));

                    final GoogleMap map = mapFragment.getMap();
                    map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            String key;
                            if (home) {
                                key = "Home";
                            } else {
                                key = "Work";
                            }
                            String instructionsString = "";
                            for (String instruction : route.instructions)
                            {
                                instructionsString += instruction + ";";
                            }
                            instructionsString = instructionsString.substring(0, instructionsString.length()-1);
                            PreferenceManager.getDefaultSharedPreferences(ChoosePreferredRouteActivity.this).edit().putString("preferredRoute"+key, instructionsString).commit();
                            String pathString = "";
                            for (LatLng path : route.path)
                            {
                                pathString += path.latitude + ";" + path.longitude + ";";
                            }
                            pathString = pathString.substring(0, pathString.length()-1);

                            PreferenceManager.getDefaultSharedPreferences(ChoosePreferredRouteActivity.this).edit()
                                    .putString("path"+key, pathString)
                                    .putFloat(key+"nelat", (float) route.latLngBounds.northeast.latitude)
                                    .putFloat(key+"nelng", (float) route.latLngBounds.northeast.longitude)
                                    .putFloat(key+"swlat", (float) route.latLngBounds.southwest.latitude)
                                    .putFloat(key+"swlng", (float) route.latLngBounds.southwest.longitude)
                                    .commit();
                            finish();
                        }
                    });
                    map.setMyLocationEnabled(false);
                    map.setOnMapLoadedCallback  (new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(route.latLngBounds, 100));
                        }
                    });
                    PolylineOptions rectOptions = new PolylineOptions();
                    rectOptions.color(getResources().getColor(android.R.color.holo_red_dark));
                    rectOptions.addAll(route.path);
                    rectOptions.width(20);
                    map.addPolyline(rectOptions);
                    index++;
                }
                return true;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                routes = BingMapsAPI.getListOfPossibleRoutes(ChoosePreferredRouteActivity.this, home);

                handler.sendEmptyMessage(0);
            }
        }).start();
    }


}
