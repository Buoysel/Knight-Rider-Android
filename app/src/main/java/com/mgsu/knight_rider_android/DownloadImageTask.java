package com.mgsu.knight_rider_android;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.android.volley.VolleyLog;

import java.io.InputStream;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    //Sets the user's profile picture

    ImageView bmImage;

    public DownloadImageTask (ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urlDisplay = urls[0];
        Bitmap mIcon = null;
        try {
            InputStream in = new java.net.URL(urlDisplay).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            VolleyLog.d("Error" + e.getMessage());
            e.printStackTrace();
        }
        return mIcon;
    }


    protected void onPostExecute(Bitmap result) {
        if (result != null) {
            removeBackground();
            bmImage.setImageBitmap(result);
        }
    }

    @TargetApi(16)
    private void removeBackground() {
        bmImage.setBackground(null);
    }
}