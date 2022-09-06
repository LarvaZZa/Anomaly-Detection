package com.smart.demo0104;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MapService extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private HashMap<String, double[]> data;

    public MapService(HashMap<String, double[]> data){
        this.data = data;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map=googleMap;
        for (Map.Entry<String, double[]> entry : data.entrySet()){
            map.addMarker(new MarkerOptions().position(new LatLng(entry.getValue()[0], entry.getValue()[1])).title("anomaly"));
        }
    }
}
