package io.github.martinschneider.baiyue.platform

import app.cash.sqldelight.db.SqlDriver
import io.github.martinschneider.baiyue.data.MapPreferences

expect fun createSqlDriver(context: Any?): SqlDriver

expect fun currentTimeString(): String

expect fun readBundledAsset(context: Any?, path: String): String

expect fun createMapPreferences(context: Any?): MapPreferences
