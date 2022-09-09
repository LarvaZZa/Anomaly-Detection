package com.smart.demo0104;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView infoText;
    private Button primaryButton;

    private LocationRequest locationRequest;
    private SensorManager sensorManager;
    private Sensor sensor;

    private long secForPrimaryDataCollection = 30; //time to collect initial data
    private long secForAnomalyDataCollection = 60; //time for detecting anomalies and uploading to database

    private HashMap<Integer, double[]> sensorData = new HashMap<>();
    private int count = 0;
    private boolean initialSensorStop = false;

    private double[] xValues;
    private double[] yValues;
    private double[] zValues;

    private double[] threshold; //x min max; y min max; z min max
    private Distribution[] distribution = new Distribution[3];
    private double[] deviation = new double[3];
    private double[] mean = new double[3];
    private enum confidence {LOW, MEDIUM, HIGH};

    private FirebaseFirestore db;
    private boolean lockDataUpload = false;
    private HashMap<String, Object[]> data = new HashMap<>();

    //a possible tone generator whenever an anomaly is detected but buggy
    //private ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoText = findViewById(R.id.infoText);
        primaryButton = findViewById(R.id.primaryButton);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);

        primaryButton.setOnClickListener(v -> main());
    }

    private void main(){

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        primaryButton.setVisibility(View.INVISIBLE);

        new CountDownTimer(secForPrimaryDataCollection * 1000, 1000) { //5sec
            @Override
            public void onTick(long l) {
                infoText.setText("Gathering initial data... \n Time remaining: " + l / 1000 + "s");
            }

            @Override
            public void onFinish() {
                sensorManager.unregisterListener(MainActivity.this); //stops accelerometer
                initialSensorStop = true;

                //finds threshold which is currently not used for finding anomalies, only the mean and deviation is used
                threshold = findThreshold(sensorData);

                //distribution for x,y,z values
                distribution[0] = new Distribution(xValues);
                distribution[1] = new Distribution(yValues);
                distribution[2] = new Distribution(zValues);

                //deviation for x,y,z values
                deviation[0] = distribution[0].getStandardDeviation();
                deviation[1] = distribution[1].getStandardDeviation();
                deviation[2] = distribution[2].getStandardDeviation();

                //mean for x,y,z values
                mean[0] = distribution[0].getMean();
                mean[1] = distribution[1].getMean();
                mean[2] = distribution[2].getMean();

                //threshold and standard deviation output for testing purposes
                System.out.println(threshold[0]+"\n"+
                        threshold[1]+"\n"+
                        threshold[2]+"\n"+
                        threshold[3]+"\n"+
                        threshold[4]+"\n"+
                        threshold[5]+"\n");
                System.out.println("\n");
                System.out.println("X DEVIATION: " + deviation[0]);
                System.out.println("Y DEVIATION: " + deviation[1]);
                System.out.println("z devation: " + deviation[2]);

                startAnomalyDetection(sensorManager);
            }
        }.start();
    }

    private double [] findThreshold(HashMap<Integer, double[]> sensorData) {
        double[] result = null;

        int dataSize = sensorData.size();
        xValues = new double[dataSize];
        yValues = new double[dataSize];
        zValues = new double[dataSize];
        count = 0;

        for(Map.Entry<Integer, double[]> entry : sensorData.entrySet()){
            if (result != null){

                //checking for X
                if(entry.getValue()[0] < result[0]){
                    result[0] = entry.getValue()[0];
                } else if (entry.getValue()[0] > result[1]){
                    result[1] = entry.getValue()[0];
                }

                //checking for Y
                if(entry.getValue()[1] < result[2]){
                    result[2] = entry.getValue()[1];
                } else if (entry.getValue()[1] > result[3]){
                    result[3] = entry.getValue()[1];
                }

                //checking for Z
                if(entry.getValue()[2] < result[4]){
                    result[4] = entry.getValue()[2];
                } else if (entry.getValue()[2] > result[5]){
                    result[5] = entry.getValue()[2];
                }

            } else {
                result = new double[6];
                result[0] = entry.getValue()[0]; //x min
                result[1] = entry.getValue()[0]; //x max
                result[2] = entry.getValue()[1]; //y min
                result[3] = entry.getValue()[1]; //y max
                result[4] = entry.getValue()[2]; //z min
                result[5] = entry.getValue()[2]; //z max
            }

            //adding all x, y, and z values
            xValues[count] = entry.getValue()[0];
            yValues[count] = entry.getValue()[1];
            zValues[count] = entry.getValue()[2];
            count++;
        }
        return result;
    }

    private void startAnomalyDetection(SensorManager sensorManager){

        sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        new CountDownTimer(secForAnomalyDataCollection * 1000, 1000) { //5sec
            @Override
            public void onTick(long l) {
                infoText.setText("Detecting anomalies... \n Time remaining: " + l / 1000 + "s");
            }

            @Override
            public void onFinish() {
                sensorManager.unregisterListener(MainActivity.this);
                infoText.setText("LOADING...");
                retrieveData();
                infoText.setText("DONE");
            }
        }.start();
    }

    //gets stored anomalies data from database
    private void retrieveData(){
        db = FirebaseFirestore.getInstance();
        db.collection("anomalies")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                data.put(document.getId(), new Object[]{document.getData().get("lat"), document.getData().get("lng"), document.getData().get("confidence")});
                            }
                            showResultsOnMap(data);
                        } else {
                            System.out.println("Error getting documents.");
                        }
                    }
                });
    }

    private void showResultsOnMap(HashMap<String, Object[]> data){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new MapService(data));
    }

    //asks user to turn on GPS if GPS is off
    private void turnOnGPS() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(task -> {

            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Toast.makeText(MainActivity.this, "GPS is already turned on", Toast.LENGTH_SHORT).show();

            } catch (ApiException e) {

                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(MainActivity.this, 2);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Device does not have location
                        break;
                }
            }
        });

    }

    //checks if GPS is enabled or not
    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;

    }


    @Override
    public synchronized void onSensorChanged(SensorEvent sensorEvent) {

        double[] values = new double[]{sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]};

        if (!initialSensorStop){
            sensorData.put(count, values);
            count++;
        } else {
            //possible to check against x and y values for turning of the bike and for acceleration
            //checkAnomaly(sensorEvent.values[0], deviation[0], mean[0], values);
            //checkAnomaly(sensorEvent.values[1], deviation[1], mean[1], values);

            //currently only checks against z value for bumps ant potholes
            checkAnomaly(sensorEvent.values[2], deviation[2], mean[2], values);
        }
    }

    //may need to be synchronized
    private void checkAnomaly(double value, double deviation, double mean, double[] values){
        //FIX THIS DETECTION MAYBE?
        if(value > mean+3*deviation || value < mean-3*deviation){
            new Thread(new uploadRunnable(confidence.HIGH, values)).start();
        } else if (value > mean+2*deviation || value < mean-2*deviation){
            new Thread(new uploadRunnable(confidence.MEDIUM, values)).start();
        }
    }

    public class uploadRunnable implements Runnable {

        private confidence confidence;
        private double[] values;

        public uploadRunnable(confidence confidence, double[] values){
            this.confidence = confidence;
            this.values = values;
        }

        public void run(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    if (isGPSEnabled()) {

                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .requestLocationUpdates(locationRequest, new LocationCallback() {

                                    @Override
                                    public void onLocationResult(@NonNull LocationResult locationResult) {
                                        super.onLocationResult(locationResult);

                                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                                .removeLocationUpdates(this);

                                        if (locationResult != null && locationResult.getLocations().size() >0){

                                            db = FirebaseFirestore.getInstance();

                                            int index = locationResult.getLocations().size() - 1;
                                            double latitude = locationResult.getLocations().get(index).getLatitude();
                                            double longitude = locationResult.getLocations().get(index).getLongitude();

                                            // Create a new anomaly with a latitude, longitude and confidence level (and x,y,z values)
                                            Map<String, Object> anomaly = new HashMap<>();
                                            anomaly.put("lat", latitude);
                                            anomaly.put("lng", longitude);
                                            anomaly.put("confidence", confidence);
                                            anomaly.put("x", values[0]);
                                            anomaly.put("y", values[1]);
                                            anomaly.put("z", values[2]);
                                            anomaly.put("time", System.currentTimeMillis());

                                            // Add a new document with a generated ID
                                            db.collection("anomalies")
                                                    .add(anomaly)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(MainActivity.this, "ANOMALY UPLOAD ERROR",
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        }
                                    }
                                }, Looper.getMainLooper());

                    } else {
                        turnOnGPS();
                    }

                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    //nothing to do here
    }

}