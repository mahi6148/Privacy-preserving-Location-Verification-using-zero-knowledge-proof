package com.example.majorapplication.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionHelper {

     fun checkLocationServices(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermissions(
        activity: ComponentActivity,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
        onLocationServicesDisabled: () -> Unit
    ): ActivityResultLauncher<Array<String>> {
        val locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { resultPermissions ->
            val isLocationGranted:Boolean = resultPermissions[Manifest.permission.ACCESS_FINE_LOCATION]?:false
            if (isLocationGranted) {
                if (checkLocationServices(activity)) {
                    onPermissionGranted()
                } else {
                    Toast.makeText(activity, "Please enable location services", Toast.LENGTH_LONG).show()
                    onLocationServicesDisabled()
                }
            } else {
                Toast.makeText(activity, "Location permission denied", Toast.LENGTH_LONG).show()
                onPermissionDenied()
            }
            if (VERSION.SDK_INT>=VERSION_CODES.TIRAMISU){
                val isNotificationGranted:Boolean = resultPermissions[Manifest.permission.POST_NOTIFICATIONS]?:false
                if (!isNotificationGranted)  {
                    Toast.makeText(activity, "Location permission denied", Toast.LENGTH_LONG).show()
                    onPermissionDenied()
                }
            }
        }
        return locationPermissionLauncher
    }


    fun checkAndRequestPermissions(
        activity: ComponentActivity,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
        onLocationServicesDisabled: () -> Unit
    ) {

        val permissions:Array<String> = if(VERSION.SDK_INT>=VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.POST_NOTIFICATIONS)
        }else{
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val permissionCheckResult = permissions.map {permission ->
            ContextCompat.checkSelfPermission(activity, permission)
        }



        if (permissionCheckResult.all {checkedValue->
                checkedValue==PackageManager.PERMISSION_GRANTED
            }) {
            if (checkLocationServices(activity)) {
                onPermissionGranted()
            } else {
                Toast.makeText(activity, "Please enable location services", Toast.LENGTH_LONG).show()
                onLocationServicesDisabled()
            }
        } else {
            val launcher = requestPermissions(activity, onPermissionGranted, onPermissionDenied, onLocationServicesDisabled)
            launcher.launch(permissions)
        }
    }
}
