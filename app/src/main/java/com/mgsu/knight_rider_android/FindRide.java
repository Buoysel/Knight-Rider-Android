package com.mgsu.knight_rider_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.concurrent.ThreadLocalRandom.*;

public class FindRide extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private RequestQueue queue;
    private SharedPreferences prefs;

    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    public static final int REQUEST_LOCATION_CODE = 99;

    /* Offset Markers in with the same  */
    private ArrayList<double[]> coordinatesList;
    private final double MARKER_OFFSET = 0.000150;


    private double userLat, userLng;

    private HashMap<Marker, HashMap<String, String>> markers;
    private HashMap<String, String> markerValues;

    private String selectedCampus = "";

    private TextInputEditText searchBarInput;
    private String searchInput;
    private String storedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_ride);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

//        searchBarInput = (TextInputEditText)findViewById(R.id.searchBarInput);
//        searchBarInput.setOnEditorActionListener(new TextInputEditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE)
//                {
//                    if (mMap != null)
//                        mMap.clear();
//                    GeocodeAddressTask task = new GeocodeAddressTask();
//                    task.execute();
//                }
//                return false;
//            }
//        });
        setUpSearchBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void clearMarkers() {
        if (mMap != null)
            mMap.clear();
    }

    private void setUpSearchBar() {
        List<String> searchList = new ArrayList<String>();
        String[] campusArray = getResources().getStringArray(R.array.campus_array);

        searchList.add("- Where to? - (All Campuses)");
        for (int i = 0; i < campusArray.length; i++) {
            searchList.add(campusArray[i]);
        }
        final Spinner searchSpinner = (Spinner) findViewById(R.id.searchBarInput);
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, searchList);
        searchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpinner.setAdapter(searchAdapter);

        searchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: //Selected the "Where to?" item
                        selectedCampus = "";
                        findAllRides(selectedCampus);
                        break;
                    default: //Everything else:
                        selectedCampus = searchSpinner.getSelectedItem().toString();
                        findAllRides(selectedCampus);
                        selectedCampus = "";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void findAllRides(final String campus) {
        clearMarkers();
        coordinatesList = new ArrayList<>();

        String url = getString(R.string.url) + "/trips";

        JsonArrayRequest findTripRequest = new JsonArrayRequest(Request.Method.GET, url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        markers = new HashMap<>();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject ride = response.getJSONObject(i);
                                markerValues = new HashMap<>();

                                //Do not show trips that are past their departure time.
                                if (ride.getLong("departureTime") < System.currentTimeMillis())
                                    continue;

                                //Do not show completed trips
                                if (ride.getBoolean("completed"))
                                    continue;

                                //Skip this ride if it's not what the user searched for.
                                if (!campus.equals(""))
                                    if (!campus.contains(ride.getString("destCity")))
                                        continue;

                                VolleyLog.d(response.getJSONObject(i).toString());
                                double latitude;
                                double longitude;
                                try {
                                    latitude = setMarkerOffset(ride.getDouble("meetingLatitude"));
                                    longitude = setMarkerOffset(ride.getDouble("meetingLongitude"));
                                } catch (JSONException e) {
                                    latitude = setMarkerOffset(ride.getDouble("originLatitude"));
                                    longitude = setMarkerOffset(ride.getDouble("originLongitude"));
                                }

                                //Keep track of the coordinates that have already been used.
                                double[] tempCoordinates = new double[] {latitude, longitude};
                                boolean match;
                                do {
                                    match = false;
                                    for (int j = 0; j < coordinatesList.size(); j++) {

                                        if (tempCoordinates[0] == coordinatesList.get(j)[0] &&
                                            tempCoordinates[1] == coordinatesList.get(j)[1]) {
                                            match = true;
                                        }
                                    }

                                    if (match) {
                                        tempCoordinates[0] = setMarkerOffset(tempCoordinates[0]);
                                        tempCoordinates[1] = setMarkerOffset(tempCoordinates[1]);
                                    } else {
                                        coordinatesList.add(tempCoordinates);
                                    }

                                } while (match);

                                //Grab Marker Values for the Info Window
                                markerValues.put("tripId", ride.getString("id"));
                                markerValues.put("originCity", ride.getString("originCity"));
                                markerValues.put("destCity", ride.getString("destCity"));
                                markerValues.put("availableSeats", ride.getString("availableSeats"));
                                markerValues.put("profilePicture", ride.getJSONObject("driver")
                                        .getString("profilePicture"));

                                //Add Marker to the map, and handle it's Click Event
                                LatLng latLng = new LatLng(tempCoordinates[0],
                                                           tempCoordinates[1]);
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

                                Marker thisMarker = mMap.addMarker(markerOptions);
                                markers.put(thisMarker, markerValues);
                            }

                            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker marker) {
                                    marker.showInfoWindow();
                                    return false;
                                }
                            });

                            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                                @Override
                                public View getInfoWindow(Marker marker) {
                                    return null;
                                }

                                @Override
                                public View getInfoContents(Marker marker) {

                                    View v = getLayoutInflater().inflate(R.layout.item_ride_preview, null);

                                    HashMap<String, String> values = new HashMap<>();
                                    values.putAll(markers.get(marker));

                                    TextView originCity = (TextView) v.findViewById(R.id.originCityTextView);
                                    originCity.setText(values.get("originCity"));

                                    TextView destinationCity = (TextView) v.findViewById(R.id.destinationCityTextView);
                                    destinationCity.setText(values.get("destCity"));

                                    TextView seatNumber = (TextView) v.findViewById(R.id.seatNumberTextView);
                                    if (Integer.parseInt(values.get("availableSeats")) < 10)
                                        seatNumber.setText("0" + values.get("availableSeats"));
                                    else
                                        seatNumber.setText(values.get("availableSeats"));

                                    ImageView driverImage = (ImageView) v.findViewById(R.id.driverImage);
                                    new DownloadImageTask(driverImage)
                                            .execute(values.get("profilePicture"));

                                    return v;
                                }
                            });

                            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                                @Override
                                public void onInfoWindowClick(Marker marker) {
                                    HashMap<String, String> values = new HashMap<>();
                                    values.putAll(markers.get(marker));

                                    Intent i = new Intent("com.mgsu.knight_rider_android.RideDetails");
                                    i.putExtra("tripId", Integer.parseInt(values.get("tripId")));
                                    i.putExtra("userLat", userLat);
                                    i.putExtra("userLng", userLng);
                                    startActivity(i);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out. Reloading...",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            startActivity(getIntent());
                        }
                    }
                }
        )
        {
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

        queue.add(findTripRequest);
    }

    private double setMarkerOffset(double coordinate) {
        double offset = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            offset = current().nextDouble(-MARKER_OFFSET, MARKER_OFFSET + 0.000001);
        } else {
            Random rand = new Random();
            offset = -MARKER_OFFSET + (MARKER_OFFSET + (-MARKER_OFFSET + 0.000001)) * rand.nextDouble();
        }
        return coordinate + offset;
    }

/////* This is to be used if the Map uses a text bar, rather than a dropdownlist. *////

//    private class GeocodeAddressTask extends AsyncTask <Void, Void, Void> {
//        @Override
//        protected void onPreExecute() {
//            searchInput = searchBarInput.getText().toString();
//            StringBuilder searchBuilder = new StringBuilder();
//            for (int i = 0; i < searchInput.length(); i++) {
//                if (searchInput.charAt(i) == ' ')
//                    searchBuilder.append('+');
//                else
//                    searchBuilder.append(searchInput.charAt(i));
//            }
//            searchInput = searchBuilder.toString();
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            String url = "https://maps.googleapis.com/maps/api/geocode/json?";
//            url += "address=" + searchInput;
//            url += "&key=" + getString(R.string.google_maps_key);
//
//            JsonObjectRequest searchRequest = new JsonObjectRequest(Request.Method.GET, url,
//                    new Response.Listener<JSONObject>() {
//                        @Override
//                        public void onResponse(JSONObject response) {
//                            try {
//                                storedAddress = response.getJSONArray("results")
//                                        .getJSONObject(0).getString("formatted_address");
//
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    },
//                    new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            VolleyLog.d(error.toString());
//                        }
//                    }
//            );
//            queue.add(searchRequest);
//            try {
//                Thread.sleep(2500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            FindMatchingRidesTask task = new FindMatchingRidesTask();
//            task.execute();
//        }
//    }
//
//    private class FindMatchingRidesTask extends AsyncTask <Void, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            String url = getString(R.string.url) + "/trips";
//
//            JsonArrayRequest findTripRequest = new JsonArrayRequest(Request.Method.GET, url,
//                    new Response.Listener<JSONArray>() {
//                        @Override
//                        public void onResponse(JSONArray response) {
//                            markers = new HashMap<Marker, Integer>();
//                            for (int i = 0; i < response.length(); i++) {
//                                try {
//                                    JSONObject ride = response.getJSONObject(i);
//
//                                    if (ride.getBoolean("completed"))
//                                        continue;
//
//                                    if (!ride.getString("destAddress").contains(storedAddress) &&
//                                        !storedAddress.contains(ride.getString("destAddress")))
//                                        continue;
//
//                                    VolleyLog.d(response.getJSONObject(i).toString());
//                                    double latitude = ride.getDouble("originLatitude");
//                                    double longitude = ride.getDouble("originLongitude");
//                                    int tripId = ride.getInt("id");
//
//                                    LatLng latLng = new LatLng(latitude, longitude);
//                                    MarkerOptions markerOptions = new MarkerOptions();
//                                    markerOptions.position(latLng);
//                                    markerOptions.title(ride.getString("originCity") + " to " +
//                                            ride.getString("destCity"));
//                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
//
//                                    Marker thisMarker = mMap.addMarker(markerOptions);
//
//                                    markers.put(thisMarker, tripId);
//
//                                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                                        @Override
//                                        public boolean onMarkerClick(Marker marker) {
//                                            Intent i = new Intent("com.mgsu.knight_rider_android.RideDetails");
//                                            i.putExtra("tripId", markers.get(marker));
//                                            i.putExtra("userLat", userLat);
//                                            i.putExtra("userLng", userLng);
//                                            startActivity(i);
//                                            return false;
//                                        }
//                                    });
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    },
//                    new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            VolleyLog.d(error.toString());
//                            if (error instanceof TimeoutError) {
//                                Toast.makeText(getBaseContext(), "The server timed out. Reloading...",
//                                        Toast.LENGTH_SHORT).show();
//                                finish();
//                                startActivity(getIntent());
//                            }
//                        }
//                    }
//            )
//            {
//                @Override
//                public Map<String, String> getHeaders() throws AuthFailureError {
//                    HashMap<String, String> headers = new HashMap<String, String>();
//                    headers.put("Content-Type", application/json");
//                    headers.put("Access-Control-Allow-Origin", "*");
//                    String token = prefs.getString("knight-rider-token", null);
//                    headers.put("X-Authorization", "Bearer " + token);
//                    headers.put("Cache-Control", "no-cache");
//                    return headers;
//                }
//            };
//
//            queue.add(findTripRequest);
//
//            return null;
//        }
//    }
//
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED)
                    {
                        if (client == null) {
                            buildGoogleApiClient();
                            mMap.setMyLocationEnabled(true);
                        }
                    }
                }
                else { //Permission denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                ==  PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        if(currentLocationMarker != null)
            currentLocationMarker.remove();

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        userLat = location.getLatitude();
        userLng = location.getLongitude();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("My Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker());

        currentLocationMarker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        VolleyLog.d("Camera Moving");

        mMap.animateCamera(CameraUpdateFactory.zoomBy(5));

        if (client != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        currentLocationMarker.remove();
    }

    @Override
    public void onConnected( Bundle bundle) {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }




}
