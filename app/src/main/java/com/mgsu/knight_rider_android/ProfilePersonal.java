package com.mgsu.knight_rider_android;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.TextInputEditText;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class ProfilePersonal extends Fragment {

    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText addressInput;
    private TextInputEditText zipInput;
    private TextInputEditText phoneInput;
    private ImageView profileImage;
    private RatingBar ratingBar;
    private TextView ratingsTotal;

    private RequestQueue queue;
    private SharedPreferences prefs;
    private String userId;
    private String username;

    static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile_personal, container, false);

        setProfileLabels(v);

        TextView editProfilePicText = (TextView)v.findViewById(R.id.userImageEdit);
        editProfilePicText.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getProfilePic();
            }
        }));

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

    private void getProfilePic() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream selectedImage = getActivity().getContentResolver().openInputStream(data.getData());

                    //Set the profileImage view
                    Bitmap image = BitmapFactory.decodeStream(selectedImage);
                    profileImage.setImageBitmap(image);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 25, baos);

                    //Convert to base64 String and upload
                    byte[] imageBytes = baos.toByteArray();
                    String encodedString = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    uploadNewProfilePic(encodedString);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void uploadNewProfilePic(String encoded) {
        String url = getString(R.string.url) + "/users/profilepicture/" + userId;

        HashMap <String, String> params = new HashMap<>();
        params.put("id", userId);
        params.put("extension", "jpg");
        params.put("profilePicture", encoded);

        JsonObjectRequest uploadPicRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d(response.toString());
                        Toast.makeText(getActivity(), "Successfully updated profile picture!",
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
                        } else {
                            Toast.makeText(getActivity(), "Something went wrong. Please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
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

        queue.add(uploadPicRequest);
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

        HashMap<String, String> params = new HashMap();
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
                    public void onResponse(final JSONObject response) {
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


                            if (numberOfRatings > 0) {
                                averageRating = round(((double) totalRatingScore / numberOfRatings), 1);
                                VolleyLog.d("Average rating: " + averageRating);
                                ratingsTotal.setText("(" + numberOfRatings + ")");
                                ratingBar.setRating((float) averageRating);

                                ratingsTotal.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //Start the activity to show all of this driver's reviews.
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
                        if (error instanceof TimeoutError) {
                            Toast.makeText(getActivity(), "The server timed out. Please try again.",
                                    Toast.LENGTH_LONG).show();
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

        queue.add(getInfoRequest);
    }

    private void setProfileLabels(View v) {
        firstNameInput = (TextInputEditText)v.findViewById(R.id.firstNameInput);
        lastNameInput = (TextInputEditText)v.findViewById(R.id.lastNameInput);
        addressInput = (TextInputEditText)v.findViewById(R.id.addressInput);
        zipInput = (TextInputEditText)v.findViewById(R.id.zipInput);
        phoneInput = (TextInputEditText)v.findViewById(R.id.phoneInput);
        profileImage = (ImageView)v.findViewById(R.id.driverImage);
        ratingBar = (RatingBar)v.findViewById(R.id.ratingBar);
        ratingsTotal = (TextView)v.findViewById(R.id.ratingsTotal);
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
