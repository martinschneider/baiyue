package io.github.martinschneider.baiyue.platform

/**
 * Port of the Organic Maps URL encoder from the web app's 15_script.js:804-863.
 * Generates URLs like: http://omaps.app/{zoom}{latLonEncoded}/{name}
 */
object OrganicMaps {
    private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val DEFAULT_ZOOM = 7

    fun encodeUrl(lat: Double, lon: Double, name: String, zoom: Int = DEFAULT_ZOOM): String {
        val encoded = encodeLatLon(lat, lon, zoom, name)
        return "http://omaps.app/$encoded"
    }

    fun encodeLatLon(lat: Double, lon: Double, zoom: Int, name: String): String {
        val zoomI = when {
            zoom <= 4 -> 0
            zoom >= 19.75.toInt() -> 63
            else -> ((zoom - 4) * 4)
        }
        val latLonString = latLonToString(lat, lon)
        var result = base64Char(zoomI).toString() + latLonString
        if (name.isNotEmpty()) {
            result += "/" + encodeUri(name)
        }
        return result
    }

    private fun base64Char(x: Int): Char {
        require(x in 0..63) { "Invalid input: $x" }
        return BASE64_CHARS[x]
    }

    private fun latToInt(lat: Double, maxValue: Int): Int {
        val x = (lat + 90.0) / 180.0 * maxValue
        return x.toInt().coerceIn(0, maxValue)
    }

    private fun lonIn180180(lon: Double): Double {
        return if (lon >= 0) {
            (lon + 180.0) % 360.0 - 180.0
        } else {
            val l = (lon - 180.0) % 360.0 + 180.0
            if (l >= 180.0) l - 360.0 else l
        }
    }

    private fun lonToInt(lon: Double, maxValue: Int): Int {
        val x = (lonIn180180(lon) + 180.0) / 360.0 * (maxValue + 1.0) + 0.5
        return if (x <= 0 || x >= maxValue + 1) 0 else x.toInt()
    }

    private fun latLonToString(lat: Double, lon: Double): String {
        val maxVal = (1 shl 30) - 1
        val latI = latToInt(lat, maxVal)
        val lonI = lonToInt(lon, maxVal)

        val result = StringBuilder()
        var shift = 27
        for (i in 0 until 9) {
            val latBits = (latI shr shift) and 7
            val lonBits = (lonI shr shift) and 7

            val nextByte = ((latBits shr 2) and 1 shl 5) or
                    ((lonBits shr 2) and 1 shl 4) or
                    ((latBits shr 1) and 1 shl 3) or
                    ((lonBits shr 1) and 1 shl 2) or
                    ((latBits and 1) shl 1) or
                    (lonBits and 1)

            result.append(base64Char(nextByte))
            shift -= 3
        }
        return result.toString()
    }

    private fun encodeUri(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            // Match JS encodeURI: don't encode unreserved + reserved chars
            if (c.isLetterOrDigit() || c in "-_.~!*'();,/?:@&=+\$#") {
                sb.append(c)
            } else {
                val bytes = c.toString().toByteArray(Charsets.UTF_8)
                for (b in bytes) {
                    sb.append('%')
                    val hex = (b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                    sb.append(hex)
                }
            }
        }
        return sb.toString()
    }
}
