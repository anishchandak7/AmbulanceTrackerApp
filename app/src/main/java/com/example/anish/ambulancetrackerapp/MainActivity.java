package com.example.anish.ambulancetrackerapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,PermissionsListener, View.OnClickListener,LocationEngineListener {

    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private Location originallocation;
    private Button call_btn,exit_btn,recenterbtn,signoutbtn;
    private TextView driver_name,driver_number;
    String hospital_name;
    GeoPoint geoPoint;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private FirebaseAuth auth;
    private CollectionReference hospitalsReference=FirebaseFirestore.getInstance().collection("Hospitals");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiYW5pc2hjaGFuZGFrNyIsImEiOiJjamo2d3d6cGwyN3poM3FxZ3J1NHJtZ3Z2In0.hEajAQLnnf5-hHx0NMuG_w");
        auth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        call_btn = (Button) findViewById(R.id.call_ambulance_btn);
        exit_btn = (Button) findViewById(R.id.exit_btn);
        recenterbtn = (Button) findViewById(R.id.recenterbutton);
        signoutbtn = (Button) findViewById(R.id.signoutbutton);
        driver_name = (TextView) findViewById(R.id.driver_name_tv);
        driver_number = (TextView) findViewById(R.id.driver_number_tv);
        Intent intent = getIntent();
        //Fetch selected hospital from HospitalListActivity:
        if(intent.hasExtra("hospital_name"))
        {
            hospital_name=intent.getStringExtra("hospital_name");
        }
        if(hospital_name!=null) {
            //Hospitals(collection)->hospital_name(document)->Ambulances(collection)->Amb1(document)
            //For multiple ambulances for same hospital apply different logic AFTER collection Ambulances

            //Fetch details from document:
            hospitalsReference.document(hospital_name).collection("Ambulances").document("Amb1").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @SuppressLint("NewApi")
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if(documentSnapshot.get("status").equals("available")) {
                        geoPoint = (GeoPoint) documentSnapshot.get("location");
                        driver_name.setText("Driver name : "+(CharSequence) documentSnapshot.get("driver"));
                        driver_number.setText("License number :"+(CharSequence) documentSnapshot.get("license_number"));
                        //To implement syncronization between ambulances in order to identify which ambulance of particular
                        //hospital is busy or available for help use below code as status.
                        //This whole if-condition is used for this purpose.
                        //documentSnapshot.getReference().update("status","busy");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        }
        //Buttons OnCLickListenners:
        call_btn.setOnClickListener(this);
        exit_btn.setOnClickListener(this);
        recenterbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCameraPosition(originallocation);
            }
        });

        signoutbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(auth.getCurrentUser()!=null)
                {
                    auth.signOut();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                }
            }
        });
    }

    //This method is responsible for finding route if exist between source to destination:
    private void getRoute(Point origin, Point destination)
    {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Log.d("TAG!", "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e("TAG!", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e("TAG!", "No routes found");
                            return;
                        }
                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e("TAG", "Error: " + t.getMessage());
                    }
                });
    }
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    //Checks status of permissions:
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            initializeLocationEngine();
            initializeLocationLayer();
            enableLocationComponent();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        enableLocationComponent();
        addMarker(geoPoint);
    }

    @SuppressLint("MissingPermission")

    private void initializeLocationEngine()
    {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastlocation = locationEngine.getLastLocation();
        if(lastlocation!=null)
        {
            originallocation = lastlocation;
            setCameraPosition(originallocation);
        }
        else
        {
            locationEngine.addLocationEngineListener(this);
        }
    }
    private void initializeLocationLayer()
    {
        locationLayerPlugin = new LocationLayerPlugin(mapView,mapboxMap,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode.NORMAL);
    }
    private void setCameraPosition(Location location)
    {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),13.0));
    }

    private void addMarker(GeoPoint geoPoint)
    {
        if(geoPoint!=null) {
            Icon icon = IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.map_marker_light);
            mapboxMap.addMarker(new MarkerOptions()
                    .position(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()))
                    .title(hospital_name)
            .icon(icon));
            destinationPosition = Point.fromLngLat(originallocation.getLongitude(), originallocation.getLatitude());
            originPosition = Point.fromLngLat(geoPoint.getLongitude(), geoPoint.getLatitude());
            getRoute(originPosition, destinationPosition);
        }
    }
    @SuppressLint("MissingPermission")
    private void enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(this, R.color.mapbox_blue))
                    .build();

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Activate
            locationComponent.activateLocationComponent(this);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            originallocation = locationComponent.getLastKnownLocation();
            setCameraPosition(originallocation);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.call_ambulance_btn)
        {
            Intent regionActIntent = new Intent(this,RegionActivity.class);
            startActivity(regionActIntent);
        }
        else if(v.getId()==R.id.exit_btn)
        {
            finishAffinity();
            System.exit(0);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!=null)
        {
            originallocation=location;
            setCameraPosition(originallocation);
        }

    }
}
