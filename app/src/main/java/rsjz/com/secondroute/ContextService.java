package rsjz.com.secondroute;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.List;


/**
 * Handles Geofencing and running of BackgroundService
 */
public class ContextService extends Service implements LocationListener
{
    public static boolean isHeadingHome;
    LocationManager locationManager;
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
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        currentLat = 0;
        currentLng = 0;
        lastRun = 0;
        isHeadingHome = !intent.getBooleanExtra("home", true); //exited home geofence
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        int transitionType = LocationClient.getGeofenceTransition(intent);
        // Test that a valid transition was reported
        if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER))
        {
            Toast.makeText(this, "Geofence entered. Stopping background service...", Toast.LENGTH_SHORT).show();

            stopActiveTracking();
            stopSelf();
        }
        else // exited
        {
            Toast.makeText(this, "Geofence exited. Active tracking started... acquiring gps signal...", Toast.LENGTH_SHORT).show();

            // Check direction
            startActiveTracking();
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location.hasAccuracy())
            {
                currentLat = (float) location.getLatitude();
                currentLng = (float) location.getLongitude();
                Toast.makeText(this, "location lock acquired. lat=" + currentLat + " lng=" + currentLng, Toast.LENGTH_SHORT).show();

                startIntentService();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }
    public void startActiveTracking()
    {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }
    public void stopActiveTracking()
    {
        locationManager.removeUpdates(this);
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

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}
}
