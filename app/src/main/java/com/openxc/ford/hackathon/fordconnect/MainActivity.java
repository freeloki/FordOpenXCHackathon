package com.openxc.ford.hackathon.fordconnect;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.Image;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.github.anastr.speedviewlib.ImageSpeedometer;
import com.github.anastr.speedviewlib.SpeedView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.openxc.VehicleManager;
import com.openxc.ford.hackathon.fordconnect.handler.IotIgniteHandler;
import com.openxc.ford.hackathon.fordconnect.handler.LocationHandler;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.Latitude;
import com.openxc.measurements.Longitude;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.VehicleSpeed;

import org.json.JSONException;
import org.json.JSONObject;

import at.markushi.ui.CircleButton;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private VehicleManager mVehicleManager;

    private IotIgniteHandler mIgniteHandler;

    private double latitude;
    private double longitude;


    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            mVehicleManager.addListener(EngineSpeed.class, mSpeedListener);
            mVehicleManager.addListener(VehicleSpeed.class, mVehicleSpeedListener);
            //mVehicleManager.addListener(Longitude.class,longitudeListener);
            //mVehicleManager.addListener(Latitude.class,latitudeListener);

            setConnectionState(openXcState, true);


        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
            setConnectionState(openXcState, false);
        }
    };

    private EngineSpeed.Listener mSpeedListener = new EngineSpeed.Listener() {
        @Override
        public void receive(Measurement measurement) {

            final EngineSpeed engineSpeed = (EngineSpeed) measurement;
            final float rpm = (float) engineSpeed.getValue().doubleValue();
            Log.i(TAG, "Engine Speed Simulation: " + rpm);
            mIgniteHandler.setRpmData(rpm);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRpmView.speedTo(rpm, 500);
                }
            });
        }
    };

    private VehicleSpeed.Listener mVehicleSpeedListener = new VehicleSpeed.Listener() {
        @Override
        public void receive(Measurement measurement) {
            final VehicleSpeed speed = (VehicleSpeed) measurement;
            final float speedy = (float) speed.getValue().doubleValue();
            Log.i(TAG, "Vehicle Speed Simulation: " + speedy);
            mIgniteHandler.setSpeedData(speedy);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpeedView.speedTo(speedy, 500);
                }
            });
        }
    };

    /**
     * Get location from phone itself
     **/

/*    private Latitude.Listener latitudeListener = new Latitude.Listener() {
        @Override
        public void receive(Measurement measurement) {
            Latitude lat = (Latitude) measurement;
            Log.i(TAG, "Latitude " + lat.getValue().doubleValue());

        }
    };

    private Longitude.Listener longitudeListener = new Longitude.Listener() {
        @Override
        public void receive(Measurement measurement) {
            Longitude longi = (Longitude) measurement;
            Log.i(TAG, "Longitude " + longi.getValue().doubleValue());

        }
    };*/

    private SpeedView mSpeedView;
    private SpeedView mRpmView;
    private SupportMapFragment mapFragment;
    private BroadcastReceiver mapUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String location = intent.getStringExtra("location");


            try {
                JSONObject locationJson = new JSONObject(location);
                latitude = locationJson.getDouble("latitude");

                longitude = locationJson.getDouble("longitude");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mapFragment.getMapAsync(MainActivity.this);
        }
    };

    private BroadcastReceiver igniteStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean state = intent.getBooleanExtra("state", false);

            setConnectionState(igniteState, state);
        }
    };

    private GoogleMap map;
    private CircleButton mPanicButton;
    private ImageView openXcState, igniteState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSpeedView = findViewById(R.id.speedView);
        mSpeedView.setMinMaxSpeed(0, 240);
        //  mSpeedView.
        mRpmView = findViewById(R.id.rpmView);
        mRpmView.setMinMaxSpeed(0, 6000);


        mIgniteHandler = IotIgniteHandler.getInstance(getApplicationContext());
        mIgniteHandler.start();

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // mapFragment.getMapAsync(this);
        //mapFragment.

        mPanicButton = findViewById(R.id.panicButton);
        mPanicButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //return false;
                //  Log.i(TAG, "LOOONG LONG CLICKED");
                if (mIgniteHandler != null) {
                    mIgniteHandler.sendEmergencyMessage();
                }
                return true;
            }
        });


        openXcState = findViewById(R.id.openXcImage);
        igniteState = findViewById(R.id.igniteState);
        LocalBroadcastManager.getInstance(this).registerReceiver(mapUpdateReceiver, new IntentFilter("UPDATE_LOC"));
        LocalBroadcastManager.getInstance(this).registerReceiver(igniteStateReceiver, new IntentFilter("IGNITE_STATE"));


    }

    @Override
    protected void onResume() {
        super.onResume();

        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if (mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (map != null) {
            map.clear();
        }

        if (latitude > 0 && longitude > 0) {

            map = googleMap;
            LatLng area = new LatLng(latitude, longitude);

            map.addMarker(new MarkerOptions().position(area)
                    .title("Araç Konumu"));
            map.setMaxZoomPreference(15f);
            map.moveCamera(CameraUpdateFactory.newLatLng(area));
            map.moveCamera(CameraUpdateFactory.zoomBy(1));
        }

    }

    private void setConnectionState(final ImageView view, final boolean state) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    //    view.set
                    view.setImageResource(R.drawable.connected);
                } else {
                    view.setImageResource(R.drawable.disconnected);
                }
            }
        });

    }
}
