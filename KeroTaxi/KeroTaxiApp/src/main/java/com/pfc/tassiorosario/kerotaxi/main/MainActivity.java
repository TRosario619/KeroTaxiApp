package com.pfc.tassiorosario.kerotaxi.main;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.DeviceRegistration;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.geo.GeoPoint;
import com.backendless.messaging.DeliveryOptions;
import com.backendless.messaging.PublishOptions;
import com.backendless.messaging.PushPolicyEnum;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.backendless.services.messaging.MessageStatus;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pfc.tassiorosario.kerotaxi.R;
import com.pfc.tassiorosario.kerotaxi.defaults.DefaultCallback;
import com.pfc.tassiorosario.kerotaxi.defaults.Defaults;
import com.pfc.tassiorosario.kerotaxi.model.Corrida;
import com.pfc.tassiorosario.kerotaxi.model.Taxista;
import com.pfc.tassiorosario.kerotaxi.registerAndLogin.LoginActivity;

import java.util.Iterator;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int SECINMILLISECONDS = 1000;
    private static final int MININMILLISECONDS = 60 * SECINMILLISECONDS;
    private Location lastLoc;
    private GoogleMap gMap;
    private SupportMapFragment mapFragment;
    private NavigationView navigationView;
    private LocationRequest LocRequest;
    private GoogleApiClient gApiClient;
    private Marker currLocMarker;
    private BackendlessUser ku;
    private LatLng latLng;
    private TextView nomeUsuario;
    private TextView usuario;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    private GoogleApiClient client;
    private ProgressDialog progressDialog;
    private DeviceRegistration devReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Backendless.setUrl(Defaults.SERVER_URL);
        Backendless.initApp(this, Defaults.APPLICATION_ID, Defaults.SECRET_KEY, Defaults.VERSION);
        ku = Backendless.UserService.CurrentUser();


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // designing the navigation drawer layout
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ku == null) {
                    Snackbar.make(view, "Faça o Login para habilitar esta função", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {

                    callCab();
                }

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //Work with this to change item menu
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        Menu nav_menu = navigationView.getMenu();
        View headerView = navigationView.getHeaderView(0);

        nomeUsuario = (TextView) headerView.findViewById(R.id.welcomeTxtV);
        usuario = (TextView) headerView.findViewById(R.id.userTxtV);
        if (ku == null) {

            nav_menu.findItem(R.id.nav_edit_user).setVisible(false);
            nav_menu.findItem(R.id.nav_history).setVisible(false);
            nav_menu.findItem(R.id.nav_logout).setVisible(false);
        } else {
            nav_menu.findItem(R.id.nav_login).setVisible(false);
            nomeUsuario.setText("Olá "+ ku.getProperty("name"));
            usuario.setText(ku.getEmail());
            String regDev= (String)ku.getProperty("regDispositivo");
           // if(regDev.equals(null)){
                Backendless.Messaging.getDeviceRegistration(new AsyncCallback<DeviceRegistration>() {

                    @Override
                    public void handleResponse(DeviceRegistration devRegistry) {
                        devReg=devRegistry;
                        saveRegDevice();
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {


                    }
                });

        }

        invalidateOptionsMenu();


        // Map code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            checkLocationPermission();
        }
        mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    private void callCab() {

        String query = "distance(%s, %s, localizacaoActual.latitude,localizacaoActual.longitude)< km(5)";
        String whereClause = String.format(query, lastLoc.getLatitude(), lastLoc.getLongitude());

        BackendlessDataQuery dataQuery = new BackendlessDataQuery(whereClause);
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.addRelated("localizacaoActual");
        dataQuery.setQueryOptions(queryOptions);

        progressDialog = ProgressDialog.show(MainActivity.this, "", "A localizar taxistas...", true);
        Backendless.Data.of(Taxista.class).find(dataQuery, new AsyncCallback<BackendlessCollection<Taxista>>() {
            @Override
            public void handleResponse(BackendlessCollection<Taxista> taxistas) {
                progressDialog.cancel();
                if (taxistas.getCurrentPage().size() == 0) {
                    progressDialog.cancel();
                    System.out.println("no taxi drivers");
                }

                Toast.makeText(MainActivity.this, "Success: " + taxistas.getTotalObjects(), Toast.LENGTH_SHORT).show();
                progressDialog = ProgressDialog.show(MainActivity.this, "", "A enviar pedido...", true);
                Iterator<Taxista> iterator = taxistas.getCurrentPage().iterator();
                final DeliveryOptions deliveryOptions = new DeliveryOptions();

                while (iterator.hasNext()) {
                    final Taxista taxista = iterator.next();
                    System.out.println(String.format("Taxista: %s", taxista.getIdUsuario()));

                    Backendless.Persistence.of(BackendlessUser.class).findById(taxista.getIdUsuario(), new AsyncCallback<BackendlessUser>() {
                        @Override
                        public void handleResponse(BackendlessUser keroTaxiUser) {
                            deliveryOptions.addPushSinglecast((String)keroTaxiUser.getProperty( "regDispositivo" ));

                        }

                        @Override
                        public void handleFault(BackendlessFault backendlessFault) {
                            System.out.println("find user Server error: " + backendlessFault.getMessage());
                        }
                    });

                }
                Corrida newCorrida= new Corrida();
                newCorrida.setIdPassageiro(ku.getUserId());
                newCorrida.setLocalizacaoCliente(new GeoPoint(latLng.latitude,latLng.longitude));

                deliveryOptions.setPushPolicy(PushPolicyEnum.ALSO);

                PublishOptions publishOptions = new PublishOptions();
                publishOptions.putHeader(PublishOptions.ANDROID_TICKER_TEXT_TAG, getString(R.string.app_name));
                publishOptions.putHeader(PublishOptions.ANDROID_CONTENT_TITLE_TAG, "Um novo passageiro para si");
                publishOptions.putHeader(PublishOptions.ANDROID_CONTENT_TEXT_TAG, "Olá ");
                //publishOptions.putHeader(PublishOptions.MESSAGE_TAG,newCorrida.getIdPassageiro());

               // Bundle sendBox= new Bundle();



                Backendless.Messaging.publish(newCorrida.toString2(), publishOptions, deliveryOptions, new AsyncCallback<MessageStatus>() {
                    @Override
                    public void handleResponse(MessageStatus messageStatus) {
                        progressDialog.cancel();
                        Toast.makeText(MainActivity.this, "Pendido enviado", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        progressDialog.cancel();
                        System.out.println("Server error 1: " + backendlessFault.getMessage()+", "+backendlessFault.getCode());
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                progressDialog.cancel();
                System.out.println("Server error 2:" + backendlessFault.getMessage());
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (gApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(gApiClient, this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //onLocationChanged(lastLoc);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                this.finish();
            }
            if (resultCode == RESULT_CANCELED) {
                //Do nothing?
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent itemIntent;

        if (id == R.id.nav_login) {
            itemIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(itemIntent);
        } else if (id == R.id.nav_edit_user) {

        } else if (id == R.id.nav_history) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_about) {

        } else if (id == R.id.nav_logout) {
            new AlertDialog.Builder(MainActivity.this).setMessage("Deseja realmente sair?").setNegativeButton("Não", null).setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onLogoutItemClicked();
                }
            }).show();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onLogoutItemClicked() {
        Backendless.UserService.logout(new DefaultCallback<Void>(MainActivity.this, "A sair...") {
            @Override
            public void handleResponse(Void response) {
                super.handleResponse(response);
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                if (fault.getCode().equals("3023")) // Unable to logout: not logged in (session expired, etc.)
                    handleResponse(null);
                else
                    super.handleFault(fault);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        LatLng MOZAMBIQUE = new LatLng(-18.6696553,35.5273354);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MOZAMBIQUE,5.3f));


        //Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                gMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            gMap.setMyLocationEnabled(true);
        }


    }

    protected synchronized void buildGoogleApiClient() {
        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        gApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        lastLoc = location;


        //Place current location marker
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("You are Here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        currLocMarker = gMap.addMarker(markerOptions);

        gMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng newLatLng) {
                if (currLocMarker != null) {
                    currLocMarker.remove();
                }

                currLocMarker = gMap.addMarker(new MarkerOptions().position(newLatLng).
                        title("You are Here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                System.out.println(newLatLng.latitude + ", " + newLatLng.longitude);
                latLng=newLatLng;
            }
        });

        //move map camera
        float zoomIn = (float) 16.0;
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomIn));

        //stop location updates
        if (gApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(gApiClient, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocRequest = new LocationRequest();
        LocRequest.setInterval(5 * MININMILLISECONDS);
        LocRequest.setFastestInterval(MININMILLISECONDS);
        LocRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(LocRequest);

        //**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(gApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_SHORT).show();
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this, 1000);
                            Toast.makeText(MainActivity.this, "RESOLUTION_REQUIRED", Toast.LENGTH_SHORT).show();
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Toast.makeText(MainActivity.this, "SETTINGS_CHANGE_UNAVAILABLE", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(gApiClient, LocRequest,this);
        }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "Problem", Toast.LENGTH_SHORT).show();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (gApiClient == null) {
                            buildGoogleApiClient();
                        }
                        gMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.pfc.tassiorosario.kerotaxi/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.pfc.tassiorosario.kerotaxi/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    private void savePreferences(){
        SharedPreferences sharedPrefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPrefs.edit();


    }

    private void loadPreferences(){
        SharedPreferences sharedPrefs = getPreferences(MODE_PRIVATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void saveRegDevice(){
        ku.setProperty("regDispositivo",devReg.getDeviceId());
        Backendless.UserService.update(ku, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser backendlessUser) {
                System.out.println("registry saved");
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Toast.makeText(MainActivity.this,"reg Error: "+backendlessFault.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
