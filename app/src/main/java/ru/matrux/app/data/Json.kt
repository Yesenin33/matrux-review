package ru.matrux.app.data

import org.json.JSONObject

internal fun errorCode(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching { JSONObject(body).optString("error").takeIf { it.isNotEmpty() } }.getOrNull()
}

internal fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null
