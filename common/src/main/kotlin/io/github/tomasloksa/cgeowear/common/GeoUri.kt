package io.github.tomasloksa.cgeowear.common

import java.net.URLDecoder

/** Parses `geo:` URIs from c:geo's "Navigate with" menu into a target coordinate and label. */
object GeoUri {

    data class Parsed(
        val latitude: Double,
        val longitude: Double,
        val label: String,
    )

    fun parse(uri: String): Parsed? {
        if (!uri.startsWith("geo:", ignoreCase = true)) return null
        val body = uri.substring("geo:".length)
        val queryIndex = body.indexOf('?')
        val path = if (queryIndex >= 0) body.substring(0, queryIndex) else body
        val query = if (queryIndex >= 0) body.substring(queryIndex + 1) else ""

        val queryValue = queryParam(query, "q")
        val labelFromQuery = labelIn(queryValue)

        val fromQueryCoords = coordsIn(stripLabel(queryValue))
        val fromPathCoords = coordsIn(path)

        val coords = fromPathCoords?.takeUnless { it.first == 0.0 && it.second == 0.0 }
            ?: fromQueryCoords
            ?: fromPathCoords
            ?: return null

        return Parsed(coords.first, coords.second, labelFromQuery)
    }

    private fun coordsIn(text: String?): Pair<Double, Double>? {
        if (text.isNullOrBlank()) return null
        val head = text.substringBefore(';').substringBefore('(')
        val parts = head.split(',')
        if (parts.size < 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lon = parts[1].trim().toDoubleOrNull() ?: return null
        return lat to lon
    }

    private fun stripLabel(value: String?): String? = value?.substringBefore('(')

    private fun labelIn(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val open = value.indexOf('(')
        val close = value.lastIndexOf(')')
        if (open in 0 until close) return value.substring(open + 1, close).trim()
        return if (coordsIn(value) == null) value.trim() else ""
    }

    private fun queryParam(query: String, key: String): String? {
        if (query.isBlank()) return null
        for (pair in query.split('&')) {
            val eq = pair.indexOf('=')
            if (eq < 0) continue
            if (pair.substring(0, eq) == key) {
                return runCatching { URLDecoder.decode(pair.substring(eq + 1), "UTF-8") }
                    .getOrElse { pair.substring(eq + 1) }
            }
        }
        return null
    }
}
