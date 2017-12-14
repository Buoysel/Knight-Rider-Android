package com.mgsu.knight_rider_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class CreateCar extends AppCompatActivity {

    private RequestQueue queue;
    private SharedPreferences prefs;

    private String userId;

    private TextInputEditText carMake;
    private TextInputEditText carModel;
    private TextInputEditText carCapacity;
    private TextInputEditText carColor;

    private Button deleteCarButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_car);

        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences(
                "com.mgsu.knight_rider_android", Context.MODE_PRIVATE);

        userId = prefs.getString("knight-rider-userId", null);

        carMake = (TextInputEditText)findViewById(R.id.makeInput);
        carModel = (TextInputEditText)findViewById(R.id.modelInput);
        carCapacity = (TextInputEditText)findViewById(R.id.capacityInput);
        carColor = (TextInputEditText)findViewById(R.id.colorInput);

        deleteCarButton = (Button) findViewById(R.id.deleteCarButton);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (getIntent().getBooleanExtra("isEditing", false)) {
            createEditCarActivity();
        } else {
            deleteCarButton.setVisibility(View.GONE);
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

    private void createEditCarActivity() {
        setTitle("Edit Car");

        Button createCarButton = (Button)findViewById(R.id.createCarButton);
        createCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCar();
            }
        });
        createCarButton.setText("Save Changes");
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        createParams.setMargins(0, 0, 16, 0);
        createParams.weight = 1;
        createCarButton.setLayoutParams(createParams);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.setMargins(16, 0, 0, 0);
        deleteParams.weight = 1;
        deleteCarButton.setLayoutParams(deleteParams);


        getCarValues();
    }

    private void getCarValues() {
        String url = getString(R.string.url) + "/cars/" + getIntent().getStringExtra("carId");

        JsonObjectRequest getCarRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            carMake.setText(response.getString("maker"));
                            carModel.setText(response.getString("type"));
                            carCapacity.setText(response.getString("capacity"));
                            carColor.setText(response.getString("color"));
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
        queue.add(getCarRequest);
    }

    public void addCar(View view) {
        Validator validator = new Validator();
        if (!validator.checkCarValues(carMake)
            || !validator.checkCarValues(carModel)
            || !validator.checkCarValues(carCapacity)
            || !validator.checkCarValues(carColor)) {
            return;
        }

        HashMap<String, String> carValues = new HashMap<>();
        carValues.put("maker", carMake.getText().toString());
        carValues.put("type", carModel.getText().toString());
        carValues.put("capacity", carCapacity.getText().toString());
        carValues.put("color", carColor.getText().toString());

        String url = getString(R.string.url) + "/users/" + userId + "/cars";

        //Prepare the Request
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
                        } else {
                            Toast.makeText(getBaseContext(), "Something went wrong. Please try again later.",
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

    private void updateCar() {
        Validator validator = new Validator();
        if (!validator.checkCarValues(carMake)
                || !validator.checkCarValues(carModel)
                || !validator.checkCarValues(carCapacity)
                || !validator.checkCarValues(carColor)) {
            return;
        }

        String url = getString(R.string.url) + "/users/" + userId + "/cars/" + getIntent().getStringExtra("carId");

        HashMap<String, String> carValues = new HashMap<>();
        carValues.put("maker", carMake.getText().toString());
        carValues.put("type", carModel.getText().toString());
        carValues.put("capacity", carCapacity.getText().toString());
        carValues.put("color", carColor.getText().toString());

        JsonObjectRequest updateCarRequest = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(carValues),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getBaseContext(), "Car updated successfully!",
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
                        } else {
                            Toast.makeText(getBaseContext(), "Something went wrong. Please try again later.",
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

        queue.add(updateCarRequest);
    }

    public void deleteCar(View view) {
        String url = getString(R.string.url) + "/cars/" + getIntent().getStringExtra("carId");

        JsonObjectRequest deleteCarRequest = new JsonObjectRequest(Request.Method.DELETE, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getBaseContext(), "Car deleted successfully!",
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
                        } else {
                            Toast.makeText(getBaseContext(), "Something went wrong. Please try again later.",
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

        queue.add(deleteCarRequest);
    }
}
