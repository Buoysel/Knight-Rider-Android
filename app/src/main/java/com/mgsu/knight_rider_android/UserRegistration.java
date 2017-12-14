package com.mgsu.knight_rider_android;

import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
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

public class UserRegistration extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    public final int PAGE_COUNT = 1;

    private AccountFragment accountFragment;
//    private ContactFragment contactFragment;
//    private VehicleFragment vehicleFragment;

//    private boolean includeVehicle = false;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        queue = Volley.newRequestQueue(this);

        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setText("Register");
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                    includeVehicle = true;
                register();
            }
        });
    }

    @Override
    public void onBackPressed() {

        //Restore the nextButton's original function if it was changed.
        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setText("Next");
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                nextButtonClick(v);
                VolleyLog.d("Next functionality restored");
            }
        });

        if (mViewPager.getCurrentItem() > 0) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
        else
            super.onBackPressed(); //Exit the activity
    }

    public void openTerms(View view) {
        startActivity(new Intent("com.mgsu.knight_rider_android.TermsAndConditions"));
    }
    /*
    public void nextButtonClick(View view) {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);

        //Change the text and function of the nextButton.
        if (mViewPager.getCurrentItem() == (PAGE_COUNT - 1)) {
            Button nextButton = (Button)findViewById(R.id.nextButton);
            nextButton.setText("Register");
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    includeVehicle = true;
                    register();
                }
            });
            TextView skipVehicleTextView = (TextView) findViewById(R.id.skipVehicleTextView);
            TextView termsTextView = (TextView) findViewById(R.id.termsTextView);
//            skipVehicleTextView.setVisibility(View.VISIBLE);
            termsTextView.setVisibility(View.VISIBLE);
//            skipVehicleTextView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    includeVehicle = false;
//                    register();
//                }
//            });
        }
    }
*/

    private void register() {

        HashMap<String, String> accountValues = accountFragment.getAccountValues();
        if (accountValues == null)
            return;
//        HashMap<String, String> contactValues = contactFragment.getContactValues();

        //Merge the account and contact value HashMaps
        HashMap<String, String> userValues = new HashMap<>();
        userValues.putAll(accountValues);
//        userValues.putAll(contactValues);

        createUser(userValues);

//        if (includeVehicle) {
//
//            HashMap<String, String> carValues = vehicleFragment.getCarValues();
//
//            createCar(carValues/*, accountValues.get("username"), accountValues.get("password")*/);
//        }
    }

    private void createUser(HashMap values) {
        String url = getString(R.string.url) + "/auth/register";

        JsonObjectRequest createUserRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(values),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(getBaseContext(), response.getString("message"),
                                    Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        finish();

                    }
                },
                new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.d(error.toString());
                    NetworkResponse response = error.networkResponse;
                    if (response.statusCode == 409) {
                        Toast.makeText(getBaseContext(), "Email already in use.",
                                Toast.LENGTH_LONG).show();
                    }else if (error instanceof TimeoutError) {
                        Toast.makeText(getBaseContext(), "The server timed out, but account has "
                                                         + "probably been made. Try logging in.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        ){
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

        //Add it to the RequestQueue
        queue.add(createUserRequest);
    }

/*
    private void createCar (HashMap values) {
        String url = "http://168.16.222.103:8080/knightrider/users/" + userId + "/cars";

        //Prepare the Request
        JsonObjectRequest createCarRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(values),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Display response
                        VolleyLog.d(response.toString());
                        VolleyLog.d("Successfully created car!");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(error.toString());
                    }
                }
        );

        //Add it to the RequestQueue
        queue.add(createCarRequest);
    }
    */

    /*
    private String getUserId(String username, String password) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);

        String url = "http://168.16.222.103:8080/knightrider/auth/login";
        String[] id = "";

        //Prepare the request
        JsonObjectRequest getIDRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        VolleyLog.d("Getting id for creating car...");
                        try {
                            id = response.getString("user_id");
                            VolleyLog.d("ID is " + id);
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
        );

        //add it to the RequestQueue
        queue.add(getIDRequest);

        return id;
    }*/

    public static class AccountFragment extends Fragment {
        TextInputEditText firstNameInput;
        TextInputEditText lastNameInput;
        TextInputEditText emailInput;
        TextInputEditText passwordInput;
        TextInputEditText passwordConfirmInput;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_user_registration_account, container, false);

            firstNameInput = (TextInputEditText)v.findViewById(R.id.firstNameInput);
            lastNameInput = (TextInputEditText)v.findViewById(R.id.lastNameInput);
            emailInput = (TextInputEditText)v.findViewById(R.id.emailInput);
            passwordInput = (TextInputEditText)v.findViewById(R.id.passwordInput);
            passwordConfirmInput = (TextInputEditText)v.findViewById(R.id.passwordConfirmInput);

            return v;
        }

        public HashMap<String, String> getAccountValues() {

            Validator validator = new Validator();
            if (!validator.checkName(firstNameInput)
                || !validator.checkName(lastNameInput)
                || !validator.checkEmail(emailInput)
                || !validator.checkPassword(passwordInput, passwordConfirmInput)) {
                return null;
            }

            HashMap<String, String> accountValues = new HashMap<String, String>();

            String firstName = firstNameInput.getText().toString();
            String lastName = lastNameInput.getText().toString();
            String email = emailInput.getText().toString().toLowerCase();
            String password = passwordInput.getText().toString();
            String passwordConfirm = passwordConfirmInput.getText().toString();

            accountValues.put("firstName", firstName);
            accountValues.put("lastName", lastName);
            accountValues.put("email", email);
            accountValues.put("password", password);
            accountValues.put("matchingPassword", passwordConfirm);

            return accountValues;
        }
    }

    /*
    public static class ContactFragment extends Fragment {
        TextInputEditText addressInput;
        TextInputEditText zipInput;
        TextInputEditText phoneInput;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_user_registration_contact, container, false);

            addressInput = (TextInputEditText)v.findViewById(R.id.addressInput);
            zipInput = (TextInputEditText)v.findViewById(R.id.zipInput);
            phoneInput = (TextInputEditText)v.findViewById(R.id.phoneInput);

            return v;
        }

        public HashMap<String, String> getContactValues() {

            HashMap<String, String> contactValues = new HashMap<String, String>();

            String address = addressInput.getText().toString();
            String zipcode = zipInput.getText().toString();
            String phone = phoneInput.getText().toString();

            contactValues.put("address", address);
            contactValues.put("zip", zipcode);
            contactValues.put("phone", phone);

            return contactValues;
        }

    }
*/
    /*
    public static class VehicleFragment extends Fragment {
        TextInputEditText makeInput;
        TextInputEditText modelInput;
        TextInputEditText yearInput;
        TextInputEditText vehicleNameInput;
        TextInputEditText seatsInput;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_user_registration_vehicle, container, false);

            makeInput = (TextInputEditText)v.findViewById(R.id.makeInput);
            modelInput = (TextInputEditText)v.findViewById(R.id.modelInput);
            yearInput = (TextInputEditText)v.findViewById(R.id.yearInput);
            vehicleNameInput = (TextInputEditText)v.findViewById(R.id.vehicleNameInput);
            seatsInput = (TextInputEditText)v.findViewById(R.id.numberOfSeatsInput);

            return v;
        }

        public HashMap<String, String> getCarValues() {

            HashMap<String, String> carValues = new HashMap<String, String>();

            String make = makeInput.getText().toString();
            String model = modelInput.getText().toString();
            String year = yearInput.getText().toString();
            String vehicleName = vehicleNameInput.getText().toString();
            String seats = seatsInput.getText().toString();

            carValues.put("maker", make);
            carValues.put("model", model);
            carValues.put("year", year);
            carValues.put("type", vehicleName);
            carValues.put("capacity", seats); //Convert this to an int

            return carValues;
        }
    }*/

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    accountFragment = new AccountFragment();
                    return accountFragment;
//                case 1:
//                    contactFragment = new ContactFragment();
//                    return contactFragment;
//                case 2:
//                    vehicleFragment = new VehicleFragment();
//                    return vehicleFragment;
                default: return null;
            }
        }

        @Override
        public int getCount() {
            // Show total pages.
            return PAGE_COUNT;
        }

    }
}
