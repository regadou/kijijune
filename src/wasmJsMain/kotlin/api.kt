import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlinx.browser.document
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents

fun newObject(): JsAny = js("({})")

fun getKeys(owner: JsAny?): JsArray<JsString> = js("Object.keys(owner)")

fun getProperty(owner: JsAny?, key: String): JsAny? = js("owner[key]")

fun setProperty(owner: JsAny?, key: String, value: JsAny?): JsAny? = js("owner[key] = value")

fun fromJson(json: String): JsAny?  = js("JSON.parse(json)")

fun toJson(value: JsAny?): String = js("JSON.stringify(value, null, 4)")

fun toJsAny(value: Any?): JsAny? {
    if (value is Int)
        return value.toJsNumber()
    if (value is Short)
        return value.toInt().toJsNumber()
    if (value is Byte)
        return value.toInt().toJsNumber()
    if (value is Number)
        return value.toDouble().toJsNumber()
    if (value is Boolean)
        return value.toJsBoolean()
    if (value is CharSequence)
        return value.toString().toJsString()
    if (value is Char)
        return value.toString().toJsString()
    if (value == null)
        return null
    if (value is Collection<*>) {
        val array = JsArray<JsAny?>()
        for (item in value)
            array[array.length] = toJsAny(item)
    }
    if (value is Array<*>)
        return toJsAny(value.toList())
    if (value is Map<*,*>) {
        val obj = newObject()
        for (entry in value.entries)
            setProperty(obj, entry.key.toString(), toJsAny(entry.value))
        return obj
    }
    if (value is Map.Entry<*,*>) {
        val obj = newObject()
        setProperty(obj, value.key.toString(), toJsAny(value.value))
        return obj
    }
    return value.toJsReference()
}

fun toKotlin(js: JsAny?): Any? {
    val kt = if (js == null)
        null
    else if (js is JsString)
        js.toString()
    else if (js is JsNumber) {
        val txt = js.toString()
        if (txt.contains(".") || txt.contains("e"))
            txt.toDouble()
        else
            txt.toInt()
    }
    else if (js is JsBoolean)
        js.toBoolean()
    else if (js is JsArray<*>) {
        val list = mutableListOf<Any?>()
        for (index in 0 until js.length)
            list.add(toKotlin(js[index]))
        list
    }
    else {
        val map = mutableMapOf<String, Any?>()
        val keys = getKeys(js)
        for (k in 0 until keys.length) {
            val key = keys[k].toString()
            map[key] = toKotlin(getProperty(js, key))
        }
        map
    }
    return kt
}

fun toMap(src: Any?): Map<String,Any?> {
    val dst = if (src is Map<*,*>)
        src.mapKeys { it.key.toString() }
    else if (src == null)
        emptyMap()
    else if (src is CharSequence) {
        val txt = src.toString()
        if (txt.isBlank())
            emptyMap()
        else
            mapOf("value" to txt)
    }
    else
        mapOf("value" to src)
    return dst
}

fun toList(value: Any?): List<Any?> {
    if (value is List<*>)
        return value
    if (value is Collection<*>)
        return value.toList()
    if (value is Array<*>)
        return value.toList()
    if (value == null)
        return emptyList()
    if (value is CharSequence && value.isBlank())
        return emptyList()
    return listOf(value)
}

fun toDouble(value: Any?): Double {
    if (value is Number)
        return value.toDouble()
    if (value is CharSequence)
        return value.toString().toDoubleOrNull() ?: 0.0
    if (value is Boolean)
        return if (value) 1.0 else 0.0
    if (value is Collection<*>)
        return if (value.isEmpty()) 0.0 else toDouble(value.iterator().next())
    if (value is Array<*>)
        return if (value.isEmpty()) 0.0 else toDouble(value.iterator().next())
    if (value is Map<*,*>)
        return if (value.isEmpty()) 0.0 else toDouble(value.iterator().next().value)
    if (value is Map.Entry<*,*>)
        return toDouble(value.value)
    if (value == null)
        return 0.0
    return toDouble(value.toString())
}

fun joinStrings(value: Any?, sep: String = "<br>"): String {
    if (value is Collection<*>)
        return value.joinToString(sep)
    return value?.toString() ?: ""
}

fun printDate(src: Any?): String {
    val t = if (src is Instant)
        src
    else if (src == null)
        Clock.System.now()
    else if (src is Number)
        Instant.fromEpochSeconds((src.toDouble() * 1000).toLong())
    else if (src is CharSequence)
        Instant.parse(src)
    else {
        val n = (if (src is Collection<*>) src.iterator().next() else src).toString().toLongOrNull() ?: return ""
        Instant.fromEpochSeconds(n * 1000)
    }
    return t.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET).split("T")[0]
}

fun escapeHtml(src: String?): String {
    return (src ?: "").toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

fun encodeURIComponent(src: String): String = js("encodeURIComponent(src)")

fun decodeURIComponent(src: String): String = js("decodeURIComponent(src)")

fun getQuery(): Map<String,Any?> {
    val map = mutableMapOf<String,Any?>()
    val urlParts = document.URL.split("#")[0].split("?")
    if (urlParts.size > 1) {
        var parts = urlParts[1].split("&")
        for (p in parts) {
            val part = p.trim().split("=")
            val name = decodeURIComponent(part[0])
            val value = if (part[1] == null) "" else decodeURIComponent(part[1])
            val old = map[name]
            if (old == null)
                map[name] = value
            else if (old is MutableCollection<*>)
                (old as MutableCollection<Any?>).add(value)
            else
                map[name] = mutableListOf(old, value)
        }
    }
    return map
}
