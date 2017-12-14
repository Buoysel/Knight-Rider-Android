package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Reviews extends AppCompatActivity {

    private SharedPreferences prefs;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        getAllReviews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getAllReviews() {
        final ArrayList<HashMap> reviews = new ArrayList<>();

        driverId = getIntent().getStringExtra("driverId");
        String url = getString(R.string.url) + "/reviews/drivers/" + driverId;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest allReviewsRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        reviews.clear();
                        try {
                            JSONArray allReviews = response.getJSONArray("reviews");
                            for (int i = allReviews.length() - 1; i >= 0; i--) {
                                JSONObject review = allReviews.getJSONObject(i);

                                HashMap<String, String> r = new HashMap<>();

                                Long timeEpoch = (Long.parseLong(review.getString("logDate")));
                                SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
                                String date = timeFormat.format(new Date(timeEpoch));

                                r.put("date", date);
                                r.put("comment", review.getString("comment"));
                                r.put("score", review.getString("score"));
                                //TripId to show the trips destination?
                                //Should I show the user's name, or keep it anonymous?

                                reviews.add(r);
                            }

                            ListAdapter reviewAdapter = new Reviews.ReviewAdapter(getBaseContext(), reviews);
                            ListView reviewListView = (ListView) findViewById(R.id.reviewsListView);
                            reviewListView.setAdapter(reviewAdapter);
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

        queue.add(allReviewsRequest);
    }

    private class ReviewAdapter extends ArrayAdapter<ArrayList<HashMap>> {

        ArrayList<HashMap> reviews = new ArrayList<>();

        ReviewAdapter(Context context, ArrayList reviews) {
            super(context, R.layout.activity_reviews, reviews);
            this.reviews = reviews;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater reviewInflater = LayoutInflater.from(getContext());

            View reviewView = reviewInflater.inflate(R.layout.item_review, parent, false);

            final HashMap singleReview = reviews.get(position);

            TextView comment = (TextView)reviewView.findViewById(R.id.commentTextView);
            comment.setText(singleReview.get("comment").toString());

            TextView date = (TextView)reviewView.findViewById(R.id.dateTextView);
            date.setText(singleReview.get("date").toString());

            RatingBar rating = (RatingBar) reviewView.findViewById(R.id.ratingBar);
            String scoreStr = singleReview.get("score").toString();
            float scoreFlt = Float.parseFloat(scoreStr);
            rating.setRating(scoreFlt);

            return reviewView;
        }

    }

}
