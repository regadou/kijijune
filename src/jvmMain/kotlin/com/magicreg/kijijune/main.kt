package com.magicreg.kijijune

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

val objectMapper: ObjectMapper = ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)
val commands = mapOf(
    "update" to ::update,
    "help" to ::help
)

fun main(args: Array<String>) {
    if (args.isEmpty())
        help()
    for (arg in args) {
        val cmd = commands[arg]
        if (cmd == null) {
            println("Invalid command: $arg")
            help()
            break
        }
        cmd()
    }
}

fun help() {
    println("Usage: kijijune <command> [...]")
    println("Valid commands: "+commands.keys.joinToString(", "))
}

fun fromJson(json: String): Any? {
    return objectMapper.readValue(json, Any::class.java)
}

fun toJson(value: Any?): String {
    return objectMapper.writeValueAsString(value)
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

