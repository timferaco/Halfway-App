package com.halfway.halfwayapp.MapRequestHelpers;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.maps.android.SphericalUtil.computeDistanceBetween;
import static com.google.maps.android.SphericalUtil.interpolate;

/**
 * Created by Vishal on 10/20/2018.
 * Used by Tim & Paul to help with Directions URL Call
 * Found @https://github.com/Vysh01/android-maps-directions/blob/master/app/src/main/java/com/thecodecity/mapsdirection/directionhelpers/PointsParser.java
 */

public class PointsParser extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
    TaskLoadedCallback taskCallback;
    String directionMode = "driving";

    public PointsParser(Context mContext, String directionMode) {
        this.taskCallback = (TaskLoadedCallback) mContext;
        this.directionMode = directionMode;
    }

    // Parsing the data in non-ui thread
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("mylog", jsonData[0].toString());
            DataParser parser = new DataParser();
            Log.d("mylog", parser.toString());

            // Starts parsing data
            routes = parser.parse(jObject);
            Log.d("mylog", "Executing routes");
            Log.d("mylog", routes.toString());

        } catch (Exception e) {
            Log.d("mylog", e.toString());
            e.printStackTrace();
        }
        return routes;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> result) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;
        // Traversing through all the routes
        for (int i = 0; i < result.size(); i++) {
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();
            // Fetching i-th route
            List<HashMap<String, String>> path = result.get(i);
            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);
                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);
                points.add(position);
            }

            System.out.println(points.size());
            //Midpoint is below
            LatLng midpoint = findMidPoint(points);
            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            if (directionMode.equalsIgnoreCase("walking")) {
                lineOptions.width(10);
                lineOptions.color(Color.MAGENTA);
            } else {
                lineOptions.width(20);
                lineOptions.color(Color.BLUE);
            }
            Log.d("mylog", "onPostExecute lineoptions decoded");
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            //mMap.addPolyline(lineOptions);
            taskCallback.onTaskDone(lineOptions);

        } else {
            Log.d("mylog", "without Polylines drawn");
        }
    }

    LatLng findMidPoint(ArrayList<LatLng> points) {


        LatLng temp = points.get(0);

        ArrayList<LatLng> start = new ArrayList<>();
        ArrayList<LatLng> end = new ArrayList<>();

        //We need some error handling here for points less than size 4

        start.add(points.remove(0));
        start.add(points.remove(0));

        //Take Last two endpoints to initially tell time.
        end.add(points.remove(points.size()-1));
        end.add(points.remove(points.size()-1));

        double startTime = computeDistanceBetween(start.get(0), start.get(1));
        double endTime = computeDistanceBetween(end.get(0), end.get(1));

        while(true) {
            Log.d("START TIME:", Double.toString(startTime));
            Log.d("END TIME:", Double.toString(endTime));
            double addedMeters = 0;
            //If there are no points left, then the temp point is the midpoint
            if (points.size() == 0) {
                break;
            }

            //If there is a higher time for the starting person, add
            if (startTime > endTime) {
                temp = points.get(points.size() - 1);
                addedMeters = computeDistanceBetween(end.get(end.size() - 1), temp);
                if (addedMeters > 200) {
                    //Adds to the end of list a point inbetween temp and the end of size
                    points.add(interpolate(end.get(end.size() - 1), temp, .5));
                } else {
                    //Removes the temp point
                    points.remove(points.size()-1);
                    end.add(temp);
                    endTime += addedMeters;
             }

            } else {
                temp = points.get(0);
                addedMeters = computeDistanceBetween(start.get(start.size() - 1), temp);
                if (addedMeters > 200) {
                     points.add(0,interpolate(start.get(start.size() - 1), temp, .5));
                } else {
                    points.remove(0);
                    start.add(temp);
                    startTime += addedMeters;
                }
            }
        }
        Log.d("START", Integer.toString(start.size()));
        Log.d("END", Integer.toString(end.size()));
        Log.d("MIDPOINT", temp.toString());
        return temp;
    }
}

