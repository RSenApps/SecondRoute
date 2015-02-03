package rsen.com.secondroute;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public abstract class ReceiveGeofenceTransitionService extends Service {

    /**
     * Sets an identifier for this class' background thread
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.start(this);
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event != null) {

            if (event.hasError()) {
                onError(event.getErrorCode());
            } else {
                int transition = event.getGeofenceTransition();
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    String[] geofenceIds = new String[event.getTriggeringGeofences().size()];
                    for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {
                        geofenceIds[index] = event.getTriggeringGeofences().get(index).getRequestId();
                    }

                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        onEnteredGeofences(geofenceIds);
                    } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        onExitedGeofences(geofenceIds);
                    }
                }
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }


    protected abstract void onEnteredGeofences(String[] geofenceIds);

    protected abstract void onExitedGeofences(String[] geofenceIds);

    protected abstract void onError(int errorCode);
}