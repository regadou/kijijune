import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.Clock
import org.w3c.dom.HTMLTableElement

@ExperimentalJsExport
@JsExport
fun initPubs() {
    val table = document.querySelector("table") as HTMLTableElement
    table.style.width = (document.body!!.clientWidth - 10).toString()
    table.style.height = (document.body!!.clientHeight - 70).toString()
    val query = getQuery()
    val key = query["key"]?.toString()
    val value = query["value"]?.toString()
    if (key == null || value == null)
        return window.alert("Missing key or value in query string")
    findUser(key, value)
}

@ExperimentalJsExport
@JsExport
private fun showPub(url: String) {
    window.fetch(url).then{r -> r.json().then{ json ->
        val pub = toMap(toKotlin(json))
        val money = pub["price"]
        val currency = pub["currency"] ?: ""
        val price = if (money == null) "" else "$money $currency"
        val time = printDate(pub["time"])
        val description = "$price $time ${pub["type"]}<br>${pub["description"]}<br>"
        var html = "<div style='border:1px solid black'><h1 style='text-align:center'>${pub["title"]}</h1>$description"
        val pictures = toList(pub["pictures"]).map { toMap(it) }
        for (picture in pictures) {
            val file = toMap(picture["file"])
            val type = picture["_content_type"] ?: file["_content_type"]
            val content = picture["_content"] ?: file["_content"]
            if (type != null && content != null)
                html += " &nbsp; <img src='data:$type;base64,$content'>"
        }
        document.querySelector("#content")!!.innerHTML = "$html</div>"
        null
    }}
}

private fun loadPubs(pubs: List<Map<String,Any?>>): JsAny? {
    var html = ""
    for (pub in pubs) {
        var txt = ""+pub["price"]+" "+pub["time"]+" "+pub["type"]+"</br>"+pub["title"]
        var code = "javascript:showPub(\"data/${pub["url"]}\")"
        html += "<div style='border:1px solid black'><a href='$code'>$txt</a></div>"
    }
    document.querySelector("#menu")!!.innerHTML = html
    return null
}

private fun findUser(key: String, value: String) {
    val now = Clock.System.now().toEpochMilliseconds()
    window.fetch("data/users.json?now").then{r -> r.json().then{ json ->
        val users = toList(toKotlin(json)).map{toMap(it)}
        for (user in users) {
            if (user[key] == value) {
                val title = "Annonces de "+joinStrings(user["title"]," / ")
                document.title = title
                document.querySelector("h1")!!.innerHTML = title
                val pubs = toList(user["pubs"]).map{toMap(it)}
                return@then loadPubs(pubs)
            }
        }
        window.alert("Pas trouvé $key = $value")
        null
    }}
}

