package com.mgsu.knight_rider_android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/*
    Service class intended to track the user's location while a trip is active.
    It should be started when a user creates a trip as a driver, or when the user joins
    a trip as a passenger. It should stop in both cases if the user's location is within a
    certain range of the destination.
 */

public class TripLocationService extends Service {

    public static boolean isActive = false;

    private static final String TAG = "TRIPGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 10000;
    private static final float LOCATION_DISTANCE = 0f;
    private static final float DESTINATION_RANGE = 400f;

    private SharedPreferences prefs;
    private RequestQueue queue;

    private String userId;
    private double userLat, userLng, userSpeed;
    private String destName;
    private double destLat, destLng;
    private String tripId;
    private String driverId;

    final int DESTINATION_NOTIFICATION_ID = 1;

    TripLocationListener mLocationListener = new TripLocationListener(LocationManager.GPS_PROVIDER);

    public TripLocationService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        Toast.makeText(this, "Service has started!", Toast.LENGTH_SHORT).show();

        prefs = getBaseContext().getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        queue = Volley.newRequestQueue(getBaseContext());

        destName = intent.getStringExtra("destName");
        destLat = Double.parseDouble(intent.getStringExtra("destLat"));
        destLng = Double.parseDouble(intent.getStringExtra("destLng"));
        tripId = String.valueOf(intent.getIntExtra("tripId", -1));
        driverId = intent.getStringExtra("driverId");
        userId = prefs.getString("knight-rider-userId", null);

        isActive = true;

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListener
                );
            } catch (SecurityException ex) {
                Log.i(TAG, "Failed to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.i(TAG, "GPS provider does not exist " + ex.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
//        Toast.makeText(this, "Service has ended", Toast.LENGTH_SHORT).show();
        super.onDestroy();
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        isActive = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class TripLocationListener implements LocationListener {

        public TripLocationListener(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            userLat = location.getLatitude();
            userLng = location.getLongitude();
            userSpeed = location.getSpeed();

            VolleyLog.d("Userid: " + userId +
                        "\nDriverId: " + driverId);

            Location destLocation = new Location(location);
            destLocation.setLatitude(destLat);
            destLocation.setLongitude(destLng);
            float distanceToLocation = location.distanceTo(destLocation);

            /*
            Toast.makeText(getBaseContext(), "Just checking in..." +
                                                "\nuserLat: " + userLat +
                                                "\nuserLng: " + userLng +
                                                "\nDistance to " + destName + " Campus" +
                                                "  from my current location: " +
                                                distanceToLocation,
                                                Toast.LENGTH_LONG).show();
            */
            if (distanceToLocation <= DESTINATION_RANGE) {
                completeTrip();
                stopSelf();
            }
            postLocationToServer(userLat, userLng, userSpeed);
        }

        private void postLocationToServer(double lat, double lng, double speed) {


            String url = getString(R.string.url) + "/locations/" + userId;

            HashMap<String, String> params = new HashMap<>();
            params.put("userId", userId);
            params.put("latitude", String.valueOf(lat));
            params.put("longitude", String.valueOf(lng));
            params.put("speed", String.valueOf(speed));

            JsonObjectRequest postLocationRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            VolleyLog.d(response.toString());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(error.toString());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    //headers.put("Content-Type", application/json");
                    headers.put("Access-Control-Allow-Origin", "*");
                    String token = prefs.getString("knight-rider-token", null);
                    headers.put("X-Authorization", "Bearer " + token);
                    headers.put("Cache-Control", "no-cache");
                    return headers;
                }
            };

            queue.add(postLocationRequest);
        }

        private void completeTrip() {
            displayNotification();

            if (!driverId.equals(userId))
                return;

            String url = getString(R.string.url) + "/completetrip/" + tripId;

            JsonObjectRequest completeTripRequest = new JsonObjectRequest(Request.Method.POST, url,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            VolleyLog.d(response.toString());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(error.toString());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    //headers.put("Content-Type", application/json");
                    headers.put("Access-Control-Allow-Origin", "*");
                    String token = prefs.getString("knight-rider-token", null);
                    headers.put("X-Authorization", "Bearer " + token);
                    headers.put("Cache-Control", "no-cache");
                    return headers;
                }
            };

            queue.add(completeTripRequest);
        }

        private void displayNotification() {

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder notifBuilder;

            if (driverId.equals(userId)) {

                /* The Driver does not get the prompt to leave a review. */

                notifBuilder = new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle("You have arrived!")
                        .setContentText("Welcome to " + destName + " campus!")
                        .setAutoCancel(true);
            } else {

                /* Send a notification to allow passengers to leave a review. */

                //---PendingIntent to launch activity if the user selects this notification
                Intent i = new Intent("com.mgsu.knight_rider_android.RateRide");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("tripId", tripId);
                i.putExtra("driverId", driverId);
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, i, 0);
                notifBuilder = new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle("You have arrived!")
                        .setContentText("Welcome to " + destName + " campus!\n" +
                                        "Tap to leave a review.")
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

            }

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
            nm.notify(DESTINATION_NOTIFICATION_ID, notifBuilder.build());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
