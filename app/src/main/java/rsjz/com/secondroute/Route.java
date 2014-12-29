package rsjz.com.secondroute;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Ryan on 10/18/2014.
 */
public class Route {
    public int durationMinutes;
    public ArrayList<String> instructions;
    public ArrayList<LatLng> path;
    public LatLngBounds latLngBounds;
}
