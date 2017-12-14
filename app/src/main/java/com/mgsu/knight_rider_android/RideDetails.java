package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideDetails extends AppCompatActivity implements OnMapReadyCallback{

    private GoogleMap map;
    private RequestQueue queue;
    private SharedPreferences prefs;

    private TextView originCityTextView;
    private TextView originDetailTextView;
    private TextView originTimeTextView;

    private TextView meetingLocationTextView;

    private TextView destinationCityTextView;
    private TextView destinationDetailTextView;

    private TextView vehicleNameTextView;
    private TextView vehicleYearTextView;

    //DriverImage
    private ImageView driverImageView;
    private String driverId;

    //Trip id
    private int tripId;

    private TextView seatNumberTextView;

    private Button rideDetailsButton;

    //Destination latitude and longitude for service
    private String destLat, destLng;

    //Is this ride complete?
    private boolean rideComplete = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        tripId = getIntent().getIntExtra("tripId", -1);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rideMapView);
        mapFragment.getMapAsync(this);

        setupViews();
        getRideDetails();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        map.getUiSettings().setScrollGesturesEnabled(false);

    }

    private  void setupViews() {
        originCityTextView = (TextView)findViewById(R.id.originCityTextView);
        originDetailTextView = (TextView)findViewById(R.id.originDetailTextView);
        originTimeTextView = (TextView) findViewById(R.id.originTimeTextView);
        meetingLocationTextView = (TextView) findViewById(R.id.meetingLocationTextView);
        destinationCityTextView = (TextView) findViewById(R.id.destinationCityTextView);
        destinationDetailTextView = (TextView) findViewById(R.id.destinationDetailTextView);
        vehicleNameTextView = (TextView) findViewById(R.id.vehicleNameTextView);
        vehicleYearTextView = (TextView) findViewById(R.id.vehicleYearTextView);
        seatNumberTextView = (TextView) findViewById(R.id.seatNumberTextView);
        driverImageView = (ImageView) findViewById(R.id.driverImage);
        rideDetailsButton = (Button)findViewById(R.id.rideDetailsButton);
    }



    private void getRideDetails() {

        String url = getString(R.string.url) + "/trips/" + tripId;

        JsonObjectRequest getRideRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        try {
                            JSONObject driver = response.getJSONObject("driver");
                            //Handle Driver Image
                            new DownloadImageTask(driverImageView)
                                    .execute(driver.getString("profilePicture"));
                            driverId = driver.getString("driverId");
                            driverImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent i = new Intent("com.mgsu.knight_rider_android.ProfileAlt");
                                    i.putExtra("userId", driverId);
                                    startActivity(i);
                                }
                            });

                            originCityTextView.setText(response.getString("originCity"));
                            destinationCityTextView.setText(response.getString("destCity"));

                            if (response.getString("meetingLocation").equals(null) ||
                                response.getString("meetingLocation").equals("null")) {
                                meetingLocationTextView.setText(response.getString("originCity"));
                            } else {
                                meetingLocationTextView.setText(response.getString("meetingLocation"));
                            }
                            originDetailTextView.setText(response.getString("originAddress"));

                            Long timeEpoch = (Long.parseLong(response.getString("departureTime")));
                            SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
                            String departureTime = timeFormat.format(new Date(timeEpoch));
                            originTimeTextView.setText(departureTime);

                            destinationDetailTextView.setText(response.getString("destAddress"));

                            JSONObject car = response.getJSONObject("car");
                            vehicleNameTextView.setText(car.getString("maker"));
                            vehicleYearTextView.setText(car.getString("type"));

                            destLat = response.getString("destLatitude");
                            destLng = response.getString("destLongitude");

                            //Show path on the map.
                            HashMap<String, String> coordinates = new HashMap<>();
                            coordinates.put("originLat", response.getString("originLatitude"));
                            coordinates.put("originLong", response.getString("originLongitude"));
                            coordinates.put("destLat", destLat);
                            coordinates.put("destLong", destLng);
                            setMapDirections(coordinates);


                            //Handle Passengers
                            JSONArray passengers = response.getJSONArray("passengers");
                            showPassengers(passengers);

                            int seatsNum = response.getInt("remainingSeats");
                            if (seatsNum < 10)
                                seatNumberTextView.setText("0" + seatsNum);
                            else
                                seatNumberTextView.setText(Integer.toString(seatsNum));

                            rideComplete = response.getBoolean("completed");

                            setUpButton(response, driver, passengers);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                    }
                }
        )
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
//                headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                String token = prefs.getString("knight-rider-token", null);
                headers.put("X-Authorization", "Bearer " + token);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        queue.add(getRideRequest);
    }



    private void setMapDirections(final HashMap<String, String> mapDirections) {

        //Create a two markers, one for the origin and destination, and draw a line between them.

        final LatLng originLatLng = new LatLng(Double.parseDouble(mapDirections.get("originLat")),
                Double.parseDouble(mapDirections.get("originLong")));
        final LatLng destLatLng = new LatLng(Double.parseDouble(mapDirections.get("destLat")),
                Double.parseDouble(mapDirections.get("destLong")));

        String mapsUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + mapDirections.get("originLat") + "," + mapDirections.get("originLong") +
                "&destination=" + mapDirections.get("destLat") + "," + mapDirections.get("destLong") +
                "&key=" + getString(R.string.google_maps_key);

        JsonObjectRequest getDirectionsRequest = new JsonObjectRequest(Request.Method.GET, mapsUrl,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray steps = response.getJSONArray("routes")
                                    .getJSONObject(0).getJSONArray("legs")
                                    .getJSONObject(0).getJSONArray("steps");

                            String[] paths = new String[steps.length()];
                            for (int i = 0; i < paths.length; i++) {
                                paths[i] = String.valueOf(steps.getJSONObject(i)
                                        .getJSONObject("polyline")
                                                .getString("points"));
                            }

                            for (int i = 0; i < paths.length; i++) {
                                PolylineOptions options = new PolylineOptions();
                                options.color(Color.BLUE);
                                options.width(10);
                                options.addAll(PolyUtil.decode(paths[i]));
                                map.addPolyline(options);
                            }

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(originLatLng);
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_VIOLET
                            ));
                            map.addMarker(markerOptions);
                            markerOptions.position(destLatLng);
                            map.addMarker(markerOptions);

                            //Move the camera to encompass both points.
                            LatLngBounds bounds;
                            double north, south, east, west;
                            if (originLatLng.latitude < destLatLng.latitude) {
                                north = destLatLng.latitude;
                                south = originLatLng.latitude;
                            } else {
                                north = originLatLng.latitude;
                                south = destLatLng.latitude;
                            }

                            if (originLatLng.longitude < destLatLng.longitude) {
                                east = destLatLng.longitude;
                                west = originLatLng.longitude;
                            } else {
                                east = originLatLng.longitude;
                                west = destLatLng.longitude;
                            }
                            bounds = new LatLngBounds(new LatLng(south, west),
                                                      new LatLng(north, east));
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                    }
                }
        );
        queue.add(getDirectionsRequest);

    }

    private void showPassengers(JSONArray passArray) throws JSONException {

        ArrayList<HashMap> passengers = new ArrayList<>();

        for (int i = 0; i < passArray.length(); i++) {
            HashMap<String, String> d = new HashMap<>();
            JSONObject passenger = passArray.getJSONObject(i);

            d.put("id", passenger.getString("id"));
            d.put("profilePictureURL", passenger.getString("profilePicture"));

            VolleyLog.d("Profile Picture URL: " + d.get("profilePictureURL"));

            passengers.add(d);
        }

        RecyclerView passengersRecycler = (RecyclerView) findViewById(R.id.passengersRecycler);
        passengersRecycler.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        passengersRecycler.setAdapter(new PassengerAdapter(passengers));
        passengersRecycler.setLayoutManager(layoutManager);

    }

    private void setUpButton(JSONObject r, JSONObject d, JSONArray p) throws JSONException {
        int userId = Integer.parseInt(prefs.getString("knight-rider-userId", null));

        boolean isDriver = false;
        boolean isPassenger = false;

        if (d.getInt("driverId") == userId) {
            isDriver = true;
            VolleyLog.d("This user is the driver.");
        } else {
            for (int i = 0; i < p.length(); i++) {
                if (p.getJSONObject(i).getInt("id") == userId) {
                    isPassenger = true;
                    VolleyLog.d("This user is a passenger");
                    break;
                }
            }
        }

        if (isDriver) {

            if (rideComplete) {
                rideDetailsButton.setText("Ride completed.");
                rideDetailsButton.setAlpha(0.5f);
                rideDetailsButton.setEnabled(false);
                return;
            }

            if (TripLocationService.isActive) {
                rideDetailsButton.setText("Cancel Ride");
                rideDetailsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        VolleyLog.d("Canceling Ride...");
                        cancelRide();
                    }
                });
            } else {
                rideDetailsButton.setText("Start Ride");
                rideDetailsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        VolleyLog.d("Starting Ride...");
                        startLocationService();
                    }
                });
            }

        } else if (isPassenger) {
            rideDetailsButton.setText("Leave Ride");
            rideDetailsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VolleyLog.d("Leaving Ride...");
                    int method = Request.Method.DELETE;
                    joinLeaveRide(method);
                }
            });
        } else if (!isDriver && !isPassenger) {

            if (TripLocationService.isActive) {
                rideDetailsButton.setText("Already in a ride");
                rideDetailsButton.setAlpha(0.5f);
                rideDetailsButton.setEnabled(false);
            }
            else if (r.getInt("remainingSeats") < 1) {
                rideDetailsButton.setText("Ride is full");
                rideDetailsButton.setAlpha(0.5f);
                rideDetailsButton.setEnabled(false);
            } else {
                rideDetailsButton.setText("Join Ride");
                rideDetailsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        VolleyLog.d("Joining Ride...");
                        int method = Request.Method.POST;
                        joinLeaveRide(method);
                    }
                });
            }
        }
    }

    private void joinLeaveRide(final int method) {
        String userId = prefs.getString("knight-rider-userId", null);

        HashMap<String, String> params = new HashMap<>();

        if (method == Request.Method.POST) {
            double userLat = getIntent().getDoubleExtra("userLat", 0);
            double userLng = getIntent().getDoubleExtra("userLng", 0);

            params.put("userId", userId);
            params.put("tripId", Integer.toString(tripId));
            params.put("latitude", String.valueOf(userLat));
            params.put("longitude", String.valueOf(userLng));

            try {
                List<Address> userAddress = new Geocoder(this).getFromLocation(userLat, userLng, 1);
                params.put("address", userAddress.get(0).getThoroughfare() + " "
                                    + userAddress.get(0).getLocality() + ", "
                                    + userAddress.get(0).getAdminArea() + " "
                                    + userAddress.get(0).getPostalCode());
                VolleyLog.d("User Address: " + params.get("address"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        String url = getString(R.string.url) + "/passengers/" + tripId + "/" + userId;

        JsonObjectRequest joinRideRequest = new JsonObjectRequest(method, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());

                        if (method == Request.Method.POST) {
                            Toast.makeText(getBaseContext(), "Successfully joined this trip!",
                                    Toast.LENGTH_SHORT).show();

                           startLocationService();

                        } else if (method == Request.Method.DELETE) {
                            Toast.makeText(getBaseContext(), "You have left this trip",
                                    Toast.LENGTH_SHORT).show();
                            stopService(new Intent(getBaseContext(), TripLocationService.class));
                        }

                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out. Reloading...",
                                    Toast.LENGTH_LONG).show();
                            finish();
                            startActivity(getIntent());
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

        queue.add(joinRideRequest);
    }

    private void cancelRide() {
        String url = getString(R.string.url) + "/trips/" + tripId;

        JsonObjectRequest deleteRequest = new JsonObjectRequest(Request.Method.DELETE, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        Toast.makeText(getBaseContext(), "Ride has been deleted.", Toast.LENGTH_LONG).show();
                        stopService(new Intent(getBaseContext(), TripLocationService.class));
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out. Reloading...",
                                    Toast.LENGTH_LONG).show();
                            finish();
                            startActivity(getIntent());
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

        queue.add(deleteRequest);
    }

    private void startLocationService() {
        Intent i = new Intent(getBaseContext(), TripLocationService.class);
        i.putExtra("destName", destinationCityTextView.getText().toString());
        i.putExtra("destLat", destLat);
        i.putExtra("destLng", destLng);
        i.putExtra("tripId", tripId);
        i.putExtra("driverId",driverId);
        getBaseContext().startService(i);
    }


    //---Necessary Code in order to make the list of passenger profile images scroll horizontally---
    private class PassengerAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private ArrayList<HashMap> passengers = new ArrayList<>();
        private View passengerItem;

        public PassengerAdapter(ArrayList passengers) {
            this.passengers = passengers;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View passengerView = LayoutInflater.from(getBaseContext())
                    .inflate(R.layout.item_passenger, parent, false);
            passengerItem = passengerView;
            MyViewHolder holder = new MyViewHolder(passengerView);
            return holder;
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            VolleyLog.d(String.valueOf(position));
            HashMap passenger = passengers.get(position);

            ImageView profilePic = (ImageView) passengerItem.findViewById(R.id.profilePic);
            new DownloadImageTask(profilePic).execute(passenger.get("profilePictureURL").toString());
            final String userId = passenger.get("id").toString();

            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("com.mgsu.knight_rider_android.ProfileAlt");
                    i.putExtra("userId", userId);
                    startActivity(i);
                }
            });
        }

        @Override
        public int getItemCount() {
            return passengers.size();
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {

        public MyViewHolder(View v) {
            super(v);
        }
    }


}
