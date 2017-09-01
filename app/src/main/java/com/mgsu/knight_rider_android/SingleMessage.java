package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

public class SingleMessage extends AppCompatActivity {

    private SharedPreferences prefs;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_message);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        getConversation();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getConversation() {
        final ArrayList<HashMap> messages = new ArrayList<>();

        String tripId = getIntent().getStringExtra("tripId");
        String url = getString(R.string.url) + "/trips/" + tripId;

        JsonObjectRequest getConvoRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        messages.clear();
                        try {
                            JSONArray conversation = response.getJSONArray("messages");
                            VolleyLog.d(conversation.toString());
                            for (int i = 0; i < conversation.length(); i++) {
                                JSONObject message = conversation.getJSONObject(i);

                                HashMap<String, String> m = new HashMap<>();
                                m.put("userId", message.getString("userId"));
                                m.put("userName", message.getString("firstName") + " "
                                                + message.getString("lastName"));
                                m.put("message", message.getString("comment"));

                                Long timeEpoch = (Long.parseLong(message.getString("logDate")));
                                SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
                                String time = timeFormat.format(new Date(timeEpoch));
                                m.put("time", time);

                                VolleyLog.d("Single Message: " + m.toString());
                                messages.add(m);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        ListAdapter messageAdapter = new MessageAdapter(getBaseContext(), messages);
                        ListView messagesListView = (ListView) findViewById(R.id.messageListView);
                        messagesListView.setAdapter(messageAdapter);
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
//                headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            String token = prefs.getString("knight-rider-token", null);
            headers.put("X-Authorization", "Bearer " + token);
            headers.put("Cache-Control", "no-cache");
            return headers;
        }
    };

        queue.add(getConvoRequest);
    }

    public void sendMessage(View view) {

        TextInputEditText inputText = (TextInputEditText) findViewById(R.id.inputText);

        if (inputText.getText().toString().equals(""))
            return;

        HashMap<String, String> message = new HashMap<>();

        String tripId = getIntent().getStringExtra("tripId");
        String userId = prefs.getString("knight-rider-userId", null);
        String url = getString(R.string.url) + "/messages/" + tripId + "/" + userId;

        message.put("tripId", tripId);
        message.put("userId", userId);
        message.put("comment", inputText.getText().toString());

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url,new JSONObject(message),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        finish();
                        startActivity(getIntent());
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
//                headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                String token = prefs.getString("knight-rider-token", null);
                headers.put("X-Authorization", "Bearer " + token);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        queue.add(postRequest);
    }

    private class MessageAdapter extends ArrayAdapter<ArrayList<HashMap>> {

        ArrayList<HashMap> messages = new ArrayList<>();

        MessageAdapter(Context context, ArrayList messages) {
            super(context, R.layout.activity_single_message, messages);
            this.messages = messages;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater messageInflater = LayoutInflater.from(getContext());

            View messageView;
            HashMap singleMessage = messages.get(position);
            int userId = Integer.parseInt(prefs.getString("knight-rider-userId", null));

            //In this case, check if userID matches the current user's id.
            if (Integer.parseInt(singleMessage.get("userId").toString()) == userId )
                messageView = messageInflater.inflate(R.layout.message_outgoing_item, parent, false);
            else
                messageView = messageInflater.inflate(R.layout.message_incoming_item, parent, false);


            //Only incoming messages will show the sender's name
            if (Integer.parseInt(String.valueOf(singleMessage.get("userId"))) != userId) {
                TextView incomingName = (TextView) messageView.findViewById(R.id.userNameTextView);
                incomingName.setText(singleMessage.get("userName").toString());
            }

            TextView messageTextView = (TextView) messageView.findViewById(R.id.messageTextView);
            messageTextView.setText(singleMessage.get("message").toString());

            TextView timeTextView = (TextView) messageView.findViewById(R.id.timeTextView);
            timeTextView.setText(singleMessage.get("time").toString());


            return messageView;
        }
    }
}
