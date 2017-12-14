package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RateRide extends AppCompatActivity {


    RequestQueue queue;
    SharedPreferences prefs;

    int value;
    String comment;
    String driverId;
    String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_ride);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        driverId = getIntent().getStringExtra("driverId");
        tripId = getIntent().getStringExtra("tripId");

//        Toast.makeText(getBaseContext(), "DriverId is " + driverId +
//                                        "\ntripId is " + tripId, Toast.LENGTH_LONG);

        RatingBar ratingBar = (RatingBar)findViewById(R.id.ratingBar);

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                value = (int) rating;
            }
        });

        getProfilePhoto();
    }

    private void getProfilePhoto() {

        final ImageView driverImage = (ImageView)findViewById(R.id.driverImage);

        String url = getString(R.string.url) + "/users/" + driverId;

        JsonObjectRequest getDriverRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            new DownloadImageTask(driverImage)
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

        queue.add(getDriverRequest);
    }

    public void submitReview(View v) {

        if (value < 1) {
            Toast.makeText(getBaseContext(), "You must leave a rating of 1 or greater",
                    Toast.LENGTH_LONG);
            return;
        }

        TextInputEditText commentInput = (TextInputEditText)findViewById(R.id.commentInput);
        comment = commentInput.getText().toString();

        HashMap<String, String> params = new HashMap<>();
        params.put("tripId", tripId);
        params.put("userId", driverId);
        if (comment.equals(""))
            params.put("comment", "N/A");
        else
            params.put("comment", comment);
        params.put("score", String.valueOf(value));

        String url = getString(R.string.url) + "/reviews/" + tripId + "/" + driverId;

        JsonObjectRequest sendReviewRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        Toast.makeText(getBaseContext(), "Thanks for the review!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        Toast.makeText(getBaseContext(), "Something went wrong. Please try again.",
                                Toast.LENGTH_SHORT).show();
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

        queue.add(sendReviewRequest);
    }

    public void cancel(View v) {
        //Probably go back to the MainActivity, or something....
        finish();
    }
}
