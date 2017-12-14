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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserLogin extends AppCompatActivity {

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;

    private SharedPreferences prefs;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Obtain shared prefs
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE
        );
        queue = Volley.newRequestQueue(this);

        /*if (!hasTokenExpired()) {
            login();
        } else {*/
            setContentView(R.layout.activity_user_login);

            usernameInput = (TextInputEditText) findViewById(R.id.usernameInput);
            passwordInput = (TextInputEditText) findViewById(R.id.passwordInput);
//        }

    }

    /*
    private void login() { //Get data from userPrefs
        HashMap <String, String> params = new HashMap<String, String>();
        params.put("username", prefs.getString("knight-rider-username", null));
        params.put("password", prefs.getString("knight-rider-password", null));

//        VolleyLog.d("Username = " + prefs.getString("knight-rider-username", null));
//        VolleyLog.d("Password = " + prefs.getString("knight-rider-password", null));

        String url = getString(R.string.url) + "/auth/login";

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
                        } else {
                            Toast.makeText(getBaseContext(), "Something went wrong. Please try again",
                                    Toast.LENGTH_LONG).show();
                        }

                        setContentView(R.layout.activity_user_login);

                        usernameInput = (TextInputEditText) findViewById(R.id.usernameInput);
                        passwordInput = (TextInputEditText) findViewById(R.id.passwordInput);
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
    }*/

    public void login(View view){ //Get data from the textbox fields.

        Validator validator = new Validator();
        if (!validator.checkUsername(usernameInput)
             || !validator.checkPassword(passwordInput, "login")) {
            return;
        }


        final HashMap<String, String>  params = new HashMap<String, String>();
        params.put("username", usernameInput.getText().toString().toLowerCase());
        params.put("password", passwordInput.getText().toString());

        String url = getString(R.string.url) + "/auth/login";

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
//                            prefs.edit().putString("knight-rider-username", params.get("username")).apply();
//                            prefs.edit().putString("knight-rider-password", encryptPassword(params.get("password"))).apply();
//                            VolleyLog.d("Saved id: " + prefs.getString("knight-rider-userId", null));
//                            setTokenExpirationDate();

                            startActivity(new Intent("com.mgsu.knight_rider_android.MainActivity"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } /*catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }*/
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

    /*
    private String encryptPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(Charset.forName("UTF-8")));
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < encodedHash.length; i++) {
            String hex = Integer.toHexString(0xff & encodedHash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        VolleyLog.d(hexString.toString());
        return hexString.toString();
    }*/

    /*
    private void setTokenExpirationDate() {
        int expirationTime = 7;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, expirationTime);
        prefs.edit().putLong("knight-rider-token-expdate", calendar.getTimeInMillis()).apply();
    }*/

    /*
    private boolean hasTokenExpired() {
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        long expTime = prefs.getLong("knight-rider-token-expdate", 0);

        VolleyLog.d("Current time: " + currentTime);
        VolleyLog.d("Expiration Time: " + expTime);

        if (currentTime > expTime)
            return true;

        return false;
    }*/



    public void register(View view) {
        startActivity(new Intent("com.mgsu.knight_rider_android.UserRegistration"));
    }
}
