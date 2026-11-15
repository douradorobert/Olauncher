package app.olauncher.data

import org.json.JSONArray
import org.json.JSONObject

internal object ContextualRuleCodec {
    fun decode(serialized: String): Map<String, ContextualRule> {
        val array = runCatching { JSONArray(serialized) }.getOrDefault(JSONArray())
        return buildMap {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val identity = item.optString("app")
                if (identity.isBlank()) continue
                put(identity, ContextualRule(
                    appIdentity = identity,
                    startMinutes = item.optInt("start", -1).takeIf { it >= 0 },
                    endMinutes = item.optInt("end", -1).takeIf { it >= 0 },
                    eventKeywords = item.stringSet("keywords"),
                    outsideHome = item.optBoolean("outsideHome"),
                    atHome = item.optBoolean("atHome"),
                    drivingBluetooth = item.optBoolean("drivingBluetooth"),
                    bluetoothDeviceAddresses = item.stringSet("bluetoothDeviceAddresses"),
                    drivingAndroidAuto = item.optBoolean("drivingAndroidAuto"),
                    cooldownMinutes = item.optInt("cooldownMinutes", item.optInt("drivingCooldownMinutes")),
                    daysOfWeek = buildSet {
                        val days = item.optJSONArray("daysOfWeek")
                        if (days != null) for (dayIndex in 0 until days.length()) {
                            days.optInt(dayIndex).takeIf { it in 1..7 }?.let(::add)
                        }
                    },
                ))
            }
        }
    }

    fun encode(rules: Map<String, ContextualRule>): String {
        val array = JSONArray()
        rules.values.forEach { value ->
            array.put(JSONObject()
                .put("app", value.appIdentity)
                .put("start", value.startMinutes ?: -1)
                .put("end", value.endMinutes ?: -1)
                .put("keywords", JSONArray(value.eventKeywords.toList()))
                .put("outsideHome", value.outsideHome)
                .put("atHome", value.atHome)
                .put("drivingBluetooth", value.drivingBluetooth)
                .put("bluetoothDeviceAddresses", JSONArray(value.bluetoothDeviceAddresses.sorted()))
                .put("drivingAndroidAuto", value.drivingAndroidAuto)
                .put("cooldownMinutes", value.cooldownMinutes)
                .put("daysOfWeek", JSONArray(value.daysOfWeek.sorted())))
        }
        return array.toString()
    }

    private fun JSONObject.stringSet(name: String): Set<String> = buildSet {
        val values = optJSONArray(name)
        if (values != null) for (index in 0 until values.length()) {
            values.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}
