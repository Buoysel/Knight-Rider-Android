package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Messages extends AppCompatActivity {

    private SharedPreferences prefs;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        userId = prefs.getString("knight-rider-userId", null);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        getAllMessages();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getAllMessages() {
        final ArrayList<HashMap> messages = new ArrayList<>();

        String url = getString(R.string.url) + "/users/" + userId + "/trips";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest allMessagesRequest = new JsonArrayRequest(Request.Method.GET, url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        messages.clear();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONArray messagesArray = response.getJSONObject(i).getJSONArray("messages");

                                if (messagesArray.length() < 1)
                                    continue;

                                JSONObject ride = response.getJSONObject(i);
                                JSONObject latestMessage = messagesArray.getJSONObject(messagesArray.length() -1);

                                HashMap<String, String> m = new HashMap<>();

                                m.put("tripId", ride.getString("id"));
                                m.put("profileURL", latestMessage.getString("profilePicture"));
                                m.put("rideHeader", ride.getString("originCity") + " to "
                                                    +ride.getString("destCity"));
                                m.put("messageFooter", latestMessage.getString("comment"));

                                Long timeEpoch = (Long.parseLong(latestMessage.getString("logDate")));
                                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                                String time = timeFormat.format(new Date(timeEpoch));
                                m.put("time", time);

                                messages.add(m);

                                VolleyLog.d("Ride object: " + ride.toString());
                                VolleyLog.d("Messages for this ride: " + m.toString());
                            }

                            ListAdapter messageAdapter = new Messages.MessageAdapter(getBaseContext(), messages);
                            ListView messageListView = (ListView) findViewById(R.id.messageListView);
                            messageListView.setAdapter(messageAdapter);
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
                            Toast.makeText(getBaseContext(), "The server timed out. Please try again.",
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

        queue.add(allMessagesRequest);


    }

    private class MessageAdapter extends ArrayAdapter<ArrayList<HashMap>> {

        ArrayList<HashMap> messages = new ArrayList<>();

        MessageAdapter(Context context, ArrayList messages) {
            super(context, R.layout.activity_messages, messages);
            this.messages = messages;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater messageInflater = LayoutInflater.from(getContext());

            View messageView = messageInflater.inflate(R.layout.item_message_preview, parent, false);

            final HashMap singleMessage = messages.get(position);

            ImageView driverImage = (ImageView) messageView.findViewById(R.id.driverImage);
            new DownloadImageTask(driverImage)
                    .execute(singleMessage.get("profileURL").toString());

            TextView rideHeader = (TextView) messageView.findViewById(R.id.rideHeader);
            rideHeader.setText(singleMessage.get("rideHeader").toString());

            TextView messageFooter = (TextView) messageView.findViewById(R.id.messageFooter);
            messageFooter.setText(singleMessage.get("messageFooter").toString());

            TextView time = (TextView) messageView.findViewById(R.id.timeTextView);
            time.setText(singleMessage.get("time").toString());

            messageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("com.mgsu.knight_rider_android.SingleMessage");
                    i.putExtra("tripId", singleMessage.get("tripId").toString());
                    startActivity(i);
                }
            });

            return messageView;
        }
    }
}
