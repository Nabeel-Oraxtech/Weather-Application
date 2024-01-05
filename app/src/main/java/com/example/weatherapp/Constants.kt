package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants{

    const val API_ID:String="233f30efce07458247dfee88a9805a43"
    const val BASE_URL:String="https://api.openweathermap.org/data/2.5/"
    const val METRIC_UNIT:String="Metric"

    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val network=connectivityManager.activeNetwork?: return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(network)?: return false

            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true

                else -> return false
            }
        }else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}