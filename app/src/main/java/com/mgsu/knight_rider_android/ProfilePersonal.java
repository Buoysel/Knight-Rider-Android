package com.mgsu.knight_rider_android;


import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfilePersonal extends Fragment {

    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText addressInput;
    private TextInputEditText zipInput;
    private TextInputEditText phoneInput;
    private ImageView profileImage;

    private RequestQueue queue;
    private SharedPreferences prefs;
    private String userId;
    private String username;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile_personal, container, false);

        setProfileLabels(v);

        Button saveButton = (Button)v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUser();
            }
        });

        queue = Volley.newRequestQueue(getActivity());
        prefs = getActivity().getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        userId = prefs.getString("knight-rider-userId", null);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        getUserInfo();
    }

    private void updateUser(){

        Validator validator = new Validator();
        if (!validator.checkName(firstNameInput)
            || !validator.checkName(lastNameInput)
            || !validator.checkAddress(addressInput)
            || !validator.checkZip(zipInput)
            || !validator.checkPhone(phoneInput)) {
            return;
        }

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("firstName", firstNameInput.getText().toString());
        params.put("lastName", lastNameInput.getText().toString());
        params.put("address", addressInput.getText().toString());
        params.put("zip", zipInput.getText().toString());
        params.put("phone", phoneInput.getText().toString());

        String url = getString(R.string.url) + "/users/" + userId;

        //Prepare the Request
        JsonObjectRequest updateRequest = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getActivity(), "Successfully updated user!",
                                Toast.LENGTH_LONG).show();
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

        VolleyLog.d(updateRequest.toString());
        //Add it to the RequestQueue
        queue.add(updateRequest);
    }

    private void getUserInfo() {
        String url = getString(R.string.url) + "/users/" + userId;

        JsonObjectRequest getInfoRequest = new JsonObjectRequest(Request.Method.GET, url, (String)null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.d(response.toString());
                            username = response.getString("username");

                            if (!response.get("firstName").equals(null)) {
                                firstNameInput.setText(response.getString("firstName"));
                            }

                            if (!response.get("lastName").equals(null)) {
                                lastNameInput.setText(response.getString("lastName"));
                            }

                            if (!response.get("address").equals(null)) {
                                addressInput.setText(response.getString("address"));
                            }

                            if (!response.get("zip").equals(null)) {
                                zipInput.setText(response.getString("zip"));
                            }

                            if (!response.get("phone").equals(null)) {
                                phoneInput.setText(response.getString("phone"));
                            }

                            new DownloadImageTask(profileImage)
                                    .execute(response.getString("profilePicture"));
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
                //headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                String token = prefs.getString("knight-rider-token", null);
                headers.put("X-Authorization", "Bearer " + token);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        queue.add(getInfoRequest);
    }

    private void setProfileLabels(View v) {
        firstNameInput = (TextInputEditText)v.findViewById(R.id.firstNameInput);
        lastNameInput = (TextInputEditText)v.findViewById(R.id.lastNameInput);
        addressInput = (TextInputEditText)v.findViewById(R.id.addressInput);
        zipInput = (TextInputEditText)v.findViewById(R.id.zipInput);
        phoneInput = (TextInputEditText)v.findViewById(R.id.phoneInput);
        profileImage = (ImageView)v.findViewById(R.id.userImageView);
    }

}
