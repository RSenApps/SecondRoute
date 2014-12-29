package rsen.com.secondroute;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
    public ContextService()
    {
        super();
    }

    @Override
    public void onDestroy()
    {
        stopActiveTracking();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        currentLat = 0;
        currentLng = 0;
        lastRun = 0;
        isHeadingHome = !intent.getBooleanExtra("home", true); //exited home geofence
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onEnteredGeofences(String[] geofenceIds) {
        Toast.makeText(this, "Geofence entered. Stopping background service...", Toast.LENGTH_SHORT).show();
        MyLog.l("Geofence entered. Stopping background service...", this);
        stopActiveTracking();
        stopSelf();
    }

    @Override
    protected void onExitedGeofences(String[] geofenceIds) {
        Toast.makeText(this, "Geofence exited. Active tracking started... acquiring gps signal...", Toast.LENGTH_SHORT).show();
        MyLog.l("Geofence entered. Stopping background service...", this);

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

    public void startActiveTracking()
    {

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    public void stopActiveTracking()
    {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startIntentService()
    {
        if (currentLat != 0 && currentLng != 0 && System.currentTimeMillis() - lastRun > 60000)
        {
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
        Toast.makeText(this, "location lock acquired. lat=" + currentLat + " lng=" + currentLng, Toast.LENGTH_SHORT).show();

        startIntentService();
    }
    @Override
    public void onConnected(Bundle bundle) {

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

}
