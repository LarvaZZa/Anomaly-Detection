package com.smart.demo0104;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapService extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private HashMap<String, Object[]> data;
    private ArrayList<Marker> markers = new ArrayList<>();

    public MapService(HashMap<String, Object[]> data){
        this.data = data;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map=googleMap;
        for (Map.Entry<String, Object[]> entry : data.entrySet()){
            if(entry.getValue()[2].toString().equals("LOW")){
                markers.add(map.addMarker(new MarkerOptions()
                        .position(new LatLng(new Double(entry.getValue()[0].toString()), new Double(entry.getValue()[1].toString()))).title(entry.getValue()[2].toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
            } else if (entry.getValue()[2].toString().equals("MEDIUM")){
                markers.add(map.addMarker(new MarkerOptions()
                        .position(new LatLng(new Double(entry.getValue()[0].toString()), new Double(entry.getValue()[1].toString()))).title(entry.getValue()[2].toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))));
            } else {
                markers.add(map.addMarker(new MarkerOptions()
                        .position(new LatLng(new Double(entry.getValue()[0].toString()), new Double(entry.getValue()[1].toString()))).title(entry.getValue()[2].toString())));
            }
        }

        if (!markers.isEmpty()){
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : markers) {
                builder.include(marker.getPosition());
            }
            LatLngBounds bounds = builder.build();

            int padding = 0; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            googleMap.animateCamera(cu);
        }
    }
}
