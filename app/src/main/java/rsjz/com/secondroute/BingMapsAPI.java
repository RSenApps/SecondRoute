package rsjz.com.secondroute;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Ryan on 9/20/2014.
 */
public class BingMapsAPI {
    private static final String MAPS_API_BASE = "http://dev.virtualearth.net/REST/V1";
    private static final String TYPE_ROUTES = "/Routes";
    private static final String MODE_DRIVING = "/Driving";

    private static final String API_KEY = "AmXC0roDXBSoAn6AUz9ScsUWbYrvoqCvjerGZ-Q4O1KxFfea9AHCi3cZ8Prl5aIM";

    public enum TRANSIT_MODE {driving, transit, walking}
    public static List<String> getPreferredDirectionsList(Context context, boolean home)
    {
        String key = "preferredRoute";
        if (home)
        {
            key += "Home";
        }
        else {
            key += "Work";
        }
        String route = PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
        return Arrays.asList(route.split(";"));
    }
    public static ArrayList<Route> getDirectionsList (float lat1, float lng1, float lat2, float lng2)
    {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(MAPS_API_BASE + TYPE_ROUTES + MODE_DRIVING);
            sb.append("?o=json");
            sb.append("&key=" + API_KEY);
            sb.append("&wp.0=" + lat1 + "," + lng1);
            sb.append("&wp.1=" + lat2 + "," + lng2);
            sb.append("&maxSolns=3");

            sb.append("&optmz=timeWithTraffic");
            sb.append("&rpo=none");

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            ArrayList<Route> routesList = new ArrayList<Route>();
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray resourcesArray = jsonObj.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources");
            for (int resourceIndex = 0; resourceIndex < resourcesArray.length(); resourceIndex++) {
                JSONObject resourceObj = resourcesArray.getJSONObject(resourceIndex);
                JSONArray itineraryItems = resourceObj.getJSONArray("routeLegs").getJSONObject(0).getJSONArray("itineraryItems");
                ArrayList<String> instructionList = new ArrayList<String>();
                for (int index = 0; index < itineraryItems.length(); index++) {
                    JSONObject itinerary = itineraryItems.getJSONObject(index);
                    String instruction = itinerary.getJSONObject("instruction").getString("text");
                    if (!instruction.toLowerCase().startsWith("road name changes"))
                    {
                        instructionList.add(instruction);
                    }

                }
                int duration = resourceObj.getInt("travelDurationTraffic");
                String durationUnit = resourceObj.getString("durationUnit");
                float divider = 1;
                if (durationUnit.equals("Second")) {
                    divider = 60;
                } else if (durationUnit.equals("Hour")) {
                    divider = 1 / 60;
                } else if (durationUnit.equals("Day")) {
                    divider = 1 / 60 / 24;
                }
                Route route = new Route();
                route.durationMinutes = (int) Math.round((double) duration / divider);
                route.instructions = instructionList;
                routesList.add(route);

            }
            return routesList;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static ArrayList<Route> getListOfPossibleRoutes(Context context, boolean home)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (home)
        {
            return BingMapsAPI.getListOfPossibleRoutes(prefs.getFloat("worklat", 0), prefs.getFloat("worklng", 0), prefs.getFloat("homelat", 0), prefs.getFloat("homelng", 0));
        }
        else {
            return BingMapsAPI.getListOfPossibleRoutes(prefs.getFloat("homelat", 0), prefs.getFloat("homelng", 0), prefs.getFloat("worklat", 0), prefs.getFloat("worklng", 0));
        }

    }
    public static ArrayList<Route> getListOfPossibleRoutes (float lat1, float lng1, float lat2, float lng2)
    {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(MAPS_API_BASE + TYPE_ROUTES + MODE_DRIVING);
            sb.append("?o=json");
            sb.append("&key=" + API_KEY);
            sb.append("&wp.0=" + lat1 + "," + lng1);
            sb.append("&wp.1=" + lat2 + "," + lng2);
            sb.append("&maxSolns=3");

            sb.append("&optmz=timeWithTraffic");

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            ArrayList<Route> routesList = new ArrayList<Route>();
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray resourcesArray = jsonObj.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources");
            for (int resourceIndex = 0; resourceIndex < resourcesArray.length(); resourceIndex++) {
                JSONObject resourceObj = resourcesArray.getJSONObject(resourceIndex);
                JSONArray bounds = resourceObj.getJSONArray("bbox");

                JSONArray itineraryItems = resourceObj.getJSONArray("routeLegs").getJSONObject(0).getJSONArray("itineraryItems");
                ArrayList<String> instructions = new ArrayList<String>();
                ArrayList<LatLng> path = new ArrayList<LatLng>();

                for (int index = 0; index < itineraryItems.length(); index++) {
                    JSONObject itinerary = itineraryItems.getJSONObject(index);
                    String instruction = itinerary.getJSONObject("instruction").getString("text");
                    if (!instruction.toLowerCase().startsWith("road name changes"))
                    {
                        instructions.add(instruction);
                    }
                    JSONArray coordinates = itinerary.getJSONObject("maneuverPoint").getJSONArray("coordinates");
                    path.add(new LatLng(coordinates.getDouble(0), coordinates.getDouble((1))));

            }
                Route route = new Route();
                route.instructions = instructions;
                route.path = path;
                route.latLngBounds = new LatLngBounds(new LatLng(bounds.getDouble(0), bounds.getDouble(1)), new LatLng(bounds.getDouble(2), bounds.getDouble(3)));
                routesList.add(route);
            }
            return routesList;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
