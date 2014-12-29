import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;

/**
 *
 *
 */
public class MainService extends IntentService
{
    public ArrayList<String> cr; // get from Ryan's objects later
    public ArrayList<String> pr;



    @Override
    protected void onHandleIntent(Intent workIntent) {

    }

    private double compareRoutes()
    {
        double matchpercentage = 0.0;
        for(int i = 0; i < Math.min(pr.size, pr.size); i++) // i starts at 0, ends at smaller number
        {
            if(pr.size - i == cr.size - i)      // begins at end of list and minus i to iterate
            {
                matchpercentage++;
            }
            else
            {
                return matchpercentage / Math.min(pr.size, cr.size);
            }
        }
        return matchpercentage / Math.min(pr.size, cr.size);
    }

    private void stopIntent()
    {
        stopIntent();
    }
}