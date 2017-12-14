package com.mgsu.knight_rider_android;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileVehicle extends Fragment {

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_profile_vehicle, container, false);

        Button createCarButton = (Button)v.findViewById(R.id.createCarButton);
        createCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("com.mgsu.knight_rider_android.CreateCar"));
            }
        });

        //Inflate the layout for this fragment
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        getAllCars();
    }

    private void getAllCars() {

        final ArrayList<HashMap> vehicles = new ArrayList<>();

        final SharedPreferences prefs = getActivity().getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        String userId = prefs.getString("knight-rider-userId", null);

        String url = getString(R.string.url) + "/users/" + userId + "/cars";
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonArrayRequest getCarsRequest = new JsonArrayRequest(Request.Method.GET, url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        vehicles.clear();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                HashMap<String, String> d = new HashMap<>();
                                JSONObject car = response.getJSONObject(i);
                                d.put("id", car.getString("id"));
                                d.put("maker", car.getString("maker"));
                                d.put("type", car.getString("type"));
                                d.put("capacity", car.getString("capacity"));
                                d.put("licensePlate", car.getString("licensePlate"));
                                d.put("color", car.getString("color"));

                                vehicles.add(d);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        ListAdapter vehicleAdapter = new VehicleAdapter(getActivity(), vehicles);
                        ListView vehicleListView = (ListView) getActivity().findViewById(R.id.vehicleListView);
                        vehicleListView.setAdapter(vehicleAdapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getActivity(), "The server timed out. Please try again.",
                                    Toast.LENGTH_LONG).show();
                            getActivity().finish();
                            startActivity(getActivity().getIntent());
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

        //Add to requestQueue
        queue.add(getCarsRequest);
    }

    private class VehicleAdapter extends ArrayAdapter<ArrayList<HashMap>> {

        ArrayList<HashMap> vehicles = new ArrayList<>();

        public VehicleAdapter(Context context, ArrayList vehicles) {
            super(context, R.layout.item_profile_vehicle, vehicles);
            this.vehicles = vehicles;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater vehicleInflater = LayoutInflater.from(getContext());

            View vehicleView = vehicleInflater.inflate(R.layout.item_profile_vehicle, parent, false);

            final HashMap singleVehicle = vehicles.get(position);

            TextView editLabel = (TextView) vehicleView.findViewById(R.id.vehicleNameEdit);
            editLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("com.mgsu.knight_rider_android.CreateCar");
                    boolean isEditing = true;
                    i.putExtra("isEditing", isEditing);
                    i.putExtra("carId", singleVehicle.get("id").toString());
                    startActivity(i);
                }
            });
            TextView nameLabel = (TextView) vehicleView.findViewById(R.id.vehicleNameLabel);
            TextView makeValue = (TextView) vehicleView.findViewById(R.id.vehicleMakeValue);
            TextView modelValue = (TextView) vehicleView.findViewById(R.id.vehicleModelValue);
            TextView seatsValue = (TextView) vehicleView.findViewById(R.id.vehicleSeatsValue);
            TextView colorValue = (TextView) vehicleView.findViewById(R.id.vehicleColorValue);
            //ImageView licenseImage = (ImageView) vehicleView.findViewById(R.id.vehicleLicenseImage);

            nameLabel.setText(singleVehicle.get("maker").toString());
            makeValue.setText(singleVehicle.get("maker").toString());
            modelValue.setText(singleVehicle.get("type").toString());
            seatsValue.setText(singleVehicle.get("capacity").toString());
            colorValue.setText(singleVehicle.get("color").toString());
//            new DownloadImageTask(licenseImage)
//                    .execute(singleVehicle.get("licensePlate").toString());
            return vehicleView;
        }
    }
}
