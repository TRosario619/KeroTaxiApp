package com.pfc.tassiorosario.kerotaxi;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.DeliveryOptions;
import com.backendless.messaging.PublishOptions;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.backendless.services.messaging.MessageStatus;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.login2.DefaultCallback;
import com.login2.Defaults;
import com.pfc.tassiorosario.kerotaxi.model.Taxista;

import java.util.Iterator;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private Location lastLoc;
    private GoogleMap gMap;
    private SupportMapFragment mapFragment;
    private NavigationView navigationView;
    private LocationRequest LocRequest;
    private GoogleApiClient gApiClient;
    private Marker currLocMarker;
    private BackendlessUser bu;
    private LatLng latLng;
    private TextView nomeUsuario;
    private TextView usuario;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Backendless.setUrl(Defaults.SERVER_URL);
        Backendless.initApp(this, Defaults.APPLICATION_ID, Defaults.SECRET_KEY, Defaults.VERSION);
        bu = Backendless.UserService.CurrentUser();


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // designing the navigation drawer layout
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bu == null) {
                    Snackbar.make(view, "Faça o Login para habilitar esta função", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {


                    callCab();

                    //System.out.println(lastLoc.getLatitude()+", "+lastLoc.getLongitude()+": yes");
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
        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);

        nomeUsuario = (TextView) headerView.findViewById(R.id.welcomeTxtV);
        usuario = (TextView) headerView.findViewById(R.id.userTxtV);
        if (bu == null) {

            nav_menu.findItem(R.id.nav_edit_user).setVisible(false);
            nav_menu.findItem(R.id.nav_history).setVisible(false);
            nav_menu.findItem(R.id.nav_logout).setVisible(false);
        } else {
            nav_menu.findItem(R.id.nav_login).setVisible(false);
            nomeUsuario.setText((String) bu.getProperty("name"));
            usuario.setText(bu.getEmail());
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
        String whereClause = String.format(query,lastLoc.getLatitude(),lastLoc.getLongitude());

        BackendlessDataQuery dataQuery = new BackendlessDataQuery(whereClause);
        QueryOptions queryOptions= new QueryOptions();
        queryOptions.addRelated("localizacaoActual");
        dataQuery.setQueryOptions(queryOptions);

        progressDialog = ProgressDialog.show(MainActivity.this, "","A localizar taxistas...", true);
        Backendless.Data.of(Taxista.class).find(dataQuery, new AsyncCallback<BackendlessCollection<Taxista>>() {
            @Override
            public void handleResponse(BackendlessCollection<Taxista> taxistas) {
                progressDialog.cancel();
                if(taxistas.getCurrentPage().size()==0){
                    progressDialog.cancel();
                    System.out.println("no taxi drivers");
                }

                Toast.makeText(MainActivity.this, "Success: "+taxistas.getTotalObjects(), Toast.LENGTH_SHORT).show();
                progressDialog = ProgressDialog.show(MainActivity.this, "","A enviar pedido...", true);
                Iterator<Taxista> iterator = taxistas.getCurrentPage().iterator();
                DeliveryOptions deliveryOptions = new DeliveryOptions();

                while( iterator.hasNext() )
                {
                    Taxista taxista = iterator.next();
                    System.out.println(String.format("Taxista: %s",taxista.getIdUsuario()) );
                    deliveryOptions.addPushSinglecast(taxista.getRegDispositivo());
                }

                PublishOptions publishOptions = new PublishOptions();
                publishOptions.putHeader( PublishOptions.ANDROID_TICKER_TEXT_TAG, getString( R.string.app_name ) );
                publishOptions.putHeader( PublishOptions.ANDROID_CONTENT_TITLE_TAG,"Um novo passageiro para si");
                publishOptions.putHeader( PublishOptions.ANDROID_CONTENT_TEXT_TAG, "Olá");


                Backendless.Messaging.publish("this is a private message", publishOptions, deliveryOptions, new AsyncCallback<MessageStatus>() {
                    @Override
                    public void handleResponse(MessageStatus messageStatus) {
                        progressDialog.cancel();
                        Toast.makeText(MainActivity.this, "Pendido enviado", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        progressDialog.cancel();
                        System.out.println("Server error: "+backendlessFault.getMessage());
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                   progressDialog.cancel();
                   System.out.println("Server error: "+backendlessFault.getMessage());
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
        Intent previous = new Intent(this, MainActivity.class);
        startActivity(previous);
        this.finish();
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
        if (currLocMarker != null) {
            currLocMarker.remove();
        }

        //Place current location marker
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("You are Here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        currLocMarker = gMap.addMarker(markerOptions);

        //move map camera
        float zoomIn = (float) 16.0;
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomIn));
        gMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        //stop location updates
        if (gApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(gApiClient, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocRequest = new LocationRequest();
        LocRequest.setInterval(1000);
        LocRequest.setFastestInterval(1000);
        LocRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(gApiClient, LocRequest, this);
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
                                           @NonNull String permissions[], int[] grantResults) {
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
}
