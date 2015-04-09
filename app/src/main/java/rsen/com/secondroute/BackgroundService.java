package rsen.com.secondroute;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * Handles Route Comparison and TRACKING
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class BackgroundService extends IntentService
{
    Handler mHandler;
    @Override
    public void onCreate() {
        Crashlytics.start(this);
        super.onCreate();
        mHandler = new Handler();
    }

    public BackgroundService()
    {
        super("BackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        //perform all code here
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float lat = prefs.getFloat("homelat", 0);
        float lng = prefs.getFloat("homelng", 0);
        if (!ContextService.isHeadingHome) //travelling to work
        {
            lat = prefs.getFloat("worklat", 0);
            lng = prefs.getFloat("worklng", 0);
        }
        ArrayList<Route> pr = BingMapsAPI.getListOfPossibleRoutes(intent.getFloatExtra("lat", 0), intent.getFloatExtra("lng", 0), lat, lng);
        List<LatLng> cr = (List<LatLng>) BingMapsAPI.getPreferredDirectionsList(this, ContextService.isHeadingHome);
        if (pr != null && cr != null) //no network or error in network
        {
            double maxConfidenceScore = 0;
            Route routeWithMaxConfidence = null;
            for (Route route : pr) {
                double confidenceScore = compareRoutes(route.maneuverPoints, cr);
                MyLog.l("Route confidence: " + confidenceScore, this);
                if (confidenceScore > maxConfidenceScore) {
                    maxConfidenceScore = confidenceScore;
                    routeWithMaxConfidence = route;
                }
            }
            MyLog.l("Max confidence (>.8): " + maxConfidenceScore, this);

            if (maxConfidenceScore >= .8) { //need to find match with original route, but also the fastest route can't be the original
                final double similarityScore = compareRoutes(pr.get(0).maneuverPoints, cr);
                MyLog.l("Similarity score (<.9): " + similarityScore, this);

                if (similarityScore <= .9) {
                    int timeDifference = routeWithMaxConfidence.durationMinutes - pr.get(0).durationMinutes;
                    int minDifference = PreferenceManager.getDefaultSharedPreferences(this).getInt("minDifference", 5);
                    MyLog.l("Difference in Time: " + timeDifference, this);

                    if (timeDifference >= minDifference) {
                        Intent i = new Intent(this, FasterRouteActivity.class);
                        MyLog.l("Faster route found that is " + (routeWithMaxConfidence.durationMinutes - pr.get(0).durationMinutes) + " faster", this);
                        for (String instruction : pr.get(0).instructions)
                        {
                            MyLog.l(instruction, this);
                        }
                        i.putExtra("instruction", pr.get(0).instructions.get(0) + " then " + pr.get(0).instructions.get(1));
                        i.putExtra("path", pr.get(0).path);
                        i.putExtra("latlngbox", pr.get(0).latLngBounds);
                        i.putExtra("differenceInTime", routeWithMaxConfidence.durationMinutes - pr.get(0).durationMinutes);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        Intent service = new Intent(this, ContextService.class);
                        stopService(service);
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(BackgroundService.this, "The fastest route was too similar to the preferred route. Max was .9 and similarity score was: " + similarityScore, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                final double mConfidence = maxConfidenceScore;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BackgroundService.this, "No match was found with preferred route. Threshold was .9 and maximum confidence was: " + mConfidence, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }



    private double compareRoutes(List<LatLng> _pr, List<LatLng> _cr)
    {
        double matchpercentage = 0.0;
        //less than, not less than or equal to so as to ignore the first direction
        for(int i = Math.min(_pr.size(), _cr.size())-1; i >= 0  ; i--)
        {
            LatLng preferredRouteManeuverPoint = _pr.get(i);
            LatLng currentRouteManeuverPoint = _cr.get(i);

            if (Math.abs(preferredRouteManeuverPoint.latitude - currentRouteManeuverPoint.latitude) < .0001 && Math.abs(preferredRouteManeuverPoint.longitude - currentRouteManeuverPoint.longitude) < .0001 )
            {
                matchpercentage++;
            }
            else
            {
                MyLog.l("Failed to match on: Preffered: " + _pr.get(i) + " Current: " + _cr.get(i), this);
                return (matchpercentage) / Math.min(_pr.size(), _cr.size());
            }
        }
        return matchpercentage / Math.min(_pr.size(), _cr.size());
    }
}
