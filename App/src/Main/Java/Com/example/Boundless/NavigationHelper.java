package com.example.boundless;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class NavigationHelper {
    public static void navigateTo(Context context, double desLat, double desLng, String label) {
        Uri googleMapsUri = Uri.parse("google.navigation:q=" + desLat + "," + desLng + "&label=" + Uri.encode(label));

        Intent mapsIntent = new Intent(Intent.ACTION_VIEW, googleMapsUri);
        mapsIntent.setPackage("com.google.android.apps.maps");

        if (mapsIntent.resolveActivity(context.getPackageManager())!=null){
            context.startActivity(mapsIntent);}
        else{
            String browserUrl = "https://www.google.com/maps/dir/?api=1" + "&destination=" + desLat + "," + desLng;
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl));
            context.startActivity(browserIntent);
        }
    }
}