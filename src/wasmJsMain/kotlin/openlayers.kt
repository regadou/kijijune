
external interface GeoMap: JsAny {
    fun addOverlay(overlay: Overlay)
    fun on(event: String, callback: (Event) -> Unit)
    fun forEachFeatureAtPixel(pixel: JsAny, callback: (Feature) -> Unit)
}

external interface Feature: JsAny {
    fun setStyle(style: JsAny)
    fun get(key: String): JsAny?
    fun getGeometry(): Geometry
}

external interface Geometry: JsAny {
    fun getCoordinates(): JsAny
}

external interface Overlay: JsAny {
    fun setPosition(pos: JsAny)
}

external interface Event: JsAny {
    val pixel: JsAny
}

fun olMap(mapId: String, locations: JsArray<Feature>, x: Double, y: Double, z: Double): GeoMap = js("""
    new ol.Map({
         target: mapId,
         layers: [
           new ol.layer.Tile({
             source: new ol.source.OSM()
           }),
           new ol.layer.Vector({
             source: new ol.source.Vector({features: locations})
           })
         ],
         view: new ol.View({
           center: ol.proj.fromLonLat([x, y]),
           zoom: z
         })
    })    
""")

fun olFeature(label: String, lon: Double, lat: Double): Feature = js("""
    new ol.Feature({
          geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat])),
          label: label
    })
""")

fun olStyle(image: String, x: Double, y: Double): JsAny = js("""
    new ol.style.Style({
         image: new ol.style.Icon({
            anchor: [x, y],
            anchorXUnits: 'fraction',
            anchorYUnits: 'fraction',
            src: image
         })
      })
""")

fun olOverlay(element: JsAny, align: String, stopEvent: Boolean, x: Int, y: Int): Overlay = js("""
    new ol.Overlay({
        element: element,
        positioning: align,
        stopEvent: stopEvent,
        offset: [x, y]
   })
""")

fun olCoordinates(lon: Double, lat: Double): JsAny = js("new ol.geom.Point(ol.proj.fromLonLat([lon, lat])).getCoordinates()")
