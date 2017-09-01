package com.mgsu.knight_rider_android;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

import java.io.IOException;
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

    /* Input fields */
    private Spinner originSpinner;
    private Spinner destinationSpinner;
    private TextInputEditText pickupInput;
    private Spinner carSpinner;
    private TextView dateTextView;
    private TextView timeTextView;
    private TextInputEditText seatsInput;

    private HashMap<String, Object> originParams = new HashMap<>();
    private HashMap<String, Object> destinationParams = new HashMap<>();
    private HashMap<String, Object> meetingParams;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        originSpinner = (Spinner) findViewById(R.id.originInput);
        destinationSpinner = (Spinner) findViewById(R.id.destinationInput);
        pickupInput = (TextInputEditText)findViewById(R.id.pickupInput);
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
          }

          Validator validator = new Validator();
        if (!validator.checkRideValues(pickupInput)
            || !validator.checkRideValues(seatsInput)) {
            return;
        }

        getAddresses();
        createRide();
    }

    private void setupSpinners() {

        /* Assign data to the Origin, Destination, and Car Spinners */

        List<String> originList = new ArrayList<String>();
        List<String> destinationList = new ArrayList<String>();
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

        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, destinationList);
        destinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(destinationAdapter);

        //Send a request to get the user's list of cars.
        final List<String> carList = new ArrayList<String>();
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

    private void getAddresses() {

        List<Address> originList;
        List<Address> destinationList;
        Geocoder geocoder = new Geocoder(this);

        String originStr;
        String destinationStr;

        String[] addressArray = getResources().getStringArray(R.array.campus_addresses);

        VolleyLog.d("Origin Address: " + addressArray[originSpinner.getSelectedItemPosition() - 1]);
        VolleyLog.d("Destination Address: " + addressArray[destinationSpinner.getSelectedItemPosition() - 1]);

        try {
            originList = geocoder.getFromLocationName(addressArray[originSpinner.getSelectedItemPosition() - 1], 1);
            destinationList = geocoder.getFromLocationName(addressArray[destinationSpinner.getSelectedItemPosition() - 1], 1);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Address originAddress = originList.get(0);
        Address destAddress = destinationList.get(0);

        originParams.put("originAddress",
                addressArray[originSpinner.getSelectedItemPosition() - 1]);
        originParams.put("originCity", originAddress.getLocality());
        originParams.put("originState", originAddress.getAdminArea());
        originParams.put("originZip", originAddress.getPostalCode());
        originParams.put("originLatitude", originAddress.getLatitude());
        originParams.put("originLongitude", originAddress.getLongitude());
        VolleyLog.d("originParams = " + originParams.toString());

        destinationParams.put("destAddress",
                addressArray[destinationSpinner.getSelectedItemPosition() - 1]);
        destinationParams.put("destCity", destAddress.getLocality());
        destinationParams.put("destState", destAddress.getAdminArea());
        destinationParams.put("destZip", destAddress.getPostalCode());
        destinationParams.put("destLatitude", destAddress.getLatitude());
        destinationParams.put("destLongitude", destAddress.getLongitude());
        VolleyLog.d("destinationParams = " + destinationParams.toString());
    }

    private void createRide() {

        String userId = prefs.getString("knight-rider-userId", null);

        DateFormat df = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
        Date dateTime;
        String date = "";
        try {
            dateTime = df.parse(dateTextView.getText().toString() + ", " +
                            timeTextView.getText().toString());
            date = String.valueOf(dateTime.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("driverId", userId);
        params.put("carId", carSpinner.getSelectedItemId());
        params.putAll(originParams);
        params.putAll(destinationParams);
        params.put("departureTime", date);
        params.put("meetingLocation", pickupInput.getText().toString());
        params.put("meetingLatitude", 32.808286);
        params.put("meetingLongitude", -83.734994);
        params.put("availableSeats", seatsInput.getText().toString());

        VolleyLog.d(params.toString());

        String url = getString(R.string.url) + "/users/" + userId + "/trips";

        JsonObjectRequest createRideRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
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
                            finish();
                        } else {
                            Toast.makeText(getBaseContext(), "Something went wrong. Please try again.",
                                    Toast.LENGTH_LONG).show();
                            finish();
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
        queue.add(createRideRequest);
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
