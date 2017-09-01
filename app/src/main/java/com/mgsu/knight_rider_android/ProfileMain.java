package com.mgsu.knight_rider_android;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

public class ProfileMain extends AppCompatActivity {

    String currentView = "";
    FragmentManager fragmentManager = getFragmentManager();

    ConstraintLayout personalTab;
    ConstraintLayout vehicleTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_main);

        //Create the back arrow in the toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        ProfilePersonal profilePersonal = new ProfilePersonal();
        fragmentTransaction.replace(android.R.id.content, profilePersonal);
        fragmentTransaction.commit();


        personalTab = (ConstraintLayout) findViewById(R.id.personalTab);
        vehicleTab = (ConstraintLayout) findViewById(R.id.vehiclesTab);

        currentView = "personal";

        personalTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPersonal();
            }
        });

        vehicleTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVehicles();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Back arrow click handler
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void showPersonal() {
        if (currentView == "personal") {
            return;
        } else {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            ProfilePersonal profilePersonal = new ProfilePersonal();
            fragmentTransaction.replace(android.R.id.content, profilePersonal);
            fragmentTransaction.commit();

//            changeTabColor();

            currentView = "personal";
        }
    }


    public void showVehicles() {
        if (currentView == "vehicles") {
            return;
        } else {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            ProfileVehicle profileVehicle = new ProfileVehicle();
            fragmentTransaction.replace(android.R.id.content, profileVehicle);
            fragmentTransaction.commit();

//            changeTabColor();

            currentView = "vehicles";
        }
    }

    /*
    @TargetApi(21)
    private void changeTabColor() {

        if ()

        if (currentView == "vehicles") {
            personalTab.setBackground(getDrawable(R.drawable.profile_personal_tab));
            vehicleTab.setBackground(getDrawable(R.drawable.profile_vehicles_tab));
        } else if (currentView == "personal") {
            personalTab.setBackground( getDrawable(R.drawable.profile_personal_tab_alt));
            vehicleTab.setBackground(getDrawable(R.drawable.profile_vehicles_tab_alt));
        }

    }*/

}
