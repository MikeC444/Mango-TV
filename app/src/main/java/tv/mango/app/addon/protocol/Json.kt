package tv.mango.app.addon.protocol

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Tolerant readers for add-on responses.
 *
 * Add-on responses are parsed by hand from [JsonElement] rather than through
 * generated serialisers, and that is a deliberate choice rather than a shortcut.
 *
 * Add-ons are third-party services written by many people over many years. In
 * practice a field the specification calls a string arrives as a number, a
 * field it calls an array arrives as a single value, and fields it has never
 * heard of arrive constantly. A generated serialiser answers all of that by
 * throwing, which would mean one careless add-on breaks a screen that four
 * other add-ons could have filled.
 *
 * Every reader here returns null or an empty list instead. Nothing throws, so
 * a malformed corner of a response costs only the field it is in.
 */

internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject

internal fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject

/** A string, accepting numbers and booleans written where one was expected. */
internal fun JsonObject.str(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive.content.isBlank()) return null
    if (primitive.content == "null") return null
    return primitive.content
}

internal fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.content?.trim()?.toIntOrNull()

internal fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull
        ?: (this[key] as? JsonPrimitive)?.content?.trim()?.toLongOrNull()

/** A boolean, accepting the strings and 0/1 that add-ons send instead. */
internal fun JsonObject.bool(key: String, default: Boolean = false): Boolean {
    val primitive = this[key] as? JsonPrimitive ?: return default
    primitive.booleanOrNull?.let { return it }
    return when (primitive.content.trim().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> default
    }
}

internal fun JsonObject.array(key: String): List<JsonElement> =
    (this[key] as? JsonArray)?.toList().orEmpty()

/**
 * A list of strings.
 *
 * Accepts a bare string where an array was specified, which several add-ons
 * send for single-valued genres and languages.
 */
internal fun JsonObject.strList(key: String): List<String> {
    return when (val element = this[key]) {
        is JsonArray -> element.mapNotNull { item ->
            (item as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        }
        is JsonPrimitive -> listOfNotNull(element.content.takeIf { it.isNotBlank() })
        else -> emptyList()
    }
}

/** Objects from an array, skipping any entry that is not one. */
internal fun JsonObject.objectList(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

/** The first of [keys] that carries a value, for fields add-ons name differently. */
internal fun JsonObject.firstStr(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { str(it) }

internal fun JsonObject.firstStrList(vararg keys: String): List<String> =
    keys.firstNotNullOfOrNull { key -> strList(key).takeIf { it.isNotEmpty() } }.orEmpty()

internal fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

internal fun JsonElement.asArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()

internal fun JsonElement.asStringOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()?.takeIf { it.isNotBlank() }
