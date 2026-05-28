package com.yanye.home.ui.footprint

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ProvinceShape(
    val adcode: Int,
    val name: String,
    val fullName: String,
    val centerLon: Double,
    val centerLat: Double,
    val polygons: List<List<GeoPoint>>
)

data class GeoPoint(
    val lon: Double,
    val lat: Double
)

data class GeoBounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double
)

fun loadChinaProvinceShapes(context: Context): List<ProvinceShape> {
    val raw = context.assets.open("china_provinces.geojson")
        .bufferedReader()
        .use { it.readText() }
    val features = JSONObject(raw).getJSONArray("features")
    return buildList {
        for (index in 0 until features.length()) {
            val feature = features.getJSONObject(index)
            val properties = feature.getJSONObject("properties")
            val geometry = feature.getJSONObject("geometry")
            val fullName = properties.getString("name")
            if (fullName.isBlank()) continue
            val center = properties.optJSONArray("centroid") ?: properties.optJSONArray("center")
            add(
                ProvinceShape(
                    adcode = properties.optInt("adcode"),
                    name = fullName.toShortProvinceName(),
                    fullName = fullName,
                    centerLon = center?.optDouble(0) ?: 0.0,
                    centerLat = center?.optDouble(1) ?: 0.0,
                    polygons = parseGeometry(geometry)
                )
            )
        }
    }
}

fun loadProvinceCityShapes(
    context: Context,
    adcode: Int
): List<ProvinceShape> =
    runCatching {
        val raw = context.assets.open("province_city_geojson/$adcode.geojson")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(raw)
        if (root.optString("type") != "FeatureCollection") return@runCatching emptyList()
        val features = root.getJSONArray("features")
        buildList {
            for (index in 0 until features.length()) {
                val feature = features.getJSONObject(index)
                val properties = feature.getJSONObject("properties")
                val geometry = feature.getJSONObject("geometry")
                val fullName = properties.optString("name")
                if (fullName.isBlank()) continue
                val center = properties.optJSONArray("centroid") ?: properties.optJSONArray("center")
                add(
                    ProvinceShape(
                        adcode = properties.optInt("adcode"),
                        name = fullName.toShortCityName(),
                        fullName = fullName,
                        centerLon = center?.optDouble(0) ?: 0.0,
                        centerLat = center?.optDouble(1) ?: 0.0,
                        polygons = parseGeometry(geometry)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

fun provinceBounds(shapes: List<ProvinceShape>): GeoBounds {
    val points = shapes.flatMap { shape -> shape.polygons.flatten() }
    return GeoBounds(
        minLon = points.minOf { it.lon },
        maxLon = points.maxOf { it.lon },
        minLat = points.minOf { it.lat },
        maxLat = points.maxOf { it.lat }
    )
}

fun pointInProvince(
    lon: Double,
    lat: Double,
    shape: ProvinceShape
): Boolean =
    shape.polygons.any { ring -> pointInRing(lon, lat, ring) }

private fun parseGeometry(geometry: JSONObject): List<List<GeoPoint>> {
    val type = geometry.getString("type")
    val coordinates = geometry.getJSONArray("coordinates")
    return when (type) {
        "Polygon" -> parsePolygon(coordinates)
        "MultiPolygon" -> buildList {
            for (index in 0 until coordinates.length()) {
                addAll(parsePolygon(coordinates.getJSONArray(index)))
            }
        }
        else -> emptyList()
    }
}

private fun parsePolygon(polygon: JSONArray): List<List<GeoPoint>> =
    buildList {
        for (ringIndex in 0 until polygon.length()) {
            val ring = polygon.getJSONArray(ringIndex)
            add(parseRing(ring))
        }
    }

private fun parseRing(ring: JSONArray): List<GeoPoint> =
    buildList {
        for (pointIndex in 0 until ring.length()) {
            val point = ring.getJSONArray(pointIndex)
            add(
                GeoPoint(
                    lon = point.getDouble(0),
                    lat = point.getDouble(1)
                )
            )
        }
    }

private fun pointInRing(
    lon: Double,
    lat: Double,
    ring: List<GeoPoint>
): Boolean {
    var inside = false
    var previousIndex = ring.lastIndex
    for (index in ring.indices) {
        val current = ring[index]
        val previous = ring[previousIndex]
        val intersects = (current.lat > lat) != (previous.lat > lat) &&
            lon < (previous.lon - current.lon) * (lat - current.lat) /
            (previous.lat - current.lat) + current.lon
        if (intersects) inside = !inside
        previousIndex = index
    }
    return inside
}

private fun String.toShortProvinceName(): String =
    removeSuffix("维吾尔自治区")
        .removeSuffix("壮族自治区")
        .removeSuffix("回族自治区")
        .removeSuffix("自治区")
        .removeSuffix("特别行政区")
        .removeSuffix("省")
        .removeSuffix("市")

private fun String.toShortCityName(): String =
    removeSuffix("自治州")
        .removeSuffix("地区")
        .removeSuffix("盟")
        .removeSuffix("特别行政区")
        .removeSuffix("市")
        .removeSuffix("县")
