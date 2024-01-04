package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class MainActivity : AppCompatActivity() {
    lateinit var mFusedLocationClient:FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled()){
            Toast.makeText(this, "Location Not Enabled", Toast.LENGTH_SHORT).show()
            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)

        }
        else{
                Dexter.withActivity(this).withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(
                        report : MultiplePermissionsReport?)
                    {
                        if(report!!.areAllPermissionsGranted())
                        {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(permissions:MutableList<PermissionRequest>?, token: PermissionToken?)
                    {
                        rationalDialogForPermissions()
                    }

                }).onSameThread().check()
        }
    }
    @SuppressLint("MissingPermission")
    fun requestLocationData(){
        val mLocationRequest= com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority= com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper())
    }
    val mLocationCallback=object :LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
           val mLastLocation:Location= locationResult.lastLocation
            val latitude=mLastLocation.latitude
            Log.i("Current Latitude","$latitude")

            val longitude=mLastLocation.longitude
            Log.i("Current Longitude","$longitude")

        }

    }
    fun rationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage(""+
                "It looks like you have denied permissions, which are required for this feature." +
                " It can be enabled under Application Setting")
            .setPositiveButton("GOTO SETTINGS")
            { _,_ ->
                try{
                    val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    val uri= Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                }
                catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL"){ dialog, _ ->
                dialog.dismiss()
            }.show()
    }
    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager=
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}