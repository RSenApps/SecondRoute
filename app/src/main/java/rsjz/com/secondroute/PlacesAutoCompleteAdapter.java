package rsjz.com.secondroute;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.android.gms.location.LocationClient;

/**
 * Created by Ryan on 9/20/2014.
 */
public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private String[][] resultList;
    private LocationClient mLocationClient;

    public PlacesAutoCompleteAdapter(Context context, LocationClient mLocationClient, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mLocationClient = mLocationClient;
    }

    @Override
    public int getCount() {
        return resultList.length;
    }

    @Override
    public String getItem(int index) {
        return resultList[index][0];
    }

    public String getPlaceID(int index) {
        return resultList[index][1];
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the autocomplete results.
                    resultList = GoogleMapsAPI.autocomplete(constraint.toString(), mLocationClient);

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.length;
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
