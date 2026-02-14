package io.github.martinschneider.baiyue.data

import android.content.Context
import android.content.SharedPreferences
import io.github.martinschneider.baiyue.data.model.Region

class AndroidMapPreferences(context: Context) : MapPreferences {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("map_preferences", Context.MODE_PRIVATE)

    override var showBaiyue: Boolean
        get() = prefs.getBoolean("show_baiyue", true)
        set(value) { prefs.edit().putBoolean("show_baiyue", value).apply() }

    override var showXiaobaiyue: Boolean
        get() = prefs.getBoolean("show_xiaobaiyue", true)
        set(value) { prefs.edit().putBoolean("show_xiaobaiyue", value).apply() }

    override var showTracks: Boolean
        get() = prefs.getBoolean("show_tracks", false)
        set(value) { prefs.edit().putBoolean("show_tracks", value).apply() }

    override var mapLat: Double
        get() = prefs.getFloat("map_lat", Region.taiwanCenter.lat.toFloat()).toDouble()
        set(value) { prefs.edit().putFloat("map_lat", value.toFloat()).apply() }

    override var mapLng: Double
        get() = prefs.getFloat("map_lng", Region.taiwanCenter.lng.toFloat()).toDouble()
        set(value) { prefs.edit().putFloat("map_lng", value.toFloat()).apply() }

    override var mapZoom: Double
        get() = prefs.getFloat("map_zoom", 8.0f).toDouble()
        set(value) { prefs.edit().putFloat("map_zoom", value.toFloat()).apply() }
}
