package app.olauncher.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class SettingsBackup(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "app.olauncher"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun exportTo(uri: Uri) {
        val payload = SettingsBackupCodec.encode(prefs.all)
        require(payload.isNotBlank()) { "Empty backup payload" }
        checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
            output.write(payload.toByteArray(Charsets.UTF_8))
            output.flush()
        }
        val written = checkNotNull(context.contentResolver.openInputStream(uri))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        SettingsBackupCodec.validate(written)
    }

    fun importFrom(uri: Uri) {
        val text = checkNotNull(context.contentResolver.openInputStream(uri))
            .bufferedReader()
            .use { it.readText() }
        val document = SettingsBackupCodec.validate(text)
        val values = document.getJSONObject("values")
        prefs.edit().apply {
            prefs.all.keys.filter(SettingsBackupCodec::shouldInclude).forEach(::remove)
            values.keys().forEach { key ->
                if (!SettingsBackupCodec.shouldInclude(key)) return@forEach
                when (val value = values.get(key)) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putFloat(key, value.toFloat())
                    is String -> putString(key, value)
                    is JSONArray -> putStringSet(key, buildSet {
                        for (index in 0 until value.length()) add(value.getString(index))
                    })
                }
            }
        }.commit()
    }
}

internal object SettingsBackupCodec {
    private const val FORMAT_VERSION = 1

    private val excludedKeys = setOf(
            "FIRST_OPEN_TIME",
            "SCREEN_TIME_LAST_UPDATED",
            "LAUNCHER_RECREATE_TIMESTAMP",
            "DAILY_WALLPAPER_URL",
            "WEATHER_TEMPERATURE",
            "WEATHER_CODE",
            "WEATHER_UPDATED_AT",
            "CONTEXTUAL_HISTORY",
            // Device-bound identifiers and transient driving state stay local.
            "HOME_WIFI_BSSID",
            "HOME_WIFI_NAME",
            "HOME_WIFI_BSSIDS",
            "HOME_WIFI_NAMES",
            "CAR_BLUETOOTH_ADDRESS",
            "CAR_BLUETOOTH_NAME",
            "LAST_CAR_BLUETOOTH_ACTIVE",
            "LAST_ANDROID_AUTO_ACTIVE",
        )

    fun encode(preferences: Map<String, *>): String {
        val values = JSONObject()
        valuesForBackup(preferences).forEach { (key, value) ->
            values.put(key, encode(value))
        }
        return JSONObject()
            .put("format", "olauncher-settings")
            .put("version", FORMAT_VERSION)
            .put("values", values)
            .toString(2)
    }

    fun valuesForBackup(preferences: Map<String, *>): Map<String, *> =
        preferences.filterKeys(::shouldInclude)

    fun validate(text: String): JSONObject {
        require(text.isNotBlank()) { "Empty backup" }
        val document = JSONObject(text)
        require(document.optString("format") == "olauncher-settings") { "Unsupported backup" }
        require(document.optInt("version") in 1..FORMAT_VERSION) { "Unsupported backup version" }
        document.getJSONObject("values")
        return document
    }

    fun shouldInclude(key: String): Boolean =
        key !in excludedKeys &&
            !key.startsWith("MINDFUL_OPEN_") &&
            !key.startsWith("CONTEXTUAL_RULE_MATCH_") &&
            !key.startsWith("SHOWN_ON_")

    private fun encode(value: Any?): Any = when (value) {
        is Set<*> -> JSONArray(value.filterIsInstance<String>())
        null -> JSONObject.NULL
        else -> value
    }
}
