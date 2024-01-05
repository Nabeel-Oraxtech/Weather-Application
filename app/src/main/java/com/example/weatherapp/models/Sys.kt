package com.example.weatherapp.models

import java.io.Serializable

data class Sys (
    var type:Int,
    var id:Long,
    var country:String,
    var sunrise:Long,
    var sunset:Long
):Serializable