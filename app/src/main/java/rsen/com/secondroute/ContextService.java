package rsen.com.secondroute;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Handles Geofencing and running of BackgroundService
 */
public class ContextService extends ReceiveGeofenceTransitionService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    public static boolean isHeadingHome;
    private GoogleApiClient mGoogleApiClient;

    float currentLat = 0;
    float currentLng = 0;
    long lastRun = 0;
    boolean needToAnnounceETA = false;
    public boolean isDriving = false;
    public ContextService()
    {
        super();
    }

    @Override
    public void onDestroy()
    {
        if (mGoogleApiClient != null) {
            stopActiveTracking();
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null) {
            if (intent.getBooleanExtra("drivingChange", false)) {
                boolean newIsDriving = intent.getBooleanExtra("isdriving", false);
                if (isDriving != newIsDriving) {
                    isDriving = newIsDriving;
                    if (newIsDriving) {
                        MyLog.l("Resuming location tracking", this);
                        resumeLocationTracking();
                    } else if (mGoogleApiClient != null){
                        MyLog.l("Pausing location tracking", this);

                        pauseLocationTracking();
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    protected void onEnteredGeofences(String[] geofenceIds) {
        //Toast.makeText(this, "Geofence entered. Stopping background service...", Toast.LENGTH_SHORT).show();
        MyLog.l("Geofence entered. Stopping background service...", this);
       stopSelf();
    }

    @Override
    protected void onExitedGeofences(String[] geofenceIds) {
        needToAnnounceETA = true;
        isHeadingHome = false;
        for (int i = 0; i < geofenceIds.length; i++)
        {
            if (geofenceIds[i].toLowerCase().equals("work"))
            {
                isHeadingHome = true;
                break;
            }
        }
        String message = "Geofence exited: ";
        if (isHeadingHome)
        {
            message += "work";
        }
        else {
            message += "home";
        }
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        MyLog.l(message, this);
        // Check direction
        startActiveTracking();

            /*
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location.hasAccuracy())
            {
                currentLat = (float) location.getLatitude();
                currentLng = (float) location.getLongitude();
                Toast.makeText(this, "location lock acquired. lat=" + currentLat + " lng=" + currentLng, Toast.LENGTH_SHORT).show();

                startIntentService();
            }
            */
    }

    @Override
    protected void onError(int errorCode) {

    }
    boolean startOrStopTracking = true;
    boolean keepActivityTrackingUnchanged = true;
    boolean keepLocationTrackingUnchanged = false;
    public void startActiveTracking()
    {
        if(mGoogleApiClient == null) {
            currentLat = 0;
            currentLng = 0;
            lastRun = 0;
            //isHeadingHome = !intent.getBooleanExtra("home", true); //exited home geofence
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        startOrStopTracking = true;
        keepActivityTrackingUnchanged = false;
        keepLocationTrackingUnchanged = true;
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ignoreActivity", false))
        {
            keepActivityTrackingUnchanged = true;
            keepLocationTrackingUnchanged = false;
            isDriving = true;
        }
        mGoogleApiClient.connect();
    }

    public void pauseLocationTracking()
    {
        startOrStopTracking = false;
        keepActivityTrackingUnchanged = true;
        keepLocationTrackingUnchanged = false;
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        else {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }
    public void resumeLocationTracking()
    {
        if(mGoogleApiClient == null) {
            currentLat = 0;
            currentLng = 0;
            lastRun = 0;
            //isHeadingHome = !intent.getBooleanExtra("home", true); //exited home geofence
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        startOrStopTracking = true;
        keepActivityTrackingUnchanged = true;
        keepLocationTrackingUnchanged = false;


        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        else {
                    LocationRequest mLocationRequest = LocationRequest.create();
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    mLocationRequest.setInterval(60000); // Update location every second

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }
    public void stopActiveTracking()
    {
        startOrStopTracking = false;
        keepActivityTrackingUnchanged = false;
        keepLocationTrackingUnchanged = false;
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        else {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Intent intent = new Intent(this, ActivityRecognitionService.class);
            PendingIntent callbackIntent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, callbackIntent);
        }
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startIntentService()
    {
        if (!isDriving)
        {
            MyLog.l("Not starting service because user is not driving", this);
        }
        if (currentLat != 0 && currentLng != 0 && System.currentTimeMillis() - lastRun > 50000 && isDriving)
        {
            if (needToAnnounceETA)
            {
                needToAnnounceETA = false;
                if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("announceETA", true)) {
                    Intent announceETA = new Intent(this, AnnounceETAService.class);
                    announceETA.putExtra("lat", currentLat);
                    announceETA.putExtra("lng", currentLng);
                    startService(announceETA);
                }
            }
            lastRun = System.currentTimeMillis();
            Intent compareResults = new Intent(this, BackgroundService.class);
            compareResults.putExtra("lat", currentLat);
            compareResults.putExtra("lng", currentLng);
            startService(compareResults);
        }
    }

    public void onLocationChanged(Location location)
    {
        currentLat = (float) location.getLatitude();
        currentLng = (float) location.getLongitude();
        //Toast.makeText(this, "location lock acquired. lat=" + currentLat + " lng=" + currentLng, Toast.LENGTH_SHORT).show();

        startIntentService();
    }
    @Override
    public void onConnected(Bundle bundle) {
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        PendingIntent callbackIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (startOrStopTracking) {
            if (!keepLocationTrackingUnchanged) {
                LocationRequest mLocationRequest = LocationRequest.create();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(60000); // Update location every second

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            if (!keepActivityTrackingUnchanged) {
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 60000, callbackIntent);
            }
        }
        else {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            if (!keepActivityTrackingUnchanged) {
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, callbackIntent);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

}
