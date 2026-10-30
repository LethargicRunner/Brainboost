package com.lethargicrunner.brainboost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lethargicrunner.brainboost.model.Preset
import com.lethargicrunner.brainboost.model.Presets

/** Écran principal : presets, réglages (battement / porteuse / durée) et lecture. */
@Composable
fun BrainboostScreen(onPlay: (Double, Double, Int) -> Unit, onStop: () -> Unit) {
    var beatHz by remember { mutableStateOf(Presets.all[2].beatHz) }
    var carrierHz by remember { mutableStateOf(Presets.DEFAULT_CARRIER_HZ) }
    var durationMin by remember { mutableStateOf(0f) }
    var playing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Brainboost", style = MaterialTheme.typography.headlineMedium)
        Text("Sons binauraux — casque stéréo obligatoire", style = MaterialTheme.typography.bodySmall)

        PresetRow(presets = Presets.all, selectedBeat = beatHz) { preset ->
            beatHz = preset.beatHz
            carrierHz = preset.carrierHz
        }

        SettingSlider(
            label = "Battement",
            value = beatHz,
            range = 0.5f..40f,
            format = { `${"%.1f".format(it)} Hz` }
        ) { beatHz = it }

        SettingSlider(
            label = "Porteuse",
            value = carrierHz,
            range = 20f..1000f,
            format = { `${it.roundToInt()} Hz` }
        ) { carrierHz = it }

        SettingSlider(
            label = "Durée",
            value = durationMin,
            range = 0f..120f,
            format = { if (it <= 0f) "Illimitée" else `${it.roundToInt()} min` }
        ) { durationMin = it }

        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (playing) {
                    onStop()
                    playing = false
                } else {
                    onPlay(beatHz.toDouble(), carrierHz.toDouble(), durationMin.toInt())
                    playing = true
                }
            }
        ) {
            Text(if (playing) "Arrêter" else "Démarrer")
        }
    }
}

@Composable
private fun PresetRow(presets: List<Preset>, selectedBeat: Float, onSelect: (Preset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presets) { preset ->
            FilterChip(
                selected = kotlin.math.abs(preset.beatHz - selectedBeat) < 0.01f,
                onClick = { onSelect(preset) },
                label = { Text(preset.name) }
            )
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(format(value))
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range
        )
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
