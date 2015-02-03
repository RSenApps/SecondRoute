package rsen.com.secondroute;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

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
        super.onCreate();
        Crashlytics.start(this);
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
        ArrayList<Route> pr = BingMapsAPI.getDirectionsList(intent.getFloatExtra("lat", 0), intent.getFloatExtra("lng",0), lat, lng);
        List<String> cr = (List<String>) BingMapsAPI.getPreferredDirectionsList(this, ContextService.isHeadingHome);
        double maxConfidenceScore = 0;
        Route routeWithMaxConfidence = null;
        for (Route route : pr)
        {
            double confidenceScore = compareRoutes(route.instructions, cr);
            MyLog.l("Route confidence: " + confidenceScore, this);
            if (confidenceScore > maxConfidenceScore)
            {
                maxConfidenceScore = confidenceScore;
                routeWithMaxConfidence = route;
            }
        }
        MyLog.l("Max confidence (>.8): " + maxConfidenceScore, this);

        if (maxConfidenceScore > .8) { //need to find match with original route, but also the fastest route can't be the original
            final double similarityScore = compareRoutes(pr.get(0).instructions, cr);
            MyLog.l("Similarity score (<.9): " + similarityScore, this);

            if (similarityScore < .9) {
                int timeDifference = routeWithMaxConfidence.durationMinutes - pr.get(0).durationMinutes;
                int minDifference = PreferenceManager.getDefaultSharedPreferences(this).getInt("minDifference", 5);
                MyLog.l("Difference in Time: " + timeDifference, this);

                if (timeDifference >= minDifference) {
                    Intent i = new Intent(this, FasterRouteActivity.class);
                    i.putExtra("instruction", pr.get(0).instructions.get(0));
                    i.putExtra("differenceInTime", routeWithMaxConfidence.durationMinutes - pr.get(0).durationMinutes);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    Intent service = new Intent(this, ContextService.class);
                    stopService(service);
                }
            }
            else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(BackgroundService.this, "The fastest route was too similar to the preferred route. Max was .9 and similarity score was: " + similarityScore, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        else {
            final double mConfidence = maxConfidenceScore;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BackgroundService.this, "No match was found with preferred route. Threshold was .9 and maximum confidence was: " + mConfidence, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    Set<String> ignoreWords = new HashSet<String>(Arrays.asList(
            new String[]{"depart", "turn", "onto", "keep", "take" , "for", "at"}
    ));


    private double compareRoutes(List<String> _pr, List<String> _cr)
    {
        double matchpercentage = 0.0;
        for(int i = 1; i <= Math.min(_pr.size(), _cr.size()) ; i++) // i starts at 0, ends at smaller number
        {
            boolean match = true;
            List<String> preferredRouteWords = Arrays.asList(_pr.get(_pr.size() - i).split(" "));
            List<String> currentRouteWords = Arrays.asList(_cr.get(_cr.size() - i).split(" "));
           removeIgnoreWords(preferredRouteWords);
            removeIgnoreWords(currentRouteWords);
            if (preferredRouteWords.size() != currentRouteWords.size())
            {
                match = false;
            }
            else {
                for (int w = 0; w < preferredRouteWords.size(); w++) {

                        if (!preferredRouteWords.get(w).toLowerCase().equals(currentRouteWords.get(w).toLowerCase())) {
                            match = false;
                            break;
                        }

                }
            }
            if(match)   // begins at end of list and minus i to iterate
            {
                matchpercentage++;
            }
            else
            {
                MyLog.l("Failed to match on: Preffered: " + _pr.get(_pr.size()-i) + " Current: " + _cr.get(_cr.size()-i), this);
                return matchpercentage / Math.min(_pr.size(), _cr.size());
            }
        }
        return matchpercentage / Math.min(_pr.size(), _cr.size());
    }
    private void removeIgnoreWords(List<String> words)
    {
        Iterator<String> prIterator = words.iterator();
        while (prIterator.hasNext())
        {
            String word = prIterator.next();
            if (ignoreWords.contains(word))
            {
                prIterator.remove();
            }
        }
    }
}
