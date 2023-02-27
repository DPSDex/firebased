package com.example.firebased

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context)
{
    private var MOB = "mobilenum"
    private val SHAREDPREFFILE = "sharedpreffile"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)

    var mobileNumPref: String
    get() = sharedPreferences.getString(MOB,"-1")!!
    set(value) = sharedPreferences.edit().putString(MOB,value).apply()

}