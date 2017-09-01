package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserLogin extends AppCompatActivity {

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        //Obtain shared prefs
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE
        );


        if (!hasTokenExpired()) {
            login();
        }

        usernameInput = (TextInputEditText) findViewById(R.id.usernameInput);
        passwordInput = (TextInputEditText) findViewById(R.id.passwordInput);
    }

    private void login() { //Get data from userPrefs
        HashMap <String, String> params = new HashMap<String, String>();
        params.put("username", prefs.getString("knight-rider-username", null));
        params.put("password", prefs.getString("knight-rider-password", null));

        VolleyLog.d("Username = " + prefs.getString("knight-rider-username", null));
        VolleyLog.d("Password = " + prefs.getString("knight-rider-password", null));

        String url = getString(R.string.url) + "/auth/login";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        startActivity(new Intent("com.mgsu.knight_rider_android.MainActivity"));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getBaseContext(), "The server timed out. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("X-Requested-With", "XMLHttpRequest");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        queue.add(loginRequest);
    }

    public void login(View view){ //Get data from the textbox fields.

        Validator validator = new Validator();
        if (!validator.checkUsername(usernameInput)
             || !validator.checkPassword(passwordInput, "login")) {
            return;
        }


        final HashMap<String, String>  params = new HashMap<String, String>();
        params.put("username", usernameInput.getText().toString());
        params.put("password", passwordInput.getText().toString());

        String url = getString(R.string.url) + "/auth/login";
        RequestQueue queue = Volley.newRequestQueue(this);

        //prepare the request
        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        String t;
                        String id;
                        try {
                            //Edit prefs
                            t = response.getString("token");
                            id = response.getString("user_id");

                            prefs.edit().putString("knight-rider-token", t).apply();
                            prefs.edit().putString("knight-rider-userId", id).apply();
                            prefs.edit().putString("knight-rider-username", params.get("username")).apply();

                            /* Find alternative to saving the password in plain text like this. */
                            prefs.edit().putString("knight-rider-password", params.get("password")).apply();

                            VolleyLog.d("Saved token: " + prefs.getString("knight-rider-token", null));
                            VolleyLog.d("Saved id: " + prefs.getString("knight-rider-userId", null));

                            setTokenExpirationDate();

                            startActivity(new Intent("com.mgsu.knight_rider_android.MainActivity"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof AuthFailureError)
                            Toast.makeText(getBaseContext(), "Incorrect Username / Password combination",
                                    Toast.LENGTH_LONG).show();
                        else if (error instanceof TimeoutError)
                            Toast.makeText(getBaseContext(), "Server timed out. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        VolleyLog.d(error.toString());

                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("X-Requested-With", "XMLHttpRequest");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };
        //add it to the RequestQueue
        queue.add(loginRequest);
    }

    private boolean hasTokenExpired() {
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        long expTime = prefs.getLong("knight-rider-token-expdate", 0);

        VolleyLog.d("Current time: " + currentTime);
        VolleyLog.d("Expiration Time: " + expTime);

        if (currentTime > expTime)
            return true;

        return false;
    }

    private void setTokenExpirationDate() {
        int expirationTime = 7;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, expirationTime);
        prefs.edit().putLong("knight-rider-token-expdate", calendar.getTimeInMillis()).apply();
    }

    public void register(View view) {
        startActivity(new Intent("com.mgsu.knight_rider_android.UserRegistration"));
    }
}
