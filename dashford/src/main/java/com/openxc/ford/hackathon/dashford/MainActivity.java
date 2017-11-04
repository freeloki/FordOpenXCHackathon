package com.openxc.ford.hackathon.dashford;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    //  private LinearLayout mainLayout;
    private WebView myWebView;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.loadUrl("http://www.codegeni.us/dashford/");
        myWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void emergencyCall(String location) {

                double lat = 0;
                double lon = 0;
                try {
                    JSONObject locJson = new JSONObject(location);

                    lat = locJson.getDouble("latitude");
                    lon = locJson.getDouble("longitude");
                    Log.i(TAG, "LAT : " + lat);
                    Log.i(TAG, "LON : " + lon);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Location: " + location);

                final String query = "google.navigation:q=" + lat + "," + lon;
                Log.i(TAG, "QUERY:" + query);


                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Emergency Call Detected")
                        .setMessage("Start navigation?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                Uri gmmIntentUri = Uri.parse(query);
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                } else {
                                    Log.e(TAG, "Package is not available.");
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();


            }

            @JavascriptInterface
            public void reload() {

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (myWebView != null) {
                            myWebView.reload();
                        }
                    }
                });


            }
        }, "Android");


//40.809278,29.430055
        //    geo:latitude,longitude?q=query


        //  Uri gmmIntentUri = Uri.parse("google.navigation:d=40.809278,29.430055");

    }

    @Override
    protected void onResume() {
        super.onResume();

/*        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Uri gmmIntentUri = Uri.parse("google.navigation:q=40.809278,29.430055");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Log.e(TAG, "Package is not available.");
                }

            }
        }, 5000L);*/
    }
}
