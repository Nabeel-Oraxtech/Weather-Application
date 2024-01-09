package com.example.weatherapp

import WeatherService
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.models.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {

    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var mSharedPreferences: SharedPreferences

    var mProgressDialog: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location Not Enabled", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(
                    report: MultiplePermissionsReport?
                ) {
                    if (report!!.areAllPermissionsGranted()) {
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

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    rationalDialogForPermissions()
                }
            }).onSameThread().check()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            getLocaionWeatherDetails(latitude, longitude)
        }
    }

    fun getLocaionWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service: WeatherService = retrofit
                .create<WeatherService>(WeatherService::class.java)
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.API_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>,
                ) {
                    if (response.isSuccessful) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse? = response.body()

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        setupUI()

                        Log.i("Response Result", "$weatherList")
                    } else {
                        var rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.e("Error 400", "Bad Connection")
                            }

                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }

                            else -> {
                                Log.e("Error ", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrr", t!!.message.toString())
                    hideProgressDialog()
                }
            })
        } else {
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show()
        }
    }

    fun rationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage(
            "" +
                    "It looks like you have denied permissions, which are required for this feature." +
                    " It can be enabled under Application Setting"
        )
            .setPositiveButton("GOTO SETTINGS")
            { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionRefresh -> {
                requestLocationData()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setupUI() {
        val tvMain = findViewById<TextView>(R.id.tvMain)
        val tvMainDescription = findViewById<TextView>(R.id.tvMainDescription)
        val tvTemp = findViewById<TextView>(R.id.tvTemp)
        val tvSunrise = findViewById<TextView>(R.id.tvSunriseTime)
        val tvSunset = findViewById<TextView>(R.id.tvSunsetTime)
        val tvHumidity = findViewById<TextView>(R.id.tvHumidity)
        val tvMin = findViewById<TextView>(R.id.tvMin)
        val tvMax = findViewById<TextView>(R.id.tvMax)
        val tvSpeed = findViewById<TextView>(R.id.tvSpeed)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvCountry = findViewById<TextView>(R.id.tvCountry)
        val ivMain = findViewById<ImageView>(R.id.ivMain)

        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")

        if (!weatherResponseJsonString.isNullOrEmpty()) {
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)

            for (i in weatherList.weather.indices) {
                Log.i("Weather Name", weatherList.weather.toString())

                tvMain?.text = weatherList.weather[i].main
                tvMainDescription.text = weatherList.weather[i].description
                tvTemp?.text = weatherList.main.temp.toString() +
                        getUnit(application.resources.configuration.locales.toString())
                tvSunrise?.text = unixTime(weatherList.sys.sunrise)
                tvSunset?.text = unixTime(weatherList.sys.sunset)

                tvHumidity.text = weatherList.main.humidity.toString() + "per cent"
                tvMin.text = weatherList.main.temp_min.toString() + "min"
                tvMax.text = weatherList.main.temp_max.toString() + "max"
                tvSpeed.text = weatherList.wind.speed.toString()
                tvName.text = weatherList.name
                tvCountry.text = weatherList.sys.country

                when (weatherList.weather[i].icon) {
                    "01d" -> ivMain.setImageResource(R.drawable.sunny)
                    "02d" -> ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> ivMain.setImageResource(R.drawable.cloud)
                    "04d" -> ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> ivMain.setImageResource(R.drawable.cloud)
                    "10d" -> ivMain.setImageResource(R.drawable.rain)
                    "11d" -> ivMain.setImageResource(R.drawable.storm)
                    "13d" -> ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> ivMain.setImageResource(R.drawable.cloud)
                    "10n" -> ivMain.setImageResource(R.drawable.cloud)
                    "11n" -> ivMain.setImageResource(R.drawable.rain)
                    "13n" -> ivMain.setImageResource(R.drawable.snowflake)
                }

            }
        }
    }


    private fun getUnit(value:String):String?{
        var value="°C"
        if ("US" == value || "LR" ==value || "MM" == value){
            value="°F"
        }
        return value
    }

    private fun unixTime(timex:Long):String?{
        val date=Date(timex * 1000L)
        val sdf=SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }
}