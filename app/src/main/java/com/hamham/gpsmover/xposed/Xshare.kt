package com.hamham.gpsmover.xposed
import com.hamham.gpsmover.BuildConfig
import de.robv.android.xposed.XSharedPreferences

 class Xshare {

    private var xPref: XSharedPreferences? = null

    private fun pref() : XSharedPreferences {
        xPref = XSharedPreferences(BuildConfig.APPLICATION_ID,"${BuildConfig.APPLICATION_ID}_prefs")
        return xPref as XSharedPreferences
    }


     val isStarted : Boolean
     get() = pref().getBoolean(
         "start",
         false
     )

     val getLat: Double
     get() = pref().getFloat(
         "latitude",
         22.2855200.toFloat()
     ).toDouble()


     val getLng : Double
     get() = pref().getFloat(
         "longitude",
         114.1576900.toFloat()
     ).toDouble()

     val isHookedSystem : Boolean
     get() = pref().getBoolean(
         "isHookedSystem",
         false
     )

     val isRandomPosition :Boolean
     get() = pref().getBoolean(
         "random_position",
         false
     )

     val randomPositionRange : String?
     get() = pref().getString("random_position_range","2")

     val accuracy : String?
     get() = pref().getString("accuracy_settings","5")





}