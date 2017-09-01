package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CreateCar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_car);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void addCar(View view) {
        TextInputEditText carMake = (TextInputEditText)findViewById(R.id.makeInput);
        TextInputEditText carModel = (TextInputEditText)findViewById(R.id.modelInput);
        TextInputEditText carCapacity = (TextInputEditText)findViewById(R.id.capacityInput);

        Validator validator = new Validator();
        if (!validator.checkCarValues(carMake)
            || !validator.checkCarValues(carModel)
            || !validator.checkCarValues(carCapacity)) {
            return;
        }

        final SharedPreferences prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);
        String userId = prefs.getString("knight-rider-userId", null);


        HashMap<String, String> carValues = new HashMap<String, String>();
        carValues.put("maker", carMake.getText().toString());
        carValues.put("type", carModel.getText().toString());
        carValues.put("capacity", carCapacity.getText().toString());

        String url = getString(R.string.url) + "/users/" + userId + "/cars";

        //Prepare the Request
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest createCarRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(carValues),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Display Response
                        VolleyLog.d(response.toString());
                        Toast.makeText(getBaseContext(), "Car created successfully!",
                                Toast.LENGTH_LONG).show();
                        finish();
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

        //Add to queue
        queue.add(createCarRequest);
    }
}
