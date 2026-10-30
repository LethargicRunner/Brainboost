package com.lethargicrunner.brainboost

import com.lethargicrunner.brainboost.audio.BinauralGenerator
import com.lethargicrunner.brainboost.model.Presets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BinauralTest {

    @Test
    fun `les presets couvrent Delta Theta Alpha et Focus`() {
        val names = Presets.all.map { it.name }
        assertTrue(names.containsAll(listOf("Delta", "Theta", "Alpha", "Focus 10", "Focus 12", "Focus 15")))
        assertEquals(6, Presets.all.size)
    }

    @Test
    fun `les presets restent dans des plages physiologiques raisonnables`() {
        Presets.all.forEach {
            assertTrue("battement ${it.beatHz}", it.beatHz in 0.5f..40f)
            assertTrue("porteuse ${it.carrierHz}", it.carrierHz in 20f..1000f)
        }
        assertEquals(2f, Presets.all.first { it.name == "Delta" }.beatHz)
        assertEquals(10f, Presets.all.first { it.name == "Alpha" }.beatHz)
    }

    @Test
    fun `oreille gauche = porteuse, oreille droite = porteuse + battement`() {
        // porteuse 100 Hz, battement 10 Hz : à t = 0,01 s
        // gauche : sin(2π·100·0,01) = sin(2π) = 0
        // droite : sin(2π·110·0,01) = sin(2,2π) ≈ 0,588
        val left = BinauralGenerator.amplitude(100.0, 0.01)
        val right = BinauralGenerator.amplitude(110.0, 0.01)
        assertEquals(0.0, left, 1e-9)
        assertEquals(0.5878, right, 1e-3)
    }

    @Test
    fun `les échantillons PCM restent dans la plage 16 bits`() {
        val samples = listOf(-1.5, -1.0, 0.0, 0.7, 1.0, 1.5).map { BinauralGenerator.toPcm16(it) }
        samples.forEach { assertTrue(it in Short.MIN_VALUE..Short.MAX_VALUE) }
        assertEquals(Short.MIN_VALUE, samples[0])
        assertEquals(0, samples[2])
        assertEquals(Short.MAX_VALUE, samples[5])
    }

    @Test
    fun `un chunk stéréo contient des canaux gauche et droite différents`() {
        val chunk = ShortArray(BinauralGenerator.CHUNK_SIZE)
        BinauralGenerator.fillStereoChunk(chunk, carrierHz = 100.0, beatHz = 10.0, startSeconds = 0.0)
        var diffs = 0
        for (i in chunk.indices step 2) {
            if (chunk[i] != chunk[i + 1]) diffs++
        }
        // Avec 10 Hz de battement, les canaux diffèrent sur la quasi-totalité des échantillons.
        assertTrue("attendu >1000 différences, trouvé $diffs", diffs > 1000)
    }

    @Test
    fun `la continuité de phase est respectée entre chunks`() {
        val c2 = ShortArray(BinauralGenerator.CHUNK_SIZE)
        BinauralGenerator.fillStereoChunk(c2, 100.0, 10.0, 1.0)
        // Le dernier échantillon du chunk 2 doit suivre le signal continu du chunk 1.
        val expectedEnd = BinauralGenerator.amplitude(100.0, 1.0 + (BinauralGenerator.SAMPLE_RATE - 1) / BinauralGenerator.SAMPLE_RATE.toDouble())
        val actualEnd = c2[BinauralGenerator.SAMPLE_RATE - 2].toDouble() / Short.MAX_VALUE
        assertTrue(abs(expectedEnd - actualEnd) < 0.01)
    }
}
