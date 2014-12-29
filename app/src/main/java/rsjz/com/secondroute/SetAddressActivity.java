package rsjz.com.secondroute;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;


public class SetAddressActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationClient.OnAddGeofencesResultListener, LocationClient.OnRemoveGeofencesResultListener, GoogleMap.OnMapClickListener {
    boolean home;
    LocationClient mLocationClient;
    GoogleMap map;
    double lat = 0;
    double lng = 0;
    String placeName;
    boolean followUser;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_address);
        home = getIntent().getBooleanExtra("home", true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (home)
        {
            placeName = prefs.getString("home_address", "");
            lat = prefs.getFloat("homelat", 0);
            lng = prefs.getFloat("homelng", 0);
        }
        else
        {
            placeName = prefs.getString("work_address", "");
            lat = prefs.getFloat("worklat", 0);
            lng = prefs.getFloat("worklng", 0);
        }
        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.location_input);
        autoCompView.setText(placeName);
        if (GoogleMapsAPI.servicesConnected(this))
        {
            MapFragment mapFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));

            map = mapFragment.getMap();
            map.setTrafficEnabled(true);
            map.setOnMapClickListener(this);
            map.setMyLocationEnabled(true);


            mLocationClient = new LocationClient(this, this, this);

            findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (placeName == null || placeName.length() < 1 || lat == 0 || lng == 0) {
                        Toast.makeText(SetAddressActivity.this, "Please enter a location...", Toast.LENGTH_LONG).show();
                    } else {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SetAddressActivity.this);
                        if (home)
                        {
                            prefs.edit().putFloat("homelat", (float) lat)
                                    .putFloat("homelng", (float) lng)
                                    .putString("home_address", placeName).commit();

                        }
                        else {
                            prefs.edit().putFloat("worklat", (float) lat)
                                    .putFloat("worklng", (float) lng)
                                    .putString("work_address", placeName).commit();
                        }
                        prefs.edit().putString("preferredRouteHome", "")
                                .putString("preferredRouteWork", "")
                                .putString("pathHome", "")
                                .putString("pathWork", "")
                                .commit();

                        String key = "home";
                        if (!home)
                        {
                            key = "work";
                        }
                        mLocationClient.removeGeofences(Collections.singletonList(key), SetAddressActivity.this);
                        // Send a request to add the current geofences


                    }
                }
            });

        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop()
    {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle dataBundle)
    {
        Location location = mLocationClient.getLastLocation();
        mLocationClient.requestLocationUpdates(LocationRequest.create().setExpirationDuration(3000), this);
        if (lat == 0 || lng == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
        else {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
            MarkerOptions options = new MarkerOptions();
            options.position(new LatLng(lat, lng));
            map.addMarker(options);
        }
        // Zoom in the Google Map
        map.animateCamera(CameraUpdateFactory.zoomTo(12));

        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.location_input);
        final PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this, mLocationClient, android.R.layout.simple_list_item_1);
        autoCompView.setAdapter(adapter);
        autoCompView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                final Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        map.clear();
                        // Zoom in the Google Map
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom((LatLng) message.obj, 15));
                        MarkerOptions options = new MarkerOptions();
                        options.position((LatLng) message.obj);
                        options.title(adapter.getItem(i));
                        placeName = adapter.getItem(i);
                        map.addMarker(options);
                        return true;
                    }
                });
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = handler.obtainMessage();
                        LatLng latLng = GoogleMapsAPI.getLocationForPlace(adapter.getPlaceID(i));
                        lat = latLng.latitude;
                        lng = latLng.longitude;
                        message.obj = latLng;
                        handler.sendMessage(message);
                    }
                });
                thread.start();
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (followUser) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected()
    {
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        9000);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(this, connectionResult.getErrorCode(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * -------------------- All Geofencing Code ---------------------------------------
     *
     * */

    @Override
    public void onAddGeofencesResult(int i, String[] strings)
    {
        finish();
        // Completed Added Geofencing
    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int i3, String[] strings) {
        ArrayList<Geofence> geofences = new ArrayList<Geofence>();
        String key = "home";
        if (!home)
        {
            key = "work";
        }

        geofences.add(new SimpleGeofence(key, lat, lng, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).toGeofence());

        Intent i = new Intent(SetAddressActivity.this, ContextService.class);
        i.putExtra("home", home);
        PendingIntent intent = PendingIntent.getService(SetAddressActivity.this, 0, i, 0);

        mLocationClient.addGeofences(
                geofences, intent, SetAddressActivity.this);
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
        lat = latLng.latitude;
        lng = latLng.longitude;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        map.clear();
        MarkerOptions options = new MarkerOptions();
        options.position(latLng);
        placeName = lat + ", " + lng;
        map.addMarker(options);
        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.location_input);
        autoCompView.setText(placeName);
    }

    /**
     * A single Geofence object, defined by its center and radius.
     */
    public class SimpleGeofence
    {
        // Instance variables
        private final String mId;
        private final double mLatitude;
        private final double mLongitude;
        private final float mRadius;
        private long mExpirationDuration;
        private int mTransitionType;

        /**
         * @param geofenceId The Geofence's request ID
         * @param latitude Latitude of the Geofence's center.
         * @param longitude Longitude of the Geofence's center.
         * @param transition Type of Geofence transition.
         */
        public SimpleGeofence(
                String geofenceId,
                double latitude,
                double longitude,
                int transition) {
            // Set the instance fields from the constructor
            this.mId = geofenceId;
            this.mLatitude = latitude;
            this.mLongitude = longitude;
            this.mRadius = 500;
            this.mExpirationDuration = Geofence.NEVER_EXPIRE;
            this.mTransitionType = transition;
        }
        // Instance field getters
        public String getId()
        {
            return mId;
        }
        public double getLatitude()
        {
            return mLatitude;
        }
        public double getLongitude()
        {
            return mLongitude;
        }
        public float getRadius()
        {
            return mRadius;
        }
        public long getExpirationDuration()
        {
            return mExpirationDuration;
        }
        public int getTransitionType()
        {
            return mTransitionType;
        }
        /**
         * Creates a Location Services Geofence object from a
         * SimpleGeofence.
         *
         * @return A Geofence object
         */
        public Geofence toGeofence()
        {
            // Build a new Geofence object
            return new Geofence.Builder()
                    .setRequestId(getId())
                    .setTransitionTypes(mTransitionType)
                    .setCircularRegion(
                            getLatitude(), getLongitude(), getRadius())
                    .setExpirationDuration(mExpirationDuration)
                    .build();
        }
    }
}
