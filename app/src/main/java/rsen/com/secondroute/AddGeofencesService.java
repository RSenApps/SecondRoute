package rsen.com.secondroute;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;

public class AddGeofencesService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GooglePlayServicesClient.OnConnectionFailedListener{
    private GoogleApiClient mGoogleApiClient;

    public AddGeofencesService() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.start(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
    @Override
    public void onConnected(Bundle dataBundle) {
        final ArrayList<String> geofenceKeys = new ArrayList<String>();
        geofenceKeys.add("home");
        geofenceKeys.add("work");
        PendingResult<Status> result = LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceKeys);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {

                    ArrayList<Geofence> geofences = new ArrayList<Geofence>();
                    for (String geofenceKey : geofenceKeys) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AddGeofencesService.this);
                        double lat = prefs.getFloat(geofenceKey + "lat", 0);
                        double lng = prefs.getFloat(geofenceKey + "lng", 0);

                        geofences.add(new SimpleGeofence(geofenceKey, lat, lng, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).toGeofence());
                    }
                    Intent i = new Intent(AddGeofencesService.this, ContextService.class);
                    PendingIntent intent = PendingIntent.getService(AddGeofencesService.this, 0, i, 0);
                    PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofences, intent);
                    result.setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                // Successfully registered
                                Toast.makeText(AddGeofencesService.this, "Geofence creation succeeded, SecondRoute will now run when you leave this location", Toast.LENGTH_LONG).show();
                                stopSelf();
                            }   else{
                                Toast.makeText(AddGeofencesService.this, "Geofence creation failed, please try again later...", Toast.LENGTH_LONG).show();
                            }
                        }

                    });
                } else {
                    // No recovery. Weep softly or inform the user.
                    Toast.makeText(AddGeofencesService.this, "Geofence creation failed, please try again later...", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
         /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */

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
