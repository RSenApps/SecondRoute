package rsen.com.secondroute;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyLog {
    public static void l(String s, Context c) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        int substringStart = 0;
        String previousLog = prefs.getString("log", "Installed");
        if (previousLog.length() > 100000) {
            substringStart = previousLog.length() - 100000;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("M-dd HH:mm:ss");
        Date date = new Date();
        prefs.edit()
                .putString(
                        "log",
                        previousLog.substring(substringStart) + "\n"
                                + dateFormat.format(date) + ": " + s
                )
                .commit();
    }

    public static String getLog(Context c) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        return prefs.getString("log", "Installed");
    }
}
