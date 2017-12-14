package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private SharedPreferences prefs;
    private String userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }

        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        userId = prefs.getString("knight-rider-userId", null);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        setupNavigation();

        getAllRides();
    }

    private void setupNavigation() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);

        getUserInformation(headerView);
    }

    private void getUserInformation(final View v){
        String url = getString(R.string.url) + "/users/" + userId;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest getUserInfoRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ImageView profileImage = (ImageView)v.findViewById(R.id.profileImage);
                        TextView user_nameTextView = (TextView)v.findViewById(R.id.user_nameTextView);
                        TextView emailTextView = (TextView)v.findViewById(R.id.emailTextView);
                        try {
                            new DownloadImageTask(profileImage)
                                    .execute(response.getString("profilePicture"));
                            user_nameTextView.setText(response.getString("firstName") + " " +
                                                     response.getString("lastName"));
                            emailTextView.setText(response.getString("username"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d("Error.Response", error.toString());
                    }
                }
        ){
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

        queue.add(getUserInfoRequest);
    }

    private void getAllRides() {
        final ArrayList<HashMap> rides = new ArrayList<>();

        String url = getString(R.string.url) + "/users/" + userId + "/trips";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest getRidesRequest = new JsonArrayRequest(Request.Method.GET, url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        rides.clear();
                        try {
                            for (int i = response.length(); i > 0; i--) {
                                HashMap<String, String> d = new HashMap<>();
                                JSONObject ride = response.getJSONObject(i-1);
                                d.put("tripId", ride.getString("id"));
                                d.put("originCity", ride.getString("originCity"));
                                d.put("destCity", ride.getString("destCity"));
                                d.put("originAddress", ride.getString("originAddress"));
                                d.put("destAddress", ride.getString("destAddress"));

                                Long timeEpoch = (Long.parseLong(ride.getString("departureTime")));
                                SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
                                String departureTime = timeFormat.format(new Date(timeEpoch));
                                d.put("departureTime", departureTime);

                                if (ride.getString("meetingLocation").equals(null) ||
                                    ride.getString("meetingLocation").equals("null"))

                                    d.put("meetingLocation", ride.getString("originCity"));
                                else
                                    d.put("meetingLocation", ride.getString("meetingLocation"));

                                d.put("availableSeats", ride.getString("availableSeats"));
                                d.put("messageNumber", String.valueOf(ride.getJSONArray("messages").length()));

                                JSONObject driver = ride.getJSONObject("driver");
                                d.put("profileURL", driver.getString("profilePicture"));

                                rides.add(d);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        TextView noRidesHeader = (TextView)findViewById(R.id.noRidesHeader);
                        TextView noRidesFooter = (TextView)findViewById(R.id.noRidesFooter);
                        ListView rideListView = (ListView) findViewById(R.id.rideListView);
                        if (rides.isEmpty()) {
                            noRidesHeader.setVisibility(View.VISIBLE);
                            noRidesFooter.setVisibility(View.VISIBLE);
                            rideListView.setVisibility(View.INVISIBLE);
                        } else {
                            noRidesHeader.setVisibility(View.INVISIBLE);
                            noRidesFooter.setVisibility(View.INVISIBLE);
                            rideListView.setVisibility(View.VISIBLE);

                            //Set the adapter...
                            ListAdapter rideAdapter = new RideAdapter(getBaseContext(), rides);
                            rideListView.setAdapter(rideAdapter);
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d("Error.Response", error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }
        ) {
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

        //Add to queue
        queue.add(getRidesRequest);

    }

    public void startFindRide(View view) {
        startActivity(new Intent("com.mgsu.knight_rider.android.FindRide"));
    }

    public void startCreateRide(View view) {
        startActivity(new Intent("com.mgsu.knight_rider_android.CreateRide"));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_profile:
                startActivity(new Intent("com.mgsu.knight_rider.android.ProfileMain"));
                break;
            case R.id.nav_messages:
                startActivity(new Intent("com.mgsu.knight_rider_android.Messages"));
                break;
            case R.id.nav_terms:
                startActivity(new Intent("com.mgsu.knight_rider_android.TermsAndConditions"));
                break;
            case R.id.nav_logout:
                clearUserData();
                startActivity(new Intent("com.mgsu.knight_rider_android.UserLogin"));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void clearUserData() {
        prefs.edit().putString("knight-rider-token", null).apply();
        prefs.edit().putString("knight-rider-userId", null).apply();
//        prefs.edit().putString("knight-rider-username", null).apply();
//        prefs.edit().putString("knight-rider-password", null).apply();
//        prefs.edit().putLong("knight-rider-token-expdate", 0).apply();
        if (TripLocationService.isActive)
            stopService(new Intent(this, TripLocationService.class));
    }

    private class RideAdapter extends ArrayAdapter<ArrayList<HashMap>> {

        ArrayList<HashMap> rides = new ArrayList<>();

        RideAdapter(Context context, ArrayList rides) {
            super(context, R.layout.item_ridelist, rides);
            this.rides = rides;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater rideInflater = LayoutInflater.from(getContext());

            View rideView = rideInflater.inflate(R.layout.item_ridelist, parent, false);

            HashMap singleRide = rides.get(position);

            TextView originCity = (TextView) rideView.findViewById(R.id.originCityTextView);
            TextView destinationCity = (TextView) rideView.findViewById(R.id.destinationCityTextView);
            ImageView driverImage = (ImageView) rideView.findViewById(R.id.driverImage);
            TextView meetingLocation = (TextView) rideView.findViewById(R.id.meetingLocationTextView);
            TextView originDetail = (TextView) rideView.findViewById(R.id.originDetailTextView);
            TextView originTime = (TextView) rideView.findViewById(R.id.originTimeTextView);
            TextView destinationDetail = (TextView) rideView.findViewById(R.id.destinationDetailTextView);
            TextView seatNumber = (TextView) rideView.findViewById(R.id.seatNumberTextView);
            TextView messageNumber = (TextView) rideView.findViewById(R.id.messageNumberTexView);

            originCity.setText(singleRide.get("originCity").toString());
            destinationCity.setText(singleRide.get("destCity").toString());
            meetingLocation.setText(singleRide.get("meetingLocation").toString());
            originDetail.setText(singleRide.get("originAddress").toString());
            originTime.setText(singleRide.get("departureTime").toString());
            destinationDetail.setText(singleRide.get("destAddress").toString());

            if (Integer.parseInt(singleRide.get("availableSeats").toString()) < 10)
                seatNumber.setText("0" + singleRide.get("availableSeats").toString());
            else
                seatNumber.setText(singleRide.get("availableSeats").toString());

            messageNumber.setText(singleRide.get("messageNumber").toString());

            new DownloadImageTask(driverImage)
                    .execute(singleRide.get("profileURL").toString());

            final int tripId = Integer.parseInt(singleRide.get("tripId").toString());

            View rideDetails = rideView.findViewById(R.id.rideDetails);
            rideDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("com.mgsu.knight_rider_android.RideDetails");
                    i.putExtra("tripId", tripId);
                    startActivity(i);
                }
            });

            View messageButton = rideView.findViewById(R.id.messageButton);
            messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("com.mgsu.knight_rider_android.SingleMessage");
                    i.putExtra("tripId", String.valueOf(tripId));
                    startActivity(i);
                }
            });

            return rideView;
        }
    }
}
