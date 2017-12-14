package com.mgsu.knight_rider_android;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateRide extends AppCompatActivity {

    /* Request fields */
    private RequestQueue queue;
    private SharedPreferences prefs;
    private String userId;


    /* Input fields */
//    private TextInputEditText originInput;
    private Spinner originSpinner;
//    private TextInputEditText destinationInput;
    private Spinner destinationSpinner;
//    private TextInputEditText pickupInput;
    private Spinner pickupSpinner;
    private Spinner carSpinner;
    private ArrayList<String> carIds;
    private TextView dateTextView;
    private TextView timeTextView;
    private TextInputEditText seatsInput;

    /* Hold the pickup locations for the selected campus */
    String[] pickupArray;
    String[] coordinatesArray;

    /* Request parameters */
    private HashMap<String, Object> originParams = new HashMap<>();
    private HashMap<String, Object> destinationParams = new HashMap<>();

    private double userLat, userLng;

    /* Location Fields */
    private static final String TAG = "TRIPGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0f;
    DriverLocationListener mLocationListener = new DriverLocationListener(LocationManager.GPS_PROVIDER);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride);

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

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        userId = prefs.getString("knight-rider-userId", null);


//        originInput = (TextInputEditText) findViewById(R.id.originInput);
        originSpinner = (Spinner) findViewById(R.id.originInput);
//        destinationInput = (TextInputEditText) findViewById(R.id.destinationInput);
        destinationSpinner = (Spinner) findViewById(R.id.destinationInput);
//        pickupInput = (TextInputEditText)findViewById(R.id.pickupInput);
        pickupSpinner = (Spinner) findViewById(R.id.pickupInput);
        carSpinner = (Spinner) findViewById(R.id.carInput);
        dateTextView = (TextView)findViewById(R.id.dateTextView);
        timeTextView = (TextView)findViewById(R.id.timeTextView);
        seatsInput = (TextInputEditText)findViewById(R.id.seatsInput);


        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupSpinners();

        setDateTimeText();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveRide(View view) {

        //Make some sort of validation code.
        if (carSpinner.getSelectedItemId() <= 0) {
            Toast.makeText(this, "Please pick a car", Toast.LENGTH_SHORT).show();
            return;
        } else if (originSpinner.getSelectedItemId() <= 0) {
            Toast.makeText(this, "Please pick an origin", Toast.LENGTH_SHORT).show();
            return;
        } else if (destinationSpinner.getSelectedItemId() <= 0) {
            Toast.makeText(this, "Please pick a destination", Toast.LENGTH_SHORT).show();
            return;
        } else if (pickupSpinner.getSelectedItemId() <= 0) {
            Toast.makeText(this, "Please pick a pickup location.", Toast.LENGTH_SHORT).show();
            return;
        }

        Validator validator = new Validator();
        if (!validator.checkRideValues(seatsInput)
//            || !validator.checkRideValues(pickupInput)
//            || !validator.checkAddress(originInput)
            /*|| !validator.checkAddress(destinationInput)*/) {
            return;
        }

        GeocodeAddressTask task = new GeocodeAddressTask();
        task.execute();
    }

    private void setupSpinners() {

        /* Assign data to the Origin, Destination, and Car Spinners */


        List<String> originList = new ArrayList<>();
        List<String> destinationList = new ArrayList<>();
        String[] campusArray = getResources().getStringArray(R.array.campus_array);

        originList.add("-Select an origin-");
        destinationList.add("-Select a destination-");
        for (int i = 0; i < campusArray.length; i++) {
            originList.add(campusArray[i]);
            destinationList.add(campusArray[i]);
        }
        ArrayAdapter<String> originAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, originList);
        originAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        originSpinner.setAdapter(originAdapter);

        /* Change the pickup locations depending on the origin */
        originSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1: //Macon
                        pickupArray = getResources().getStringArray(R.array.macon_pickup_locations);
                        coordinatesArray = getResources().getStringArray(R.array.macon_pickup_coordinates);
                        setupPickupSpinner();
                        break;
                    case 2: //Warner Robins
                        pickupArray = getResources().getStringArray(R.array.warner_robins_pickup_locations);
                        coordinatesArray = getResources().getStringArray(R.array.warner_robins_pickup_coordinates);
                        setupPickupSpinner();
                        break;
                    case 3: //Dublin
                        pickupArray = getResources().getStringArray(R.array.dublin_pickup_locations);
                        coordinatesArray = getResources().getStringArray(R.array.dublin_pickup_coordinates);
                        setupPickupSpinner();
                        break;
                    case 4: //Cochran
                        pickupArray = getResources().getStringArray(R.array.cochran_pickup_locations);
                        coordinatesArray = getResources().getStringArray(R.array.cochran_pickup_coordinates);
                        setupPickupSpinner();
                        break;
                    case 5: //Eastman
                        pickupArray = getResources().getStringArray(R.array.eastman_pickup_locations);
                        coordinatesArray = getResources().getStringArray(R.array.eastman_pickup_coordinates);
                        setupPickupSpinner();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, destinationList);
        destinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(destinationAdapter);

        //Send a request to get the user's list of cars.
        final List<String> carList = new ArrayList<String>();
        carIds = new ArrayList<>();
        String userId = prefs.getString("knight-rider-userId", null);
        String url = getString(R.string.url) + "/users/" + userId + "/cars";

        JsonArrayRequest getCarsRequest = new JsonArrayRequest(Request.Method.GET, url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        carList.clear();
                        carList.add("-Select a Car-");
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                String car;
                                car = response.getJSONObject(i).getString("maker") + " - " +
                                      response.getJSONObject(i).getString("type");
                                carList.add(car);
                                carIds.add(response.getJSONObject(i).getString("id"));
                            }
                            ArrayAdapter<String> carAdapter = new ArrayAdapter<String>(getBaseContext(),
                                    android.R.layout.simple_spinner_item, carList);
                            carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            carSpinner.setAdapter(carAdapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d("Error.Response", error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out. Unable to retrieve "
                                                              + "the list of cars.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        )
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                //headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                String token = prefs.getString("knight-rider-token", null);
                headers.put("X-Authorization", "Bearer " + token);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        queue.add(getCarsRequest);
    }

    private void setupPickupSpinner() {
        List<String> pickupList = new ArrayList<>();
        pickupList.add("-Select a pickup location-");
        for (int i = 0; i < pickupArray.length; i++) {
            pickupList.add(pickupArray[i]);
        }
        ArrayAdapter<String> pickupAdapter = new ArrayAdapter<>(getBaseContext(),
                android.R.layout.simple_spinner_item, pickupList);
        pickupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pickupSpinner.setAdapter(pickupAdapter);
    }

    private class GeocodeAddressTask extends AsyncTask<Void, Void, Void> {

        String originStr;
        String destinationStr;
        boolean addressFound = true;

        @Override
        protected void onPreExecute() {

            String[] addressArray = getResources().getStringArray(R.array.campus_addresses);

            originStr = addressArray[(int)originSpinner.getSelectedItemId() - 1];
//            originStr = originInput.getText().toString();
            StringBuilder originBuilder = new StringBuilder();
            for (int i = 0; i < originStr.length(); i++) {
                if (originStr.charAt(i) == ' ')
                    originBuilder.append('+');
                else
                    originBuilder.append(originStr.charAt(i));
            }
            originStr = originBuilder.toString();

            destinationStr = addressArray[(int)destinationSpinner.getSelectedItemId() - 1];
//            destinationStr = destinationInput.getText().toString();
            StringBuilder destBuilder = new StringBuilder();
            for (int i = 0; i < destinationStr.length(); i++) {
                if (destinationStr.charAt(i) == ' ')
                    destBuilder.append('+');
                else
                    destBuilder.append(destinationStr.charAt(i));
            }
            destinationStr = destBuilder.toString();
        }

        @Override
        protected Void doInBackground(Void... params) {

            String url = "https://maps.googleapis.com/maps/api/geocode/json?";
            url += "address=" + originStr;
            url += "&key=" + getString(R.string.google_maps_key);

            JsonObjectRequest originRequest = new JsonObjectRequest(Request.Method.GET, url,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray responseArray = response.getJSONArray("results")
                                        .getJSONObject(0).getJSONArray("address_components");
                                String street = responseArray.getJSONObject(0).getString("long_name") + " "
                                        + responseArray.getJSONObject(1).getString("short_name");
                                String city = responseArray.getJSONObject(2).getString("long_name");
                                String state = responseArray.getJSONObject(4).getString("short_name");
                                String zip = responseArray.getJSONObject(6).getString("long_name");

                                String address = street + ", " +city + ", " +state + ", " +zip;

                                JSONObject latLngObject = response.getJSONArray("results")
                                        .getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                                String lat = latLngObject.getString("lat");
                                String lng = latLngObject.getString("lng");

                                originParams.put("originAddress", address);
                                originParams.put("originCity", city);
                                originParams.put("originState", state);
                                originParams.put("originZip", zip);
                                originParams.put("originLatitude", lat);
                                originParams.put("originLongitude", lng);
                                VolleyLog.d("originParams = " + originParams.toString());

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(error.toString());
                            addressFound = false;
                        }
                    }
            );
            queue.add(originRequest);

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            url = "https://maps.googleapis.com/maps/api/geocode/json?";
            url += "address=" + destinationStr;
            url += "&key=" + getString(R.string.google_maps_key);

            JsonObjectRequest destRequest = new JsonObjectRequest(Request.Method.GET, url,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray responseArray = response.getJSONArray("results")
                                        .getJSONObject(0).getJSONArray("address_components");
                                String street = responseArray.getJSONObject(0).getString("long_name") + " "
                                        + responseArray.getJSONObject(1).getString("short_name");
                                String city = responseArray.getJSONObject(2).getString("long_name");
                                String state = responseArray.getJSONObject(4).getString("short_name");
                                String zip = responseArray.getJSONObject(6).getString("long_name");

                                String address = street + ", " +city + ", " +state + ", " +zip;

                                JSONObject latLngObject = response.getJSONArray("results")
                                        .getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                                String lat = latLngObject.getString("lat");
                                String lng = latLngObject.getString("lng");

                                destinationParams.put("destAddress", address);
                                destinationParams.put("destCity", city);
                                destinationParams.put("destState", state);
                                destinationParams.put("destZip", zip);
                                destinationParams.put("destLatitude", lat);
                                destinationParams.put("destLongitude", lng);
                                VolleyLog.d("destinationParams = " + destinationParams.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(error.toString());
                            addressFound = false;
                        }
                    }
            );
            queue.add(destRequest);

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!addressFound) {
                Toast.makeText(getBaseContext(), "Either the Origin or Destination address entered " +
                        "was not found. Please try again.", Toast.LENGTH_LONG);
                return;
            } else {
                CreateRideTask task = new CreateRideTask();
                task.execute();
            }
        }
    }

    private class CreateRideTask extends AsyncTask<Void, Void, Void> {

        DateFormat df;
        Date dateTime;
        String date;
        HashMap<String, Object> rideParams;

        @Override
        protected void onPreExecute() {
            df = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
            try {
                dateTime = df.parse(dateTextView.getText().toString() + ", " +
                        timeTextView.getText().toString());
                date = String.valueOf(dateTime.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }


            rideParams = new HashMap<>();
            rideParams.put("driverId", userId);
            rideParams.put("carId", carIds.get((int)carSpinner.getSelectedItemId() - 1));
            rideParams.putAll(originParams);
            rideParams.putAll(destinationParams);
            rideParams.put("departureTime", date);

            rideParams.put("meetingLocation", pickupSpinner.getSelectedItem().toString());
            String[] meetingLatLng = coordinatesArray[(int)pickupSpinner.getSelectedItemId() - 1].split(", ");
            rideParams.put("meetingLatitude", meetingLatLng[0]);
            rideParams.put("meetingLongitude", meetingLatLng[1]);

            rideParams.put("availableSeats", seatsInput.getText().toString());
        }

        @Override
        protected Void doInBackground(Void... params) {
            VolleyLog.d("originParams = " + originParams.toString());
            VolleyLog.d("destinationParams = " + destinationParams.toString());

            VolleyLog.d("rideParams = " + rideParams.toString());


            String url = getString(R.string.url) + "/users/" + userId + "/trips";

            JsonObjectRequest createRideRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(rideParams),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            VolleyLog.d(response.toString());
                            Toast.makeText(getBaseContext(), "Ride Created!",
                                    Toast.LENGTH_LONG).show();

                            finish();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(error.toString());
                            if (error instanceof TimeoutError) {
                                Toast.makeText(getBaseContext(), "The server timed out. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getBaseContext(), "Something went wrong. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            }
                            finish();
                        }
                    }
            )
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    //headers.put("Content-Type", "application/json");
                    headers.put("Access-Control-Allow-Origin", "*");
                    String token = prefs.getString("knight-rider-token", null);
                    headers.put("X-Authorization", "Bearer " + token);
                    headers.put("Cache-Control", "no-cache");
                    return headers;
                }
            };
            queue.add(createRideRequest);
            return null;
        }

    }

    private class DriverLocationListener implements LocationListener {

        public DriverLocationListener(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            userLat = location.getLatitude();
            userLng = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


    public void showDatePickerDialog(View view){
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void showTimePickerDialog(View view) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    android.text.format.DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            String aMpM = (hourOfDay > 11) ? "PM" : "AM";
            int currentHour = (hourOfDay > 11) ? (hourOfDay - 12) : hourOfDay;
            String currentMinute = (minute < 10) ? "0" + String.valueOf(minute) :
                                                        String.valueOf(minute);

            TextView timeTextView = (TextView)getActivity().findViewById(R.id.timeTextView);
            timeTextView.setText(String.valueOf(currentHour) + ":" + currentMinute + " " + aMpM);
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            //Create a new instance of DatePickerDialog and return in
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String date = dateFormat.format(calendar.getTime());

            TextView dateTextView = (TextView)getActivity().findViewById(R.id.dateTextView);
            dateTextView.setText(date);

        }
    }

    private void setDateTimeText() {
        //Set the date and time TextViews to the current time

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        String aMpM = (hour > 11) ? "PM" : "AM";
        int currentHour = (hour > 12) ? (hour - 12) : hour;
        if (hour == 0)
            currentHour = 12;
        String currentMinute = (minute < 10) ? "0" + String.valueOf(minute) :
                                                     String.valueOf(minute);

        TextView timeTextView = (TextView)findViewById(R.id.timeTextView);
        timeTextView.setText(String.valueOf(currentHour) + ":" + currentMinute + " " + aMpM);


        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        c.set(year, month, day);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String date = dateFormat.format(c.getTime());

        TextView dateTextView = (TextView)findViewById(R.id.dateTextView);
        dateTextView.setText(date);
    }


}
