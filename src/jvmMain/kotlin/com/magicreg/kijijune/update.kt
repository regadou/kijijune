package com.magicreg.kijijune

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

const val usersUrl = "https://g1.data.e-is.pro/user/profile/_search"
const val gchangeUsersUrl = "https://data.gchange.fr/user/profile/_search"
const val gchangeMarketUrl = "https://data.gchange.fr/market/record/_search"
const val dataFolder = "webapp/data/"
const val userFile = "users.json"
val users = mutableMapOf<String,MutableMap<String,Any?>>()

fun update() {
    if (!validateOutputFolders())
        return
    postData(usersUrl, geoQuery(), "cesium users") {x -> addUser(x["_id"].toString(), toMap(x["_source"]), mapOf("cesiumId" to x["_id"]))}
    postData(gchangeUsersUrl, geoQuery(), "gchange users") {x -> addUser((toMap(x["_source"])["pubkey"]?:x["_id"]).toString(), toMap(x["_source"]), mapOf("gchangeId" to x["_id"]))}
    val missGchange = users.values.filter{ x -> x["gchangeId"]==null}.map{ x -> x["cesiumId"].toString()}
    postData(gchangeUsersUrl, userQuery("pubkey", missGchange), "missing gchange users") { x -> addUser((toMap(x["_source"])["pubkey"]?:x["_id"]).toString(), toMap(x["_source"]), mapOf("gchangeId" to x["_id"]))}
    val missCesium = users.values.filter{ x -> x["cesiumId"]==null && x["pubkey"]!=null}.map{ x -> x["pubkey"].toString()}
    postData(usersUrl, userQuery("issuer", missCesium), "missing cesium users") { x -> addUser(x["_id"].toString(), toMap(x["_source"]), mapOf("cesiumId" to x["_id"]))}
    val gchangeIds = users.values.filter{x -> x["gchangeId"]!=null}.map{x -> x["gchangeId"].toString()}
    postData(gchangeMarketUrl, userQuery("issuer", gchangeIds), "gchange pubs") {x -> addPub(x)}    
    println("Sorting pubs for each user ...")
    val values = users.values
    values.forEach{u -> u["pubs"] = sortPubs(u["pubs"])}
    println("Saving data to disk ...")
    File(dataFolder+userFile).writeText(toJson(values))
    println("Total of ${values.size} distinct users written to file $userFile")
}

private fun validateOutputFolders(): Boolean {
    val errors = mutableListOf<String>()
    for (folder in "annonces,images".split(",")) {
        val file = File(dataFolder+folder)
        if (file.isDirectory)
            println("Found folder $file")
        else if (file.exists())
            errors.add("File $file is not a folder")
        else if (!file.mkdirs())
            errors.add("Could not create folder $file")
    }

    if (errors.isEmpty())
        return true
    println(errors.joinToString("\n"))
    return false
}

private fun addUser(id: String, data: Map<String,Any?>, props: Map<String,Any?>) {
    val user = users[id] ?: mutableMapOf<String,Any?>("images" to mutableListOf<String>())
    if (user.size == 1)
        users[id] = user
    for (key in data.keys) {
        var value = data[key]
        if (key == "thumbnail" || key == "avatar") {
            addImage(user, value, id)
            continue
        }
        var old = user[key]
        if (old == null)
            user[key] = value
        else if (old == value || value == null)
            continue
        else if (old is String && value is String && old.lowercase() == value.lowercase())
            continue
        else if (key == "geoPoint") {
            val oldPoint = toMap(old).toMutableMap()
            val newPoint = toMap(value)
            oldPoint["lat"] = (toDouble(oldPoint["lat"]) + toDouble(newPoint["lat"])) / 2
            oldPoint["lon"] = (toDouble(oldPoint["lon"]) + toDouble(newPoint["lon"])) / 2
            user[key] = oldPoint
        }
        else if (old is MutableCollection<*>) {
            val coll = (old as MutableCollection<Any?>)
            if (value is Collection<*>) {
                for (v in value) {
                     if (coll.indexOf(v) < 0)
                         coll.add(v)
                }
            }
            else if (coll.indexOf(value) < 0)
                coll.add(value)
        }
        else
            user[key] = mutableListOf(old, value)
    }
    for (key in props.keys)
        user[key] = props[key]
}

private fun addImage(user: MutableMap<String,Any?>, value: Any?, id: String) {
    val values = if (value == null)
        emptyList()
    else if (value is Collection<*>)
        value
    else
        listOf(value)
    for (v in values) {
        val img = toMap(v)
        val type = img["_content_type"]?.toString()
        val content = img["_content"]?.toString()
        if (type == null || content == null)
            continue
        val images = toList(user["images"]) as MutableList<String>
        val filename = "images/"+id+"-"+images.size+"."+type.split("/")[1]
        val bytes = Base64.getDecoder().decode(content)
        File(dataFolder+filename).writeBytes(bytes)
        images.add(filename)
    }
}

private fun addPub(src: Map<String,Any?>) {
    val id = src["_id"]
    val pub = toMap(src["_source"]).toMutableMap()
    val issuer = pub["issuer"]?.toString() ?: return println("No issuer for pub id=$id")
    pub["id"] = id
    val filename = "annonces/$id.json"
    File(dataFolder+filename).writeText(toJson(pub))    
    val user = users[issuer] ?: findUser("gchangeId", issuer) ?: return println("Cannot find user with gchangeId=$issuer")
    val price = pub["price"]?.toString()
    val data = mapOf(
        "url" to filename,
        "title" to pub["title"],
        "type" to pub["type"],
        "time" to java.sql.Date(toDouble(pub["time"]).toLong()*1000).toString(),
        "price" to if (price == null) "" else price+" "+(pub["currency"]?:"")
    )
    val pubs = toList(user["pubs"])
    if (pubs.isNotEmpty())
        (pubs as MutableList<Any?>).add(data)
    else
        user["pubs"] = mutableListOf(data)
}

private fun findUser(key: String, value: Any?): MutableMap<String,Any?>? {
    for (user in users.values) {
        if (user[key] == value)
            return user
    }
    return null
}
    
private fun sortPubs(pubs: Any?): List<Map<String,Any?>> {
    if (pubs == null)
        return emptyList()
    return toList(pubs).map{toMap(it)}.sortedBy{it["time"].toString()}
}

/* alternative geoPoint
  "top_left": {
    "lat": 49.756228, 
    "lon": -77.841663
  },
  "bottom_right": {
    "lat": 45.044528,
    "lon": -67.332978
  }
*/
private fun geoQuery(): String {
    return """{
      "query": {
        "constant_score": {
            "filter": [
            {
              "exists": {
                "field": "geoPoint"
              }
            },
            {
              "geo_bounding_box": {
                "geoPoint": {
                  "top_left": {
                    "lat": 90,
                    "lon": -150
                  },
                  "bottom_right": {
                    "lat": 30,
                    "lon": -50
                  }
                }
              }
            }
          ]
        }
      },
      "from": 0,
      "size": 1000
    }"""
}

private fun userQuery(key: String, values: List<String>): String {
    val query = mutableListOf<Any?>()
    for (value in values)
        query.add(mapOf("match" to mapOf(key to value)))
    return """{
        "query":{"bool":{"should":${toJson(query)}}},
        "from":0,
        "size":1000
    }"""
}

private fun postData(url: String, json: String, message: String, callback: (Map<String,Any?>) -> Unit) {
    println("Downloading $message ...")
    val requestBuilder = HttpRequest.newBuilder().uri(URI(url))
    requestBuilder.setHeader("content-type", "application/json")
    val request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(json))
    val response = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build().send(request.build(), HttpResponse.BodyHandlers.ofString())
    val statusCode = response.statusCode()
    if (statusCode >= 400)
        throw RuntimeException("$url -> $statusCode")
    val result = fromJson(response.body())
    val rows = toList(toMap(toMap(result)["hits"])["hits"])
    println("Got ${rows.size} $message")
    for (row in rows)
        callback(toMap(row))
}

