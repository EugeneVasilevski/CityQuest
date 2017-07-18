package com.vuforia.samples.Vuforia.ui.ActivityList;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vuforia.samples.Vuforia.R;

import java.util.ArrayList;

import static com.vuforia.samples.Vuforia.R.id.map;
import com.vuforia.samples.Vuforia.app.ImageTargets.ImageTargets;

public class MainActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        OnAzimuthChangedListener {

    GoogleMap googleMap;

    public static Location location;
    private LocationManager locationManager;

    private RelativeLayout relativeLayout;

    private TextView textView;

    private ArrayList<CircleOptions> circleOptionsList = new ArrayList<>();

    private boolean mPermissionDenied = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private final float RADIUS = 100;

    private ProgressBar progBar;
    private TextView text;
    private Handler mHandler = new Handler();
    private int mProgressStatus = 0;

    private ImageButton imageButton;

    private ProgressBar distance;

    private TextView text2;

    private MyCurrentAzimuth myCurrentAzimuth;

    private ImageView mPointer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = (RelativeLayout) findViewById(R.id.menu);
        textView = (TextView) findViewById(R.id.textView);

        progBar = (ProgressBar) findViewById(R.id.progressBar);
        text = (TextView) findViewById(R.id.textView);

        distance = (ProgressBar) findViewById(R.id.distance);

        imageButton = (ImageButton) findViewById(R.id.run);

        text2 = (TextView) findViewById(R.id.azimut);

        mPointer = (ImageView) findViewById(R.id.pointer);

        imageButton.setVisibility(View.INVISIBLE);

        relativeLayout.setVisibility(View.INVISIBLE);

        initializeMap();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setupListeners();
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.setOnMapClickListener(this);
        enableMyLocation();

        if (location != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        }

        createInvisibleZone();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (googleMap != null) {
            // Access to the location has been granted to the app.
            googleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(this, "Map clicked", Toast.LENGTH_SHORT).show();

        if (!deleteZone(latLng)) {
            createZone(latLng);
        }

        //isInsideZone();
    }

    private void createZone(LatLng latLng) {
        Toast.makeText(this, "Zone created", Toast.LENGTH_SHORT).show();

        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(RADIUS)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(Color.argb(70, 35, 140, 255));

        googleMap.addCircle(circleOptions);
        circleOptionsList.add(circleOptions);
    }

    private boolean deleteZone(LatLng latLng) {
        if (circleOptionsList.isEmpty()) {
            return false;
        }

        Location location1 = new Location("Location 1");
        Location location2 = new Location("Location 2");

        location1.setLatitude(latLng.latitude);
        location1.setLongitude(latLng.longitude);

        for (CircleOptions circleOptions1 : circleOptionsList) {
            location2.setLatitude(circleOptions1.getCenter().latitude);
            location2.setLongitude(circleOptions1.getCenter().longitude);

            if (location1.distanceTo(location2) < RADIUS) {
                Toast.makeText(this, "Zone deleted", Toast.LENGTH_SHORT).show();

                circleOptionsList.remove(circleOptions1);
                googleMap.clear();

                for (CircleOptions circleOptions2 : circleOptionsList) {
                    googleMap.addCircle(circleOptions2);
                }
                return true;
            }
        }
        return false;
    }

    private void isInsideZone(Location location) {
        if (circleOptionsList.isEmpty()) {
            relativeLayout.setVisibility(View.INVISIBLE);
            return;
        }

        Location location2 = new Location("Location 2");

        boolean isInsideZone = false;

        int i = 0;

        for (CircleOptions circleOptions : circleOptionsList) {
            location2.setLatitude(circleOptions.getCenter().latitude);
            location2.setLongitude(circleOptions.getCenter().longitude);

            if (i == 0) {
                int a = Math.round(location.distanceTo(location2));
                if (a < 100) {
                    distance.setProgress(100);
                } else if (a < 2000) {
                    distance.setProgress((2000 - a) * 100 / 2000);
                }
            }

            i++;
            Toast.makeText(this, "Distance to zone " + i + " : " + Float.toString(location.distanceTo(location2)) + " meters", Toast.LENGTH_SHORT).show();

            if (location.distanceTo(location2) < RADIUS) {
                isInsideZone = true;
            }
        }

        if (isInsideZone) {
            Toast.makeText(this, "You're inside zone", Toast.LENGTH_SHORT).show();
            relativeLayout.setVisibility(View.VISIBLE);

            runProgressBar();

        } else {
            Toast.makeText(this, "You're beyond zone", Toast.LENGTH_SHORT).show();
            relativeLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void runProgressBar() {
        imageButton.setVisibility(View.INVISIBLE);
        new Thread(new Runnable() {
            public void run() {
                while (mProgressStatus < 100) {
                    mProgressStatus++;
                    // Update the progress bar
                    mHandler.post(new Runnable() {
                        public void run() {
                            progBar.setProgress(mProgressStatus);
                            text.setText(mProgressStatus + "%");
                            if (mProgressStatus == 100) imageButton.setVisibility(View.VISIBLE);
                        }
                    });
                    try {
                        Thread.sleep(70);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        mProgressStatus = 0;
    }

    private void createInvisibleZone() {
        LatLng latLng = new LatLng(52.0906, 23.7058);

        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(RADIUS)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(Color.TRANSPARENT);

        circleOptionsList.add(circleOptions);
    }

    public void onButtonClick(View view) {
        relativeLayout.setVisibility(View.INVISIBLE);
    }

    public void onCameraClick(View view) {
        Intent intent = new Intent(MainActivity.this, ImageTargets.class);

        startActivity(intent);
    }

    public void onCompassClick(View view) {
        Intent intent = new Intent(MainActivity.this, Compass.class);

        startActivity(intent);
    }

    public void onRunClick(View view) {
        Intent intent = new Intent(MainActivity.this, ImageTargets.class);

        startActivity(intent);
    }

    private void setMarker(LatLng latLng) {
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);

        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        myCurrentAzimuth.start();

        //checkEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            MainActivity.location = location;

            //text2.setText(Double.toString(calculateTeoreticalAzimuth()));
            isInsideZone(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //checkEnabled();
        }

        @Override
        public void onProviderEnabled(String provider) {
            //checkEnabled();
            //showLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    public double calculateTeoreticalAzimuth() {
        if (circleOptionsList.size() == 1) {
            return 0;
        }

        double dX = circleOptionsList.get(1).getCenter().latitude - location.getLatitude();
        double dY = circleOptionsList.get(1).getCenter().longitude - location.getLongitude();

        double phiAngle;
        double tanPhi;

        tanPhi = Math.abs (dY / dX);
        phiAngle = Math.atan (tanPhi);
        phiAngle = Math.toDegrees (phiAngle);

        if (dX > 0 && dY > 0) { // I quater
            return phiAngle;
        } else if (dX < 0 && dY > 0) { // II
            return 180 - phiAngle;
        } else if (dX < 0 && dY < 0) { // III
            return 180 + phiAngle;
        } else if (dX > 0 && dY < 0) { // IV
            return 360 - phiAngle;
        }

        return phiAngle;
    }

    private void setupListeners() {
        myCurrentAzimuth = new MyCurrentAzimuth(this, this);
        myCurrentAzimuth.start();
    }

    float a = 0;

    @Override
    public void onAzimuthChanged(float azimuthFrom, float azimuthTo) {
        float d;

        float s = (float) calculateTeoreticalAzimuth();

        if (azimuthTo < s) {
            d = s - azimuthTo;
        } else {
            d = 360 - (azimuthTo - s);
        }

        /*RotateAnimation ra = new RotateAnimation(
                a,
                d,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        */

        a = d;

        //mPointer.startAnimation(ra);
        mPointer.setRotation(d);
        text2.setText(Double.toString(calculateTeoreticalAzimuth()));
        //text2.setText(Float.toString(azimuthTo));
    }
}
