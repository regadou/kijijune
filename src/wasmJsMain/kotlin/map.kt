import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.Clock
import org.w3c.dom.*
import kotlin.random.Random

const val startZoomLevel = 4.0
const val popupDivOpen = "<div style='width:450px;max-height:300px;overflow:auto'>"
const val popupDivClose = "</div>"
val users = mutableListOf<Map<String,Any?>>()

fun main() {
    println("running kotlin main ...")
}

@ExperimentalJsExport
@JsExport
fun initMap() {
    val now = Clock.System.now().toEpochMilliseconds()
    window.fetch("data/users.json?$now").then{r -> r.json().then { json ->
        val data = toList(toKotlin(json)).map{toMap(it)}
        println("Loaded ${data.size} users")
        val rows = mutableListOf<Map<String,Any?>>()
        for (user in data) {
            users.add(user)
            val geoPoint = toMap(user["geoPoint"])
            rows.add(mutableMapOf<String,Any?>(
                "label" to createLabel(user),
                "latitude" to geoPoint["lat"],
                "longitude" to geoPoint["lon"]
            ))
        }
        displayOpenLayersMap(rows)
        null
    }}
}

@ExperimentalJsExport
@JsExport
fun showData(key: String, js: JsAny) {
    val kt = toKotlin(js)
    for (user in users) {
        if (user[key] == kt) {
            val win = window.open("", "_blank")!!
            val json = toJson(toJsAny(user))
            win.document.write("<pre>$json</pre>")
            return win.document.close()
        }
    }
    window.alert("Pas trouvé $key = $kt")
}

private fun createLabel(user: Map<String,Any?>): String {
    val images = toList(user["images"])
    val picture = if (images.isEmpty()) "&nbsp;" else "<img src='data/${images[0]}' width=200>"
    val html = mutableListOf(
        "<div style='border:solid black 1px;text-align:center'><table border=0 cellspacing=2 cellpadding=0 align=center>",
        "<tr><td valign=top width='200px'>$picture</td><td valign=top width='200px'>"
    )

    if (!appendUserData(html, printDate(user["creationTime"]), "", "<br>"))
        appendUserData(html, printDate(user["time"]), "", "<br>")
    appendUserData(html, joinStrings(user["title"], " / "), "<b>", "</b><br>")
    appendUserData(html, joinStrings(user["address"]), "", "<br>")
    appendUserData(html, joinStrings(user["city"]), "", "<br>")
    appendUserData(html, joinStrings(user["description"]), "<span style='text-wrap:wrap'>", "</span><br>")
    appendUserData(html, joinStrings(user["tags"], " "), "", "<br>")

    val socials = toList(user["socials"])
    for (s in socials) {
        val social = toMap(s)
        val type = social["type"] ?: "url"
        val url = if (type == "email") "mailto:${social["url"]}" else social["url"]
        html.add("$type: <a href='$url' target='_blank'>$url</a><br>")
    }

    val idField = if (user["cesiumId"] != null) "cesiumId" else "gchangeId"
    val pubs = toList(user["pubs"])
    if (pubs.isNotEmpty()) {
        val plural = if (pubs.size > 1) "s" else ""
        val encoded = encodeURIComponent(user[idField]?.toString() ?: "")
        html.add("<a href='pubs.html?key=$idField&value=$encoded' target='_blank'>${pubs.size} annonce$plural</a><br>")
    }
    html.add("</td></tr>")

    appendUserData(html, user["cesiumId"], "<tr><td colspan=2>cesium ID: ", "</td></tr>")
    appendUserData(html, user["gchangeId"], "<tr><td colspan=2>gchange ID: ", "</td></tr>")

    val code = "javascript:showData(\"${idField}\",\"${user[idField]}\")"
    html.add("<tr><td colspan=2 align=center><a href='$code'>Voir les donn&eacute;es</a></td></tr>")

    html.add("</table></div>")
    return html.joinToString("\n")
}

private fun appendUserData(list: MutableList<String>, value: Any?, prefix: String, suffix: String): Boolean {
    if (value == null)
        return false
    val txt = if (value is Collection<*>)
        value.filter { it?.toString()?.isNotBlank() ?: false }.joinToString("<br>")
    else if (value is Array<*>)
        value.filter { it?.toString()?.isNotBlank() ?: false }.joinToString("<br>")
    else
        value.toString().trim()
    if (txt.isEmpty())
        return false
    list.add("$prefix$txt$suffix")
    return true
}

private fun displayOpenLayersMap(rows: List<Map<String,Any?>>) {
    val x: JsAny
    val target = document.querySelector("#data")!!
    val mapid = randomVariable()
    val popupid = randomVariable()
    target.innerHTML = "<div id='$mapid'><div id='$popupid' style='background-color:#ffffff;border:1px solid black;padding:4px;display:none'></div></div>"

    val locations = JsArray<Feature>()
    var sumlat = 0.0
    var sumlon = 0.0
    for (row in rows) {
        val label = row["label"]?.toString() ?: "???"
        val lat = toDouble(row["latitude"])
        val lon = toDouble(row["longitude"])
        val feature = olFeature(label, lon, lat)
        feature.setStyle(olStyle(row["marker"]?.toString() ?: "smiley.png", 0.5, 1.0))
        locations[locations.length] = feature
        sumlat += lat
        sumlon += lon
    }

    val element = document.getElementById(popupid) as HTMLDivElement
    val popup = olOverlay(element, "bottom-center", true, 0, -12)
    val map = olMap(mapid, locations, sumlon/rows.size, sumlat/rows.size, startZoomLevel)
    map.addOverlay(popup)
    
    map.on("click") { evt ->
       val features = mutableListOf<Feature>()
       map.forEachFeatureAtPixel(evt.pixel) { feature ->
           if (feature.get("label") != null)
               features.add(feature)
       }
       if (features.isNotEmpty()) {
           val coordinates = features[0].getGeometry().getCoordinates()
           popup.setPosition(coordinates)
           element.innerHTML = popupDivOpen + features.map { f -> f.get("label") }.joinToString("\n") + popupDivClose
           element.style.display = "block"
       } else {
           val pos = olCoordinates(90.0, 90.0)
           element.style.display = "none"
           popup.setPosition(pos)
       }
    }
}

private fun randomVariable(): String {
   val size = 5 + Random.nextInt(0, 5)
   val base = 'a'
   var name = ""
   for (i in 0 until size)
      name += base + Random.nextInt(0,26)
   return name
}
