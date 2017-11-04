package com.openxc.ford.hackathon.fordconnect.handler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;

/**
 * Created by yavuz.erzurumlu on 4/10/17.
 */


public class IotIgniteHandler implements ConnectionCallback, NodeListener {

    private static final String TAG = IotIgniteHandler.class.getSimpleName();

    private static final String ON_DESTROY_MSG = "Application Destroyed";

    // Static singleton instance
    private static IotIgniteHandler INSTANCE = null;
    private static final long IGNITE_RECONNECT_INTERVAL = 10000L;
    private long SPEED_THING_CONFIGURATION_INTERVAL = -1L;
    private long RPM_THING_CONFIGURATION_INTERVAL = -1L;
    private long LOCATION_THING_CONFIGURATION_INTERVAL = -1L;
    private long EMERGENCY_THING_CONFIGURATION_INTERVAL = -1L;


    private String lastLocationData = "";
    private float lastRPMData = 0F;
    private float lastSpeedData = 0F;

    private static final String NODE_ID = "Ford";
    private static final String SPEED_THING_ID = "SpeedThing";
    private static final String RPM_THING_ID = "RPMThing";
    private static final String LOCATION_THING_ID = "LocationThing";
    private static final String EMERGENCY_THING_ID = "EmergencyCallThing";


    private IotIgniteManager mIotIgniteManager;
    private boolean igniteConnected = false;
    private Context appContext;
    private Handler igniteWatchdog = new Handler();

    private Handler dataSender = new Handler();

    private LocationHandler mLocationHandler;


    private ThingListener speedThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {

            /**
             * Thing configuration messages will be handled here.
             * For example data reading frequency or custom configuration may be in the incoming thing object.
             */

            speedThing = thing;

            Log.i(TAG, "Config received for thing[" + thing.getThingID() + "] devices : " + thing.getThingConfiguration().getDataReadingFrequency());
            if (thing.getThingConfiguration() != null && thing.getThingConfiguration().getDataReadingFrequency() >= 0) {

                Log.i(TAG, "Config :" + thing.getThingConfiguration().getDataReadingFrequency());
                SPEED_THING_CONFIGURATION_INTERVAL = thing.getThingConfiguration().getDataReadingFrequency();

                if (SPEED_THING_CONFIGURATION_INTERVAL > 0L) {
                    dataSender.removeCallbacks(speedDataSenderRunnable);
                    dataSender.post(speedDataSenderRunnable);
                }
            }

        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }

        @Override
        public void onThingUnregistered(String s, String s1) {

        }
    };

    private ThingListener rpmThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {

            /**
             * Thing configuration messages will be handled here.
             * For example data reading frequency or custom configuration may be in the incoming thing object.
             */

            rpmThing = thing;

            Log.i(TAG, "Config received for thing[" + thing.getThingID() + "] devices : " + thing.getThingConfiguration().getDataReadingFrequency());
            if (thing.getThingConfiguration() != null && thing.getThingConfiguration().getDataReadingFrequency() >= 0) {

                Log.i(TAG, "Config :" + thing.getThingConfiguration().getDataReadingFrequency());
                RPM_THING_CONFIGURATION_INTERVAL = thing.getThingConfiguration().getDataReadingFrequency();

                if (RPM_THING_CONFIGURATION_INTERVAL > 0L) {
                    dataSender.removeCallbacks(rpmDataSenderRunnable);
                    dataSender.post(rpmDataSenderRunnable);
                }
            }

        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }

        @Override
        public void onThingUnregistered(String s, String s1) {

        }
    };

    private ThingListener locationThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            locationThing = thing;

            Log.i(TAG, "Config received for thing[" + thing.getThingID() + "] devices : " + thing.getThingConfiguration().getDataReadingFrequency());
            if (thing.getThingConfiguration() != null && thing.getThingConfiguration().getDataReadingFrequency() >= 0) {

                Log.i(TAG, "Config :" + thing.getThingConfiguration().getDataReadingFrequency());
                LOCATION_THING_CONFIGURATION_INTERVAL = thing.getThingConfiguration().getDataReadingFrequency();

                if (LOCATION_THING_CONFIGURATION_INTERVAL > 0L) {
                    dataSender.removeCallbacks(locationDataSenderRunnable);
                    dataSender.post(locationDataSenderRunnable);
                }
            }
        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }

        @Override
        public void onThingUnregistered(String s, String s1) {

        }
    };

    private ThingListener emergencyThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG, "Config received for thing[" + thing.getThingID() + "] devices : " + thing.getThingConfiguration().getDataReadingFrequency());
            if (thing.getThingConfiguration() != null && thing.getThingConfiguration().getDataReadingFrequency() >= 0) {

                Log.i(TAG, "Config :" + thing.getThingConfiguration().getDataReadingFrequency());
                EMERGENCY_THING_CONFIGURATION_INTERVAL = thing.getThingConfiguration().getDataReadingFrequency();

                //Don't need to setup runnable. Config will be send when arrive.
            }
        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }

        @Override
        public void onThingUnregistered(String s, String s1) {

        }
    };


    private Node baseNode;
    private Thing speedThing, rpmThing, locationThing, emergencyThing;


    private ThingType emergencyThingType = new ThingType(
            /** Define Type of your Thing */
            "FordEmergencyThing",
            /** Set your things vendor. It's useful if you are using real sensors
             * This is important for seperating same sensor which different vendors.
             **/
            "Ford",
            /** Set your things data type.
             * IoT-Ignite works with data which type you have selected.
             */
            ThingDataType.INTEGER
    );

    private ThingType speedThingType = new ThingType(
            /** Define Type of your Thing */
            "FordSpeedThing",
            /** Set your things vendor. It's useful if you are using real sensors
             * This is important for seperating same sensor which different vendors.
             **/
            "Ford",
            /** Set your things data type.
             * IoT-Ignite works with data which type you have selected.
             */
            ThingDataType.FLOAT
    );

    private ThingType rpmThingType = new ThingType(
            /** Define Type of your Thing */
            "FordRpmThing",
            /** Set your things vendor. It's useful if you are using real sensors
             * This is important for seperating same sensor which different vendors.
             **/
            "Ford",
            /** Set your things data type.
             * IoT-Ignite works with data which type you have selected.
             */
            ThingDataType.FLOAT
    );

    private ThingType locationThingType = new ThingType(
            /** Define Type of your Thing */
            "FordLocationThing",
            /** Set your things vendor. It's useful if you are using real sensors
             * This is important for seperating same sensor which different vendors.
             **/
            "Ford",
            /** Set your things data type.
             * IoT-Ignite works with data which type you have selected.
             */
            ThingDataType.STRING
    );


    private Runnable igniteWatchdogRunnable = new Runnable() {
        @Override
        public void run() {

            if (!igniteConnected) {
                rebuildIgnite();
                igniteWatchdog.postDelayed(this, IGNITE_RECONNECT_INTERVAL);
                Log.e(TAG, "Ignite is not connected trying to reconnect...");
            } else {
                Log.e(TAG, "Ignite is already connected");
                sendIgniteConnectionBroadcast(true);
            }
        }
    };


    private Runnable speedDataSenderRunnable = new Runnable() {
        @Override
        public void run() {
            setOnline();

            if (lastSpeedData > -1) {

                if (igniteConnected && baseNode != null && speedThing != null && baseNode.isRegistered() && speedThing.isRegistered()) {
                    ThingData speedData = new ThingData();
                    speedData.addData(lastSpeedData);
                    speedThing.setThingData(speedData);
                    speedThing.sendData(speedData);
                    Log.i(TAG, "Sending speed: " + lastSpeedData);

                }
                dataSender.postDelayed(this, SPEED_THING_CONFIGURATION_INTERVAL);
            }
        }
    };

    private Runnable rpmDataSenderRunnable = new Runnable() {
        @Override
        public void run() {
            setOnline();

            if (lastRPMData > -1) {
                if (igniteConnected && baseNode != null && rpmThing != null && baseNode.isRegistered() && rpmThing.isRegistered()) {
                    ThingData rpmData = new ThingData();
                    rpmData.addData(lastRPMData);
                    rpmThing.setThingData(rpmData);
                    rpmThing.sendData(rpmData);
                    Log.i(TAG, "Sending rpm: " + lastRPMData);

                }

                dataSender.postDelayed(this, RPM_THING_CONFIGURATION_INTERVAL);
            }
        }
    };

    private Runnable locationDataSenderRunnable = new Runnable() {
        @Override
        public void run() {
            setOnline();
            Intent loc = new Intent("UPDATE_LOC");

            loc.putExtra("location", lastLocationData);
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(loc);
            mLocationHandler.getLastKnownLocation();
            lastLocationData = mLocationHandler.getPrettyLocation();

            if (!TextUtils.isEmpty(lastLocationData) && lastLocationData.length() > 5) {

                if (igniteConnected && baseNode != null && locationThing != null && baseNode.isRegistered() && locationThing.isRegistered()) {
                    ThingData locData = new ThingData();
                    locData.addData(lastLocationData);
                    locationThing.setThingData(locData);
                    locationThing.sendData(locData);
                    Log.i(TAG, "Sending loc: " + lastLocationData);

                }
                dataSender.postDelayed(this, LOCATION_THING_CONFIGURATION_INTERVAL);
            }
        }
    };

    private IotIgniteHandler(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized IotIgniteHandler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new IotIgniteHandler(appContext);
        }
        return INSTANCE;

    }

    public void start() {
        startIgniteWatchdog();
        igniteWatchdog.post(igniteWatchdogRunnable);
        mLocationHandler = new LocationHandler(appContext);
    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Ignite Connected");

        sendIgniteConnectionBroadcast(true);
        // cancel watchdog //
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteConnected = true;

        Log.i(TAG, "Creating Node : " + NODE_ID);

        /** Create base node and things. */
        baseNode = IotIgniteManager.NodeFactory.createNode(NODE_ID, NODE_ID, NodeType.GENERIC, null, this);

        if (baseNode != null) {
            /** Register base node */
            Log.i(TAG, "Registering Node : " + NODE_ID);
            registerNodeAndSetConnectionOnline(baseNode);
            /** Create things. */
            if (baseNode.isRegistered()) {

                Log.i(TAG, "Creating thing : " + SPEED_THING_ID);
                speedThing = baseNode.createThing(SPEED_THING_ID, speedThingType, ThingCategory.EXTERNAL, true, speedThingListener, null);

                if (speedThing != null) {
                    /** Register speed thing. */
                    Log.i(TAG, "Registering thing : " + SPEED_THING_ID);
                    registerThingAndSetConnectionOnline(baseNode, speedThing);
                }

                rpmThing = baseNode.createThing(RPM_THING_ID, rpmThingType, ThingCategory.EXTERNAL, true, rpmThingListener, null);

                if (rpmThing != null) {
                    /** Register rpm thing. */
                    Log.i(TAG, "Registering thing : " + RPM_THING_ID);
                    registerThingAndSetConnectionOnline(baseNode, rpmThing);
                }

                locationThing = baseNode.createThing(LOCATION_THING_ID, locationThingType, ThingCategory.EXTERNAL, true, locationThingListener, null);

                if (locationThing != null) {
                    /** Register location thing. */
                    Log.i(TAG, "Registering thing : " + LOCATION_THING_ID);
                    registerThingAndSetConnectionOnline(baseNode, locationThing);
                }

                emergencyThing = baseNode.createThing(EMERGENCY_THING_ID, emergencyThingType, ThingCategory.EXTERNAL, true, emergencyThingListener, null);

                if (emergencyThing != null) {
                    /** Register emergency thing. */
                    Log.i(TAG, "Registering thing : " + EMERGENCY_THING_ID);
                    registerThingAndSetConnectionOnline(baseNode, emergencyThing);
                }
            }
        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Ignite Disconnected");
        // start watchdog again here.
        igniteConnected = false;
        startIgniteWatchdog();
        sendIgniteConnectionBroadcast(false);

    }

    /**
     * Connect to iot ignite
     */
    private void rebuildIgnite() {
        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setConnectionListener(this)
                    .setContext(appContext)
                    .setLogEnabled(true)
                    .build();
        } catch (UnsupportedVersionException e) {
            Log.e(TAG, "UnsupportedVersionException : " + e);
        }
    }

    /**
     * remove previous callback and setup new watchdog
     */

    private void startIgniteWatchdog() {
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteWatchdog.postDelayed(igniteWatchdogRunnable, IGNITE_RECONNECT_INTERVAL);

    }

    @Override
    public void onNodeUnregistered(String nodeId) {

    }

    /**
     * Set all things and nodes connection to offline.
     * When the application close or destroyed.
     */


    public void shutdown() {
        setNodeConnection(baseNode, false, ON_DESTROY_MSG);
        setThingConnection(speedThing, false, ON_DESTROY_MSG);
        setThingConnection(rpmThing, false, ON_DESTROY_MSG);
        setThingConnection(locationThing, false, ON_DESTROY_MSG);
        setThingConnection(emergencyThing, false, ON_DESTROY_MSG);
    }

    private void registerNodeAndSetConnectionOnline(Node mNode) {

        if (mNode != null) {
            if (mNode.isRegistered() || mNode.register()) {
                mNode.setConnected(true, "");
            }
        }
    }

    private void registerThingAndSetConnectionOnline(Node mNode, Thing mThing) {

        if (mNode != null && mNode.isRegistered() && mThing != null) {

            if (mThing.isRegistered() || mThing.register()) {
                mThing.setConnected(true, "");
                if (SPEED_THING_ID.equals(mThing.getThingID())) {
                    SPEED_THING_CONFIGURATION_INTERVAL = mThing.getThingConfiguration().getDataReadingFrequency();
                    if (SPEED_THING_CONFIGURATION_INTERVAL > 0) {
                        dataSender.removeCallbacks(speedDataSenderRunnable);
                        dataSender.postDelayed(speedDataSenderRunnable, SPEED_THING_CONFIGURATION_INTERVAL);
                    }
                } else if (RPM_THING_ID.equals(mThing.getThingID())) {
                    RPM_THING_CONFIGURATION_INTERVAL = mThing.getThingConfiguration().getDataReadingFrequency();
                    if (RPM_THING_CONFIGURATION_INTERVAL > 0) {
                        dataSender.removeCallbacks(rpmDataSenderRunnable);
                        dataSender.postDelayed(rpmDataSenderRunnable, RPM_THING_CONFIGURATION_INTERVAL);
                    }
                } else if (LOCATION_THING_ID.equals(mThing.getThingID())) {
                    LOCATION_THING_CONFIGURATION_INTERVAL = mThing.getThingConfiguration().getDataReadingFrequency();
                    if (LOCATION_THING_CONFIGURATION_INTERVAL > 0) {
                        dataSender.removeCallbacks(locationDataSenderRunnable);
                        dataSender.postDelayed(locationDataSenderRunnable, LOCATION_THING_CONFIGURATION_INTERVAL);
                    }
                } else if (EMERGENCY_THING_ID.equals(mThing.getThingID())) {
                    EMERGENCY_THING_CONFIGURATION_INTERVAL = mThing.getThingConfiguration().getDataReadingFrequency();
                }
            }
        }
    }

    private void setThingConnection(Thing mThing, boolean state, String explanation) {
        if (mThing != null) {
            mThing.setConnected(state, explanation);
        }

    }

    private void setNodeConnection(Node mNode, boolean state, String explanation) {
        if (mNode != null) {
            mNode.setConnected(state, explanation);
        }
    }

    public IotIgniteManager getIgniteManager() {
        return this.mIotIgniteManager;
    }

    public void setSpeedData(float speed) {

        lastSpeedData = speed;

    }

    public void setRpmData(float rpm) {

        lastRPMData = rpm;


    }

    public void sendEmergencyMessage() {
        setOnline();
        ThingData emergencyData = new ThingData();
        emergencyData.addData(1);

        emergencyThing.setThingData(emergencyData);
        emergencyThing.sendData(emergencyData);
    }

    private void setOnline() {
        baseNode.setConnected(true, "");
        speedThing.setConnected(true, "");
        rpmThing.setConnected(true, "");
        locationThing.setConnected(true, "");
        emergencyThing.setConnected(true, "");
    }

    private void sendIgniteConnectionBroadcast(boolean state) {
        Intent igniteState = new Intent("IGNITE_STATE");
        igniteState.putExtra("state", state);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(igniteState);
    }
}