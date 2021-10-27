package com.example.olhaachuva;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_CODE = 44;

    TextView locationText;
    TextView cityText;
    TextView tempText;
    TextView descriptionText;
    TextView dateText;
    ImageView iconView;

    String[] permissions_all={Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET};
    LocationManager locationManager;

    boolean isGpsLocation;
    Location loc;
    boolean isNetworklocation;

    double lat, lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityText = findViewById(R.id.city);
        tempText = findViewById(R.id.temp);
        descriptionText = findViewById(R.id.description);
        dateText = findViewById(R.id.date);
        iconView = findViewById(R.id.icon);
        getLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            getDeviceLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.removeUpdates(this);
        }
    }

    private void getLocation() {
        if(checkPermission()){
            getDeviceLocation();
        }
        else{
            requestPermission();
        }
    }

    private boolean checkPermission() {
        int result;

        for(int i=0;i < permissions_all.length;i++){
            result = ContextCompat.checkSelfPermission(MainActivity.this,permissions_all[i]);
            if(result == PackageManager.PERMISSION_GRANTED){
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,permissions_all,PERMISSION_CODE);
    }

    private void getDeviceLocation() {
        locationManager = (LocationManager)getSystemService(Service.LOCATION_SERVICE);
        isGpsLocation = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworklocation = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isGpsLocation && !isNetworklocation){
            showSettingForLocation();
            getLastlocation();
        }
        else{
            getFinalLocation();
        }
    }

    private void getLastlocation() {
        if(locationManager!=null) {
            try {
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria,false);
                Location location=locationManager.getLastKnownLocation(provider);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSettingForLocation() {
        AlertDialog.Builder al=new AlertDialog.Builder(MainActivity.this);
        al.setTitle("Localização não habilitada!");
        al.setMessage("Deseja habilitar?");
        al.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        al.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        al.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    getFinalLocation();
                }
                else{
                    Toast.makeText(this, "A Permissão falhou!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void getFinalLocation() {

        try{
            if(isGpsLocation){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000 * 60 * 60,100,MainActivity.this);
                if(locationManager!=null){
                    loc=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                    if(loc!=null){
//                        updateUi(loc);
//                    }
                }
            }
            else if(isNetworklocation){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000 * 60 * 60,100,MainActivity.this);
                if(locationManager!=null){
                    loc=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                    if(loc!=null){
//                        updateUi(loc);
//                    }
                }
            }

        }catch (SecurityException e){
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }

    }

    private void updateUi(Location loc) {
        if(loc.getLatitude()==0 && loc.getLongitude()==0){
            getDeviceLocation();
        }
        else{
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            findWeather();
            preWeather();
        }

    }

    private void findWeather() {

        String URL = "https://api.openweathermap.org/data/2.5/weather?lat=";
        final String location = lat + "&lon="+lon+"&appid=";
        String KEY = "f9831c8b971817f9b0d06f2c4b74b5da";
        String language = "&lang=pt_br";

        String url = URL + location + KEY + language;


        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject main_obj = response.getJSONObject("main");
                    JSONArray array = response.getJSONArray("weather");
                    JSONObject object = array.getJSONObject(0);
                    String temp = String.valueOf(main_obj.getDouble("temp"));
                    String description = object.getString("description");
                    String icon = object.getString("icon");


                    int icone = 0;
                    if(icon.equals("01d")) {
                        icone = R.drawable._01d;
                    } else if(icon.equals("01n")) {
                        icone = R.drawable._01n;
                    } else if(icon.equals("02d")){
                        icone = R.drawable._02d;
                    } else if(icon.equals("02n")){
                        icone = R.drawable._02n;
                    } else if(icon.equals("03d")){
                        icone = R.drawable._03d;
                    } else if(icon.equals("03n")){
                        icone = R.drawable._03n;
                    } else if(icon.equals("04d")){
                        icone = R.drawable._04d;
                    } else if(icon.equals("04n")){
                        icone = R.drawable._04n;
                    } else if(icon.equals("09d")){
                        icone = R.drawable._09d;
                    } else if(icon.equals("09n")){
                        icone = R.drawable._09n;
                    } else if(icon.equals("10d")){
                        icone = R.drawable._10d;
                    } else if(icon.equals("10n")){
                        icone = R.drawable._10n;
                    } else if(icon.equals("11d")){
                        icone = R.drawable._11d;
                    } else if(icon.equals("11n")){
                        icone = R.drawable._11n;
                    } else if(icon.equals("13d")){
                        icone = R.drawable._13d;
                    } else if(icon.equals("13n")){
                        icone = R.drawable._13n;
                    } else if(icon.equals("50d")){
                        icone = R.drawable._50d;
                    } else if(icon.equals("50n")){
                        icone = R.drawable._50n;
                    }
                    iconView.setImageResource(icone);

                    String id = object.getString("id");
                    String city = response.getString("name");

                    cityText.setText(city);
                    descriptionText.setText(description);

                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE-MM-dd");
                    String formatedDate = sdf.format(calendar.getTime());

                    dateText.setText(formatedDate);

                    double temp_kelvin = Double.parseDouble(temp);
                    double temp_celsius = temp_kelvin-273.15;

                    temp_celsius = Math.round(temp_celsius);
                    int c = (int)temp_celsius;

                    tempText.setText(String.valueOf(c)+"°");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jor);


    }

    private void preWeather() {

        String URL = "https://api.openweathermap.org/data/2.5/forecast?lat=";
        final String location = lat + "&lon="+lon+"&appid=";
        String KEY = "f9831c8b971817f9b0d06f2c4b74b5da";

        String url = URL + location + KEY;

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray array = response.getJSONArray("list");
                    JSONObject dado1 = array.getJSONObject(0);
                    JSONObject dado2 = array.getJSONObject(1);
                    JSONObject dado3 = array.getJSONObject(2);
                    JSONObject dado4 = array.getJSONObject(3);
                    JSONObject dado5 = array.getJSONObject(4);
                    JSONObject dado6 = array.getJSONObject(5);
                    JSONObject dado7 = array.getJSONObject(6);
                    JSONObject dado8 = array.getJSONObject(7);

                    List<String> ids = new ArrayList<>();

                    JSONArray array_weather1 = dado1.getJSONArray("weather");
                    JSONObject obj1 = array_weather1.getJSONObject(0);
                    ids.add(obj1.getString("id"));

                    JSONArray array_weather2 = dado2.getJSONArray("weather");
                    JSONObject obj2 = array_weather2.getJSONObject(0);
                    ids.add(obj2.getString("id"));

//                    ids.add("201");

                    JSONArray array_weather3 = dado3.getJSONArray("weather");
                    JSONObject obj3 = array_weather3.getJSONObject(0);
                    ids.add(obj3.getString("id"));

                    JSONArray array_weather4 = dado4.getJSONArray("weather");
                    JSONObject obj4 = array_weather4.getJSONObject(0);
                    ids.add(obj4.getString("id"));

                    JSONArray array_weather5 = dado5.getJSONArray("weather");
                    JSONObject obj5 = array_weather5.getJSONObject(0);
                    ids.add(obj5.getString("id"));

                    JSONArray array_weather6 = dado6.getJSONArray("weather");
                    JSONObject obj6 = array_weather6.getJSONObject(0);
                    ids.add(obj6.getString("id"));

                    JSONArray array_weather7 = dado7.getJSONArray("weather");
                    JSONObject obj7 = array_weather7.getJSONObject(0);
                    ids.add(obj7.getString("id"));

                    JSONArray array_weather8 = dado8.getJSONArray("weather");
                    JSONObject obj8 = array_weather8.getJSONObject(0);
                    ids.add(obj8.getString("id"));


                    for (String id:ids) {

                        if(id.equals("201") || id.equals("202") || id.equals("311") || id.equals("312") || id.equals("314") || id.equals("501") || id.equals("502") || id.equals("503") || id.equals("504") || id.equals("522")) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("OLHA A CHUVA")
                                    .setIcon(R.mipmap.ic_aviso)
                                    .setMessage("Fortes pancadas de chuva previstas para as próximas 24 horas")
                                    .setCancelable(true)
                                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MainActivity.this, "Cancelar escolhido", Toast.LENGTH_SHORT).show();
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                            break;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jor);


    }

    @Override
    public void onLocationChanged(Location location) {
        updateUi(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
