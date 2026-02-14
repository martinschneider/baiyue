package io.github.martinschneider.baiyue.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.martinschneider.baiyue.data.AndroidMapPreferences
import io.github.martinschneider.baiyue.data.MapPreferences
import io.github.martinschneider.baiyue.data.db.BaiyueDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun createSqlDriver(context: Any?): SqlDriver {
    val ctx = context as Context
    return AndroidSqliteDriver(BaiyueDatabase.Schema, ctx, "baiyue.db")
}

actual fun currentTimeString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    return sdf.format(Date())
}

actual fun readBundledAsset(context: Any?, path: String): String {
    val ctx = context as Context
    return ctx.assets.open(path).bufferedReader().use { it.readText() }
}

actual fun createMapPreferences(context: Any?): MapPreferences {
    val ctx = context as Context
    return AndroidMapPreferences(ctx)
}
