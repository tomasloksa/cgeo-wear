package io.github.tomasloksa.cgeowear.common

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Serializes the navigation contract to and from byte payloads for the Data Layer. */
object NavCodec {

    private const val VERSION = 1

    fun encodeTarget(target: NavTarget): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { stream ->
            stream.writeInt(VERSION)
            stream.writeDouble(target.latitude)
            stream.writeDouble(target.longitude)
            stream.writeUTF(target.name)
            stream.writeUTF(target.geocode)
        }
        return out.toByteArray()
    }

    fun decodeTarget(bytes: ByteArray): NavTarget =
        DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
            stream.readInt()
            NavTarget(
                latitude = stream.readDouble(),
                longitude = stream.readDouble(),
                name = stream.readUTF(),
                geocode = stream.readUTF(),
            )
        }

    fun encodeTick(tick: NavTick): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { stream ->
            stream.writeInt(VERSION)
            stream.writeDouble(tick.distanceMeters)
            stream.writeFloat(tick.bearingDeg)
        }
        return out.toByteArray()
    }

    fun decodeTick(bytes: ByteArray): NavTick =
        DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
            stream.readInt()
            NavTick(
                distanceMeters = stream.readDouble(),
                bearingDeg = stream.readFloat(),
            )
        }
}
