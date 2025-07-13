package com.hamham.gpsmover.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.hamham.gpsmover.BuildConfig
import com.hamham.gpsmover.gsApp
import com.hamham.gpsmover.selfhook.XposedSelfHooks
import dagger.Reusable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


@SuppressLint("WorldReadableFiles")

object PrefManager   {

    private const val START = "start"
    private const val LATITUDE = "latitude"
    private const val LONGITUDE = "longitude"
    private const val HOOKED_SYSTEM = "isHookedSystem"
    private const val RANDOM_POSITION = "random_position"
    private const val RANDOM_POSITION_RANGE = "random_position_range"
    private const val ACCURACY_SETTING = "accuracy_settings"
    private const val MAP_TYPE = "map_type"
    private const val DARK_THEME = "dark_theme"
    private const val DISABLE_UPDATE = "disable_update"


    private val pref: SharedPreferences by lazy {
         try {
             val prefsFile = "${BuildConfig.APPLICATION_ID}_prefs"
             gsApp.getSharedPreferences(
                 prefsFile,
                 Context.MODE_WORLD_READABLE
             )
         }catch (e:SecurityException){
             val prefsFile = "${BuildConfig.APPLICATION_ID}_prefs"
             gsApp.getSharedPreferences(
                 prefsFile,
                 Context.MODE_PRIVATE
             )
         }

    }


    val isStarted : Boolean
    get() = pref.getBoolean(START, false)

    val getLat : Double
    get() = pref.getFloat(LATITUDE, 40.7128F).toDouble()

    val getLng : Double
    get() = pref.getFloat(LONGITUDE, -74.0060F).toDouble()

    var isHookSystem : Boolean
    get() = pref.getBoolean(HOOKED_SYSTEM, true)
    set(value) { pref.edit().putBoolean(HOOKED_SYSTEM,value).apply() }

    var isRandomPosition :Boolean
    get() = pref.getBoolean(RANDOM_POSITION, false)
    set(value) { pref.edit().putBoolean(RANDOM_POSITION, value).apply() }

    var randomPositionRange : String?
    get() = pref.getString(RANDOM_POSITION_RANGE,"2")
    set(value) { pref.edit().putString(RANDOM_POSITION_RANGE,value).apply()}

    var accuracy : String?
    get() = pref.getString(ACCURACY_SETTING,"5")
    set(value) { pref.edit().putString(ACCURACY_SETTING,value).apply()}

    var mapType : Int
    get() = pref.getInt(MAP_TYPE,1)
    set(value) { pref.edit().putInt(MAP_TYPE,value).apply()}

    var darkTheme: Int
        get() = pref.getInt(DARK_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = pref.edit().putInt(DARK_THEME, value).apply()

    var disableUpdate: Boolean
        get() = pref.getBoolean(DISABLE_UPDATE, false)
        set(value) = pref.edit().putBoolean(DISABLE_UPDATE, value).apply()



    fun update(start:Boolean, la: Double, ln: Double) {
        runInBackground {
            val prefEditor = pref.edit()
            prefEditor.putFloat(LATITUDE, la.toFloat())
            prefEditor.putFloat(LONGITUDE, ln.toFloat())
            prefEditor.putBoolean(START, start)
            prefEditor.apply()
            makeWorldReadable()
        }

    }


    /**
     *  Make the redirected prefs file world readable ourselves - fixes a bug in Ed/lsposed
     *
     *  This requires the XSharedPreferences file path, which we get via a self hook. It does nothing
     *  when the Xposed module is not enabled.
     */
    @SuppressLint("SetWorldReadable")
    private fun makeWorldReadable(){
        XposedSelfHooks.getXSharedPrefsPath().let {
            if(it.isNotEmpty()){
                File(it).setReadable(true, false)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun runInBackground(method: suspend () -> Unit){
        GlobalScope.launch(Dispatchers.IO) {
            method.invoke()
        }
    }

}