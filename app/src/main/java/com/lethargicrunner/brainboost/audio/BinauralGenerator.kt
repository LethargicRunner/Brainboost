package com.lethargicrunner.brainboost.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Génération pure de sons binauraux : sinus sur l'oreille gauche à carrierHz,
 * sinus à carrierHz + beatHz sur l'oreille droite.
 * Aucune dépendance externe : les échantillons PCM 16 bits sont calculés à la volée.
 */
object BinauralGenerator {

    const val SAMPLE_RATE = 44100

    /** Amplitude normalisée (0.0 .. 1.0) appliquée avant conversion 16 bits. */
    const val DEFAULT_AMPLITUDE = 0.4

    /** Nombre d'échantillons (tous canaux confondus) dans un chunk de 1 seconde. */
    const val CHUNK_SIZE = SAMPLE_RATE * 2

    /** Amplitude normalisée d'un sinus de fréquence freqHz à l'instant tSeconds. */
    fun amplitude(freqHz: Double, tSeconds: Double, level: Double = DEFAULT_AMPLITUDE): Double =
        level * sin(2.0 * PI * freqHz * tSeconds)

    /** Conversion d'une amplitude normalisée en échantillon PCM 16 bits. */
    fun toPcm16(value: Double): Short =
        (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    /**
     * Remplit un chunk stéréo entrelacé (gauche, droite, gauche, ...).
     * @param startSeconds instant de départ (pour la continuité de phase entre chunks).
     */
    fun fillStereoChunk(chunk: ShortArray, carrierHz: Double, beatHz: Double, startSeconds: Double, level: Double = DEFAULT_AMPLITUDE) {
        require(chunk.size == CHUNK_SIZE) { "chunk doit contenir $CHUNK_SIZE échantillons" }
        for (i in chunk.indices step 2) {
            val t = startSeconds + (i / 2) / SAMPLE_RATE.toDouble()
            chunk[i] = toPcm16(amplitude(carrierHz, t, level))
            chunk[i + 1] = toPcm16(amplitude(carrierHz + beatHz, t, level))
        }
    }
}
