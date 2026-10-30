package com.lethargicrunner.brainboost.model

/** Préréglage de battement binaural. */
data class Preset(
    val name: String,
    val beatHz: Float,
    val carrierHz: Float,
    val description: String,
)

object Presets {

    /** Porteuse commune (convention binaurale courante, ~100 Hz). */
    const val DEFAULT_CARRIER_HZ = 100f

    /**
     * Focus 10/12/15 : approximations simples des états Hemi-Sync ("mind awake / body asleep",
     * conscience élargie, absence de temps). NON officielles.
     */
    val all = listOf(
        Preset("Delta", 2f, DEFAULT_CARRIER_HZ, "Sommeil profond, réparateur"),
        Preset("Theta", 6f, DEFAULT_CARRIER_HZ, "Somnolence, méditation profonde, mémoire"),
        Preset("Alpha", 10f, DEFAULT_CARRIER_HZ, "Détente éveillée, yeux fermés"),
        Preset("Focus 10", 4f, DEFAULT_CARRIER_HZ, "Corps endormi, esprit éveillé (approximation)"),
        Preset("Focus 12", 7f, DEFAULT_CARRIER_HZ, "Conscience élargie (approximation)"),
        Preset("Focus 15", 9f, DEFAULT_CARRIER_HZ, "Absence de temps (approximation)"),
    )
}
