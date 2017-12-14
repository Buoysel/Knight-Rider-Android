package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class ProfileAlt extends AppCompatActivity {

    private ImageView userImageView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private RatingBar ratingBar;
    private TextView ratingsTotal;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_alt);


        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupViews();
        getUserInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getUserInfo() {
        RequestQueue queue = Volley.newRequestQueue(this);
        final SharedPreferences prefs = this.getSharedPreferences(
                 "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        userId = getIntent().getStringExtra("userId");
        String url = getString(R.string.url) + "/users/" + userId;

        JsonObjectRequest getUserRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.d(response.toString());
                            setTitle(response.getString("firstName") + " "
                                    + response.getString("lastName"));
                            emailTextView.setText(response.getString("username"));
                            phoneTextView.setText(response.getString("phone"));
                            new DownloadImageTask(userImageView)
                                    .execute(response.getString("profilePicture"));

                            //Calculate the average review score, only if the user has been a driver.
                            int totalRatingScore = 0;
                            int numberOfRatings = 0;
                            double averageRating;
                            JSONArray trips = response.getJSONArray("trips");
                            for (int i = 0; i < trips.length(); i++) {
                                JSONObject trip = trips.getJSONObject(i);
                                if (trip.getString("driverId").equals(userId)) {
                                    JSONArray reviews = trips.getJSONObject(i).getJSONArray("reviews");
                                    for (int j = 0; j < reviews.length(); j++) {
                                        JSONObject review = reviews.getJSONObject(j);
                                        totalRatingScore += Integer.parseInt(review.getString("score"));
                                        numberOfRatings++;
                                    }

                                } else {
                                    continue;
                                }
                            }

                            VolleyLog.d("Total review score: " + totalRatingScore + ". " +
                                    "Number of reviews: " + numberOfRatings);

                            if (numberOfRatings > 0) {
                                averageRating = round(((double) totalRatingScore / numberOfRatings), 1);
                                VolleyLog.d("Average rating: " + averageRating);
                                ratingsTotal.setText("(" + numberOfRatings + ")");
                                ratingBar.setRating((float) averageRating);

                                ratingsTotal.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent i = new Intent("com.mgsu.knight_rider_android.Reviews");
                                        i.putExtra("driverId", userId);
                                        startActivity(i);
                                    }
                                });
                            }

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

        queue.add(getUserRequest);
    }

    private void setupViews() {
        userImageView = (ImageView)findViewById(R.id.driverImage);
        emailTextView = (TextView)findViewById(R.id.emailTextView);
        phoneTextView = (TextView) findViewById(R.id.phoneTextView);
        ratingBar = (RatingBar)findViewById(R.id.ratingBar);
        ratingsTotal = (TextView) findViewById(R.id.ratingsTotal);
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
